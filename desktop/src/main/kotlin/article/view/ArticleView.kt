package article.view

import LatexRenderer
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import article.entities.*
import boards.entities.Board
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import individual_board.entities.Note
import individual_board.view.IndividualBoardScreen
import individual_board.view.pxToDp
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.exists
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bson.types.ObjectId
import shared.*
import space.kscience.kmath.UnstableKMathAPI
import space.kscience.kmath.asm.compileToExpression
import space.kscience.kmath.ast.parseMath
import space.kscience.kmath.ast.rendering.FeaturedMathRendererWithPostProcess
import space.kscience.kmath.ast.rendering.LatexSyntaxRenderer
import space.kscience.kmath.ast.rendering.renderWithStringBuilder
import space.kscience.kmath.expressions.MST
import space.kscience.kmath.expressions.invoke
import space.kscience.kmath.operations.Float64Field
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import javax.imageio.ImageIO
import kotlin.math.max


data class ArticleScreen(
    val board: Board,
    val article: Note
): Screen{
    @Composable
    override fun Content() {
        ArticleCompose(board, article)
    }
}

@Composable
fun ArticleCompose(board: Board, article: Note) {
    val navigator = LocalNavigator.currentOrThrow
    articleViewModel = ArticleViewModel(articleModel, article.id)

    var prevSelectedBlock by remember { mutableStateOf<Int?>(null) }
    var selectedBlock by remember { mutableStateOf<Int?>(null) }
    var currEditedText = remember {mutableStateOf<String?>(null) } // keep track of the text currently being changed
    var debugState by remember { mutableStateOf(false) }

    fun changeSelectedBlock(selectedBlock: Int?) {
        println("Moved from block $prevSelectedBlock to block $selectedBlock")
        if (prevSelectedBlock != selectedBlock) {
            if (prevSelectedBlock != null && currEditedText.value != null) {
                val currBlock = articleViewModel.contentBlocksList[prevSelectedBlock!!]
                if (currBlock.blockType == BlockType.CODE) {
                    articleModel.saveBlock(
                        prevSelectedBlock!!, stringContent = currEditedText.value!!, article = article,
                        language = (currBlock as CodeBlock).language,
                        gluedAbove = currBlock.gluedAbove, gluedBelow = currBlock.gluedBelow, board = board
                    )
                } else {
                    articleModel.saveBlock(
                        prevSelectedBlock!!,
                        stringContent = currEditedText.value!!,
                        gluedAbove = currBlock.gluedAbove,
                        gluedBelow = currBlock.gluedBelow,
                        article = article,
                        board = board
                    )
                }
                currEditedText.value = null // reset to keep track of changes of the CURRENT block
            }
        }
        prevSelectedBlock = selectedBlock
    }

    // detect when we change blocks (i.e, change focus)
    // we will only push to the db when focus shifts, so that we don't spam the db with each character change
    LaunchedEffect(selectedBlock) {
        changeSelectedBlock(selectedBlock)
    }

    fun selectAtIndex(index: Int?) { selectedBlock = index } // used with inserting blocks

    val menuButtonFuncs: Map<String, (Int) -> Unit> = mapOf(
        "Set Selected Block to Null" to {
            selectedBlock = null
            changeSelectedBlock(selectedBlock)
        },
        "Toggle Glue Above" to { index ->
            articleModel.toggleGlueUpwards(index, article, board)
        },
        "Toggle Glue Below" to { index ->
            articleModel.toggleGlueDownwards(index, article, board)
        },
        "Duplicate Block" to { index ->
            selectedBlock = index + 1
            changeSelectedBlock(selectedBlock)
            articleModel.duplicateBlock(index, article, board)
        },
        "Move Block Up" to { index ->
            val blocks: MutableList<ContentBlock> = articleViewModel.contentBlocksList
            val (upperBlockStart, lowerBlockEnd) = articleModel.getBlockBounds(blocks, index-1, index)
            if (blocks[index].gluedAbove) {
                selectedBlock = index - 1
            }
            else {
                selectedBlock = upperBlockStart
            }
            changeSelectedBlock(selectedBlock)
            articleModel.moveBlockUp(index, article, board)
        },
        "Move Block Down" to { index ->
            val blocks: MutableList<ContentBlock> = articleViewModel.contentBlocksList
            val (upperBlockStart, lowerBlockEnd) = articleModel.getBlockBounds(blocks, index, index+1)
            if (blocks[index].gluedBelow) {
                selectedBlock = index + 1
            }
            else {
                selectedBlock = lowerBlockEnd
            }
            changeSelectedBlock(selectedBlock)
            articleModel.moveBlockDown(index, article, board)
        },
        "Delete Block" to { index ->
            selectedBlock = null
            changeSelectedBlock(selectedBlock)
            articleModel.deleteBlock(index, article, board)
        }
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                // passing in empty MutableInteractionSource means no ripple effect (i.e. box not grayed out when hovered over)
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { selectedBlock = null }
            .background(tagColorMap[article.tag]!!.copy(alpha=0.05f))
    ) {
        Column(
            modifier = Modifier.padding(top=15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text( // article name
                text = article.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = tagColorMap[article.tag]!!.times(0.6f)
            )
            Text( // article description
                text = article.desc,
                fontSize = 16.sp,
            )

            // row containing any useful functionality (as buttons)
            // so far, "return to previous course" and "DEBUG" buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(
                    colors = textButtonColours(),
                    onClick = {
                        selectedBlock = null
                        changeSelectedBlock(selectedBlock)
                        ScreenManager.push(navigator, IndividualBoardScreen(board))
                    }
                ) { Text("Back to ${board.name} Board") }

                Button(
                    colors = textButtonColours(),
                    onClick = {
                        println("DEBUG (THE BLOCKS):")
                        // println("FROM MODEL: ${articleModel.contentBlockDict}")
                        println("FROM MODEL:")
                        articleModel.contentBlockDict[article.id]?.let {articleContentBlocks ->
                            for (contentBlock in articleContentBlocks) {
                                println("-----------------------------------------------")
                                println("Glued Above? ${contentBlock.gluedAbove}")
                                if (contentBlock.blockType == BlockType.CANVAS) {
                                    println()
                                    println("\tCANVAS HAS ${(contentBlock as CanvasBlock).bList.size} PATHS")
                                } else if (contentBlock.blockType == BlockType.MEDIA) {
                                    println("\tMEDIA HAS ${((contentBlock as MediaBlock).bList.size)} BYTE ARRAY")
                                }
                                else {
                                    println("\t$contentBlock")
                                }
                                println("Glued Below? ${contentBlock.gluedBelow}")
                            }
                        }
                        // println("FROM VIEWMODEL: ${contentBlocksList.contentBlocksList}")
                        println("FROM VIEWMODEL:")
                        for (contentBlock in articleViewModel.contentBlocksList) {
                            println("Glued Above? ${contentBlock.gluedAbove}")
                            if (contentBlock.blockType == BlockType.CANVAS) {
                                println("\tCANVAS HAS ${(contentBlock as CanvasBlock).bList.size} PATHS")
                            } else if (contentBlock.blockType == BlockType.MEDIA) {
                                println("\tMEDIA HAS ${((contentBlock as MediaBlock).bList.size)} BYTE ARRAY")
                            }
                            else {
                                println("\t$contentBlock")
                            }
                            println("Glued Below? ${contentBlock.gluedBelow}")
                        }
                        debugState = !debugState
                    }
                ) { Text(text = "DEBUG") }
            }

            // code for dropdown menu, linking to other boards
            @Composable
            fun RelatedNotesDropDownMenu() {
                val relatedNoteIds: List<ObjectId> = article.relatedNotes
                val notesFromModel: MutableList<Note>? = individualBoardModel.noteDict[board.id]
                val relatedNotes: List<Note>? = notesFromModel?.filter { it.id in relatedNoteIds }
                // now, relatedNotes contains all Note objects that the article is related to
                var relatedNotesExpanded by remember { mutableStateOf(false) }

                val scope = rememberCoroutineScope()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextButton(
                        colors = textButtonColours(),
                        onClick = { relatedNotesExpanded = !relatedNotesExpanded }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Go to related course (${relatedNotes?.size})"
                            )
                            Icon(
                                if (relatedNotesExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                contentDescription = "Go to Related Note",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = relatedNotesExpanded,
                        onDismissRequest = { relatedNotesExpanded = false },
                        containerColor = Colors.veryLightTeal,
                        modifier = Modifier.heightIn(max = 187.dp)
                    ) {
                        relatedNotes?.forEach { currNote ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        currNote.title,
                                        color = tagColorMap[currNote.tag]!!.times(0.7f)
                                    )
                                },
                                onClick = {
                                    selectedBlock = null
                                    changeSelectedBlock(selectedBlock)
                                    relatedNotesExpanded = false
                                    scope.launch {
                                        delay(150)
                                        ScreenManager.push(navigator, ArticleScreen(board, currNote))
                                    }
                                },
                                modifier = Modifier.background(tagColorMap[currNote.tag]!!.copy(alpha=0.05f))
                            )
                        }
                    }
                }
            }

            if (article.relatedNotes.isNotEmpty()) {
                RelatedNotesDropDownMenu()
            }

            if (articleViewModel.contentBlocksList.isEmpty()) {
                Text(
                    text="\nNo content blocks: try adding one!",
                    fontSize = 25.sp
                )
                AddBlockFrameButton(article, 0, "UP", ::selectAtIndex, board)
            }

            LazyColumn( // lazy column stores all blocks
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal=25.dp)
                    .padding(top=5.dp, bottom=20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(
                    articleViewModel.contentBlocksList, // itemsIndexed iterates over this collection
                    key = { index: Int, block: ContentBlock -> block.id } // Jetpack Compose uses keys to track recompositions
                ) { index: Int, block: ContentBlock ->
                    BlockFrame(
                        article = article,
                        blockIndex = index,
                        menuButtonFuncs = menuButtonFuncs,
                        isSelected = (selectedBlock == index),
                        onBlockClick = {
                            // if currently selected, deselect (else, select as normal)
                            selectedBlock = if (selectedBlock == index) null else index
                            println("DEBUG: Selecting block at index $index")
                        },
                        selectAtIndex = ::selectAtIndex,
                        board = board,
                        gluedAbove = block.gluedAbove,
                        gluedBelow = block.gluedBelow,
                        numContentBlocks = articleViewModel.contentBlocksList.size,
                        currEditedText = currEditedText,
                        debugState = debugState
                    )

                    // visually disconnect blocks if not glued
                    if (!block.gluedBelow) {
                        Spacer(modifier = Modifier.size(25.dp))
                    }
                }
            }
            // if no blocks, set select index to null
            if (articleViewModel.contentBlocksList.size == 0) {
                selectedBlock = null
            }
        }
    }
}

@Composable
fun BlockFrame(
    article: Note,
    blockIndex: Int,
    menuButtonFuncs: Map<String, (Int) -> Unit>,
    isSelected: Boolean,
    onBlockClick: () -> Unit,
    selectAtIndex: (Int?) -> Unit,
    board: Board,
    gluedAbove: Boolean,
    gluedBelow: Boolean,
    numContentBlocks: Int,
    currEditedText: MutableState<String?>,
    debugState: Boolean,
) {
    var block by remember { mutableStateOf(articleViewModel.contentBlocksList[blockIndex]) }

    val backgroundColor = if (isSelected) Colors.lightTeal.times(0.92f) else Colors.lightTeal

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onBlockClick() }
            .clip(RoundedCornerShape(6.dp))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            @Composable
            fun GluedBlockBorder(glueParam: Boolean, dir: String) {
                // dir can only be "Above" or "Below"
                if (dir == "Above") {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(if (glueParam) backgroundColor else Colors.medTeal)
                    )
                }
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((if (glueParam) 0 else 25).dp)
                        .background(backgroundColor)
                )
                if (dir == "Below") {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(if (glueParam) backgroundColor else Colors.medTeal)
                    )
                }
            }


            GluedBlockBorder(gluedAbove, "Above")

            Box(
                modifier = Modifier
                    .background(backgroundColor)
                    .padding(horizontal=5.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .defaultMinSize(minHeight = 15.dp)
            ) {

                if (isSelected) {
                    BlockFrameMenu(blockIndex, menuButtonFuncs, numContentBlocks, gluedAbove, gluedBelow)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 25.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isSelected) {
                        Text(
                            text = "${block.blockType.name} Block",
                            fontWeight = FontWeight.Bold
                        )
                        AddBlockFrameButton(article, blockIndex, "UP", selectAtIndex, board)
                    }

                    // TODO: replace this with generalizable code for all ContentBlocks
                    if (block.blockType in
                        listOf(
                            BlockType.PLAINTEXT,
                            BlockType.MARKDOWN,
                            BlockType.CODE,
                            BlockType.MATH
                        )
                    ) {
                        if (isSelected) {
                            EditableTextBox(
                                block = block,
                                onTextChange = { currEditedText.value = it }
                            )
                        }
                    }

                    if (block.blockType == BlockType.PLAINTEXT && !isSelected) {
                        Box(
                            modifier = Modifier.background(Colors.lightTeal).fillMaxWidth().padding(horizontal=12.dp)
                        ) {
                            Box(
                                modifier = Modifier.background(Colors.white).fillMaxWidth()
                            ) {
                                Text(text = (block as TextBlock).text)
                            }
                        }
                    }

                    if (block.blockType == BlockType.CODE && !isSelected) {
                        Box(
                            modifier = Modifier.background(Colors.lightTeal).fillMaxWidth().padding(horizontal=12.dp)
                        ) {
                            Box(
                                modifier = Modifier.background(Colors.black).fillMaxWidth()
                            ) {
                                Text(
                                    text = (block as CodeBlock).text,
                                    color = Color.Green,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.W200
                                )
                            }
                        }
                    }


                    if (block.blockType == BlockType.MARKDOWN && !isSelected) {
                        val markdownHandler = MarkdownHandler((block as MarkdownBlock).text)
                        markdownHandler.renderMarkdown()
                    }

                    var isGraph by remember { mutableStateOf(false)}
                    var graphMST by remember { mutableStateOf(("0").parseMath())}
                    if (block.blockType == BlockType.MATH && !isSelected) {
                        // Render math here

                        var latex = ""
                        try {
                            // Try to parse the "flaky" math
                            val rawMath = ((block as MathBlock).text).parseMath()
                            val syntax = FeaturedMathRendererWithPostProcess.Default.render(rawMath)
                            latex = LatexSyntaxRenderer.renderWithStringBuilder(syntax)
                            graphMST = rawMath
                        } catch (e: Exception) {
                            // Parsing error, render latex as is
                            latex = (block as MathBlock).text
                        }
                        LatexRenderer(latex)
                    }
                    if (block.blockType == BlockType.MATH && isSelected) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            TextButton(
                                modifier = Modifier.align(Alignment.Center),
                                colors = textButtonColours(),
                                onClick = {
                                    isGraph = !isGraph
                                }
                            ) {
                                Text(if (isGraph) "Close Graph" else "Open Graph")
                            }
                        }
                    }

                    // also render graph if possible
                    if (isGraph) {
                        addGraph(graphMST)
                    }

                    if (block.blockType == BlockType.CANVAS && isSelected) {
                        EditableCanvas(
                            block = block,
                            onCanvasUpdate = { bList: MutableList<Byte>, height: Int ->
                                articleModel.saveBlock(
                                    blockIndex,
                                    bList = bList,
                                    canvasHeight = height,
                                    gluedAbove = gluedAbove,
                                    gluedBelow = gluedBelow,
                                    article = article,
                                    board = board
                                )
                                selectAtIndex(null)
                            }
                        )
                    }

                    if (block.blockType == BlockType.CANVAS && !isSelected) {
                        val canvasBlock = block as CanvasBlock
                        val paths: MutableList<PathData> = bytesToPaths(canvasBlock.bList.toByteArray())
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal=12.dp)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(canvasBlock.canvasHeight.dp)
                                    .background(Color.White)
                                    .clipToBounds()
                            ) {
                                paths.forEach { path ->
                                    for (i in 0 until path.points.size - 1) {
                                        drawLine(
                                            color = path.color,
                                            start = path.points[i],
                                            end = path.points[i + 1],
                                            strokeWidth = path.strokeWidth
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (block.blockType == BlockType.MEDIA) {
                        addMedia(
                            block = block,
                            isSelected = isSelected,
                            onMediaUpdate = { bList: MutableList<Byte> ->
                                articleModel.saveBlock(
                                    blockIndex, bList = bList, gluedAbove = gluedAbove,
                                    gluedBelow = gluedBelow, article = article, board = board
                                )
                            }
                        )
                    }

                    if (isSelected) {
                        AddBlockFrameButton(article, blockIndex, "DOWN", selectAtIndex, board)
                    }
                }
            }
            GluedBlockBorder(gluedBelow, "Below")
        }
    }
}

@OptIn(UnstableKMathAPI::class)
@Composable
fun addGraph(
    mst: MST,
    xMin: Double = -10.0,
    xMax: Double = 10.0,
    yMin: Double = -10.0,
    yMax: Double = 10.0
) {
    // Create a general expression from the MST
    val expression = mst.compileToExpression(Float64Field)
    var unrecognized = false

    val points = remember(mst, xMin, xMax) {
        try {
            val result = mutableListOf<Pair<Double, Double>>()
            val steps = 1000
            val step = (xMax - xMin) / steps

            for (i in 0..steps) {
                val x = xMin + i * step
                try {
                    val y = expression(x)
                    result.add(Pair(x, y))

                } catch (e: Exception) {
                    // Skip points where evaluation fails
                }
            }
            result
        } catch (e: Exception) {
            unrecognized = true
            emptyList<Pair<Double, Double>>()
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (unrecognized) {
            Text(
                "Function not recognized :(",
                textAlign = TextAlign.Center,
                color = Colors.errorColor
            )
        }
        else {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color.White).clipToBounds()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Axis
                    val xAxisY = size.height * (yMax / (yMax - yMin))
                    val yAxisX = size.width * (-xMin / (xMax - xMin))
                    drawLine(
                        Color.Gray,
                        Offset(0f, size.height - xAxisY.toFloat()),
                        Offset(size.width, size.height - xAxisY.toFloat()),
                        strokeWidth = 2f
                    )
                    drawLine(
                        Color.Gray,
                        Offset(yAxisX.toFloat(), 0f),
                        Offset(yAxisX.toFloat(), size.height),
                        strokeWidth = 2f
                    )

                    // Function
                    if (points.size >= 2) {
                        val path = androidx.compose.ui.graphics.Path()
                        var firstPoint = true

                        for ((x, y) in points) {
                            val screenX = ((x - xMin) / (xMax - xMin) * size.width).toFloat()
                            val screenY = (size.height - ((y - yMin) / (yMax - yMin) * size.height)).toFloat()

                            if (firstPoint) {
                                path.moveTo(screenX, screenY)
                                firstPoint = false
                            } else {
                                path.lineTo(screenX, screenY)
                            }
                        }
                        drawPath(path, Color.Blue, style = Stroke(width = 3f))
                    }

                    // Grid
                    val gridColor = Color.LightGray.copy(alpha = 0.5f)
                    val gridStep = 1.0

                    var x = Math.ceil(xMin / gridStep) * gridStep
                    while (x <= xMax) {
                        val screenX = ((x - xMin) / (xMax - xMin) * size.width).toFloat()
                        drawLine(
                            gridColor,
                            Offset(screenX, 0f),
                            Offset(screenX, size.height),
                            strokeWidth = 1f
                        )
                        x += gridStep
                    }

                    var y = Math.ceil(yMin / gridStep) * gridStep
                    while (y <= yMax) {
                        val screenY = (size.height - ((y - yMin) / (yMax - yMin) * size.height)).toFloat()
                        drawLine(
                            gridColor,
                            Offset(0f, screenY),
                            Offset(size.width, screenY),
                            strokeWidth = 1f
                        )
                        y += gridStep
                    }
                }
            }
        }
    }
}

fun cropImage() {

}

fun loadImageFromBytes(imageBytes: ByteArray): ImageBitmap? {
    return try {
        // Create an InputStream from the byte array
        val inputStream = ByteArrayInputStream(imageBytes)

        // Use ImageIO to read the image
        val bufferedImage: BufferedImage = ImageIO.read(inputStream)

        // Convert the BufferedImage to ImageBitmap (for Jetpack Compose)
        bufferedImage.toComposeImageBitmap()
    } catch (e: NullPointerException) {
        println("Error loading image: ${e.message}")
        null
    }
}

@Composable
fun addMedia(block: ContentBlock, isSelected: Boolean = true, onMediaUpdate: (MutableList<Byte>) -> Unit) {
    // Initialize the byte list from the block
    val initialBytes = when (block.blockType) {
        BlockType.MEDIA -> (block as MediaBlock).bList
        else -> mutableListOf()
    }

    // Use remember to maintain state across recompositions
    var imageBytes by remember { mutableStateOf(initialBytes) }
    var filePath by remember { mutableStateOf<String?>(null) }

    val launcher = rememberFilePickerLauncher { file ->
        if (file != null) {
            filePath = file.absolutePath()
            println(filePath)
            if (file.exists()) {
                imageBytes = File(filePath).readBytes().toMutableList()
            }
        } else {
            println("No file selected")
        }
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // filePath == null && imageBytes.isEmpty()
        if (isSelected) {
            TextButton(
                colors = textButtonColours(),
                onClick = { launcher.launch() },
            ) {
                Text(if (imageBytes.isEmpty()) "Pick a file" else "Change file")
            }
        }

        if (imageBytes.isNotEmpty()) {
            onMediaUpdate(imageBytes)
//        val imageBitmap = makeFromEncoded(imageBytes.toByteArray()).toComposeImageBitmap()
            val imageBitmap = loadImageFromBytes(imageBytes.toByteArray())

            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "everyone's favorite bird",
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    cropImage()
                                    println("crop mode :)")
                                }
                            )
                        }
                )
            }
        } else {
            println("No file to display")
        }
    }
}

data class PathData(val points: List<Offset>, val color: Color, val strokeWidth: Float)
@Composable
fun EditableCanvas(block: ContentBlock, onCanvasUpdate: (MutableList<Byte>, Int) -> Unit) {

    var canvasHeight by remember { mutableStateOf((block as CanvasBlock).canvasHeight) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableStateOf(5f) }
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var isDrawing by remember { mutableStateOf(true) }
    var isErasing by remember { mutableStateOf(false) }
    var isOutsideBox by remember { mutableStateOf(false) }
    var isResizing by remember { mutableStateOf(false) }
    val resizeThreshold = LocalDensity.current.run { 30 }
    var isGrid by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() } // Controls focus

    val initialBytes = when (block.blockType) {
        BlockType.CANVAS -> (block as CanvasBlock).bList
        else -> mutableListOf()
    }
    var paths by remember { mutableStateOf(bytesToPaths(initialBytes.toByteArray())) }

    var eraserSize = 20f

    Column( // gives user "margin of safety" (i.e. they won't click out of the block if they accidentally miss a button by a little bit)
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusRequester.requestFocus() }
            .padding(12.dp)
    ) {
        MaterialTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().background(Colors.lightGrey)

                ) {
                    // Top control bar
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column() {
                            Row(
                                modifier = Modifier.defaultMinSize(30.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Current: ", fontSize = 14.sp)
                                if (isErasing) {
                                    println("I WANNA ERASE")
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "The best eraser icon I could find",
                                        modifier = Modifier.fillMaxHeight(),
                                        tint = Colors.darkGrey
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Draw,
                                        contentDescription = "The best eraser icon I could find",
                                        modifier = Modifier.fillMaxHeight(),
                                        tint = Colors.darkGrey
                                    )
                                    Box(
                                        modifier = Modifier.size(25.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size((strokeWidth + 3).pxToDp())
                                                .clip(CircleShape)
                                                .background(selectedColor.times(0.9f))
                                                .align(Alignment.Center)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(strokeWidth.pxToDp())
                                                    .clip(CircleShape)
                                                    .background(selectedColor)
                                                    .align(Alignment.Center)
                                            )
                                        }
                                    }
                                }
                            }
                            // Stroke width slider
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)

                            ) {
                                Text("Stroke: ", fontSize = 14.sp)
                                Slider(
                                    value = strokeWidth,
                                    onValueChange = { strokeWidth = it },
                                    valueRange = 1f..20f,
                                    modifier = Modifier.width(150.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Colors.darkTeal,
                                        activeTrackColor = Colors.medTeal,
                                        inactiveTrackColor = Colors.lightTeal
                                    )
                                )
                            }
                        }

                        val controller = rememberColorPickerController()
                        Box(
                            modifier = Modifier.size(100.dp).padding(10.dp)
                        ) {
                            HsvColorPicker(
                                modifier = Modifier
                                    .fillMaxSize(),
                                controller = controller,
                                initialColor = Color.Black,
                                onColorChanged = { colorEnvelope: ColorEnvelope ->
                                    selectedColor = colorEnvelope.color
                                    println("color changed to $selectedColor!!")
                                },
                            )
                        }
                        Column {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                TextButton(
                                    modifier = Modifier.defaultMinSize(minWidth = 120.dp),
                                    colors = textButtonColours(),
                                    onClick = { isErasing = !isErasing }
                                ) {
                                    Text(if (isErasing) "Draw" else "Erase")
                                }
                                TextButton(
                                    colors = textButtonColours(),
                                    onClick = { onCanvasUpdate(canvasToBytes(paths).toMutableList(), canvasHeight) }
                                ) {
                                    Text("Save")
                                }
                            }
                            TextButton(
                                modifier = Modifier.defaultMinSize(minWidth = 120.dp),
                                colors = textButtonColours(),
                                onClick = { isGrid = !isGrid }
                            ) {
                                Text(if (!isGrid) "Show Grid" else "Hide Grid")
                            }
                        }
                    }
                }


                // Drawing canvas
                Box(modifier = Modifier.fillMaxWidth().height(canvasHeight.dp).clipToBounds()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .pointerInput(selectedColor, strokeWidth) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val boxHeight = size.height.toFloat()
                                        val isNearBottomEdge = offset.y in (boxHeight - resizeThreshold)..boxHeight
                                        if (isNearBottomEdge) {
                                            isResizing = true
                                            isDrawing = false
                                            println("DEBUG: RESIZING CANVAS")
                                        } else {
                                            isDrawing = true
                                            isOutsideBox = false

                                            if (isErasing) {
                                                paths.removeAll { pathData ->
                                                    pathData.points.any { point ->
                                                        (point - offset).getDistance() < eraserSize
                                                    }
                                                }
                                                println("So tired")
                                            } else {
                                                currentPath = listOf(offset)
                                            }
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        if (isResizing) {
                                            val newHeight =
                                                max(50, (canvasHeight + 0.5 * change.positionChange().y).toInt())
                                            canvasHeight = newHeight
                                        } else {
                                            val boxWidth = size.width
                                            val boxHeight = size.height
                                            val isInside = change.position.x in 0f..boxWidth.toFloat() &&
                                                    change.position.y in 0f..boxHeight.toFloat()

                                            if (isInside) {
                                                if (isErasing) {

                                                    paths.removeAll { pathData ->
                                                        pathData.points.any { point ->
                                                            (point - change.position).getDistance() < eraserSize
                                                        }
                                                    }

                                                    println("wanna sleep")
                                                } else {
                                                    currentPath = currentPath + change.position
                                                }
                                            } else {
                                                paths = paths.toMutableList().apply {
                                                    add(PathData(currentPath, selectedColor, strokeWidth))
                                                }
                                                currentPath = emptyList()
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (isResizing) {
                                            onCanvasUpdate(canvasToBytes(paths).toMutableList(), canvasHeight)
                                        }
                                        isDrawing = false
                                        isResizing = false
                                        if (!isErasing) {
                                            paths = paths.toMutableList().apply {
                                                add(PathData(currentPath, selectedColor, strokeWidth))
                                            }
                                            currentPath = emptyList()
                                        }
                                    }
                                )
                            }
                    ) {
                        // Draw all saved paths
                        paths.forEach { path ->
                            for (i in 0 until path.points.size - 1) {
                                drawLine(
                                    color = path.color,
                                    start = path.points[i],
                                    end = path.points[i + 1],
                                    strokeWidth = path.strokeWidth
                                )
                            }
                        }

                        if (isGrid) {
                            for (i in 0..size.width.toInt() step 20) {
                                drawLine(
                                    color = Colors.lightGrey,
                                    start = Offset(i.toFloat(), 0f),
                                    end = Offset(i.toFloat(), size.height),
                                    strokeWidth = 1f
                                )
                            }
                            for (j in 0..size.height.toInt() step 20) {
                                drawLine(
                                    color = Colors.lightGrey,
                                    start = Offset(0f, j.toFloat()),
                                    end = Offset(size.width, j.toFloat()),
                                    strokeWidth = 1f
                                )
                            }
                        }

                        if (isDrawing) {
                            // Draw current path
                            for (i in 0 until currentPath.size - 1) {
                                drawLine(
                                    color = selectedColor,
                                    start = currentPath[i],
                                    end = currentPath[i + 1],
                                    strokeWidth = strokeWidth
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Colors.lightGrey)
                            .align(Alignment.BottomCenter)
                            .height((resizeThreshold / LocalDensity.current.density).dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Canvas Height Slider",
                                modifier = Modifier.size(resizeThreshold.dp - 5.dp),
                                tint = Colors.darkGrey
                            )
                        }
                    }
                }

                // Bottom action buttons
                //            Row(
                //                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                //                horizontalArrangement = Arrangement.SpaceBetween
                //            ) {
                //                Button(
                //                    onClick = { showDrawingsList = !showDrawingsList },
                //                    modifier = Modifier.padding(end = 8.dp)
                //                ) {
                //                    Text(if (showDrawingsList) "Hide Drawings" else "Show Drawings")
                //                }
                //
                //                Row {
                //                    Button(
                //                        onClick = { paths = mutableListOf() },
                //                        modifier = Modifier.padding(end = 8.dp)
                //                    ) {
                //                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                //                        Spacer(Modifier.width(4.dp))
                //                        Text("Clear")
                //                    }

                //                    Button(
                //                        onClick = {
                //                            scope.launch {
                //                                // Convert canvas to bytes
                //                                val bytes = canvasToBytes(paths, canvasWidth, canvasHeight)
                //
                //                                // Create or update drawing
                //                                val drawing = Drawing(
                //                                    id = currentDrawingId ?: UUID.randomUUID().toString(),
                //                                    name = currentDrawingName,
                //                                    width = canvasWidth,
                //                                    height = canvasHeight,
                //                                    imageData = bytes
                //                                )
                //
                //                                // Save to MongoDB
                //                                drawingRepository.saveDrawing(drawing)
                //
                //                                // Update current drawing ID
                //                                currentDrawingId = drawing.id
                //
                //                                // Refresh drawings list
                //                                drawings = drawingRepository.getAllDrawings()
                //                            }
                //                        },
                //                        modifier = Modifier.padding(end = 8.dp)
                //                    ) {
                //                        Icon(Icons.Default.Save, contentDescription = "Save")
                //                        Spacer(Modifier.width(4.dp))
                //                        Text("Save")
                //                    }
                //                }
                //            }
            }
        }
    }
}
// Convert canvas paths to bytes for storage
fun canvasToBytes(paths: List<PathData>): ByteArray {
    // Instead of direct pixel manipulation, we'll serialize the path data
    val pathData = ByteArrayOutputStream()
    val numPaths = paths.size
    pathData.write(ByteBuffer.allocate(4).putInt(numPaths).array())

    paths.forEach { path ->
        // Color (4 floats: r, g, b, a)
        pathData.write(ByteBuffer.allocate(16)
            .putFloat(path.color.red)
            .putFloat(path.color.green)
            .putFloat(path.color.blue)
            .putFloat(path.color.alpha).array())

        // Stroke width (1 float)
        pathData.write(ByteBuffer.allocate(4).putFloat(path.strokeWidth).array())

        // Number of points
        val numPoints = path.points.size
        pathData.write(ByteBuffer.allocate(4).putInt(numPoints).array())

        // Points (each point is 2 floats: x, y)
        path.points.forEach { point ->
            pathData.write(ByteBuffer.allocate(8).putFloat(point.x).putFloat(point.y).array())
        }
    }

    return pathData.toByteArray()
}

// Convert stored bytes back to paths for rendering
fun bytesToPaths(bytes: ByteArray): MutableList<PathData> {
    val paths = mutableListOf<PathData>()
    val buffer = ByteBuffer.wrap(bytes)

    try {
        // Number of paths
        val numPaths = buffer.getInt()

        // Read each path
        for (i in 0 until numPaths) {
            // Color
            val red = buffer.getFloat()
            val green = buffer.getFloat()
            val blue = buffer.getFloat()
            val alpha = buffer.getFloat()
            val color = Color(red, green, blue, alpha)

            // Stroke width
            val strokeWidth = buffer.getFloat()

            // Number of points
            val numPoints = buffer.getInt()

            // Points
            val points = mutableListOf<Offset>()
            for (j in 0 until numPoints) {
                val x = buffer.getFloat()
                val y = buffer.getFloat()
                points.add(Offset(x, y))
            }

fun isPointNearPath(point: Offset, path: Path, threshold: Float = 20f): Boolean {
    val pathBounds = path.getBounds()
    return (point.x in (pathBounds.left - threshold)..(pathBounds.right + threshold) &&
            point.y in (pathBounds.top - threshold)..(pathBounds.bottom + threshold))
}

            paths.add(PathData(points, color, strokeWidth))
        }
    } catch (e: Exception) {
        println("Error reading path data: ${e.message}")
        // Return empty paths on error
    }

    return paths
}

@Composable
fun EditableTextBox(
    block: ContentBlock,
    onTextChange: (String) -> Unit,
) {

    var startText: String = when (block.blockType) {
        BlockType.PLAINTEXT -> (block as TextBlock).text
        BlockType.MARKDOWN -> (block as MarkdownBlock).text
        BlockType.CODE -> (block as CodeBlock).text
        BlockType.MATH -> (block as MathBlock).text
        else -> ""
    }

    var textFieldValue by remember { mutableStateOf<String>(startText) }
    val focusRequester = remember { FocusRequester() } // Controls focus

    var textStyle = when (block.blockType) {
        BlockType.CODE -> TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.W200,
            color = Color.Green
        )
        else -> TextStyle.Default
    }

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() }, // Prevents ripple effect
                indication = null
            ) { focusRequester.requestFocus() } // Ensure click brings focus
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .background(if (block.blockType == BlockType.CODE) Colors.black else Colors.white)
    ) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it
                onTextChange(it)
            },
            modifier = Modifier
                .background(if (block.blockType == BlockType.CODE) Colors.black else Colors.white)
                .fillMaxWidth()
                .focusRequester(focusRequester) // Attach focus requester to manage focus
                .onKeyEvent { true }, // prevents weird visual glitch from happening
            textStyle = textStyle,
            cursorBrush = SolidColor(if (block.blockType == BlockType.CODE) Color.Green else Colors.black)
        )
    }
}

@Composable
fun BlockFrameMenu(index: Int, buttonFuncs: Map<String, (Int) -> Unit>, numContentBlocks: Int,
                   gluedAbove: Boolean, gluedBelow: Boolean) {
    // BlockFrameMenu consists of the buttons that do actions for all blocks (i.e. all types of ContentBlocks)

    var hoveredGlueButton by remember { mutableStateOf<String?>(null) }
    var hoveredOtherButton by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        @Composable
        fun MenuButton(onClick: ((Int) -> Unit)?, icon: ImageVector, desc: String, disabledCond: Boolean = false) {
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()

            // toggle hoveredButton state given the button we are hovering over
            // need to consider two cases for glue/other buttons (mutable states)
            if (desc.startsWith("Toggle Glue")) {
                if (isHovered && hoveredGlueButton != desc) {
                    hoveredGlueButton = desc
                }
                else if (!isHovered && hoveredGlueButton == desc) {
                    hoveredGlueButton = null
                }
            } else {
                if (isHovered && hoveredOtherButton != desc) {
                    hoveredOtherButton = desc
                }
                else if (!isHovered && hoveredOtherButton == desc) {
                    hoveredOtherButton = null
                }
            }

            IconButton(
                onClick = { onClick?.invoke(index) },
                colors = iconButtonColours(),
                modifier = Modifier.hoverable(interactionSource = interactionSource).size(45.dp),
                enabled = !disabledCond
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = desc,
                    modifier = Modifier.size(30.dp)
                )
            }
        }


        // buttons for glue toggling
        Box (
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // toggle glue with block above
                    MenuButton(
                        buttonFuncs["Toggle Glue Above"],
                        Icons.Filled.ArrowDropUp,
                        "Toggle Glue Above",
                        disabledCond = (index == 0)
                    )
                    // toggle glue with block below
                    MenuButton(
                        buttonFuncs["Toggle Glue Below"],
                        Icons.Filled.ArrowDropDown,
                        "Toggle Glue Below",
                        disabledCond = (index == numContentBlocks - 1)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (hoveredGlueButton != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(Colors.darkGrey.copy(alpha = 0.8f))
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = hoveredGlueButton.toString(),
                        fontSize = 12.sp,
                        color = Colors.white.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // buttons for block manipulating (i.e. not glue)
        Box (
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // duplicate current block
                    MenuButton(
                        buttonFuncs["Duplicate Block"],
                        Icons.Default.CopyAll,
                        "Duplicate Block"
                    )
                    // move current block up
                    MenuButton(
                        buttonFuncs["Move Block Up"],
                        if (gluedAbove) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardDoubleArrowUp,
                        if (gluedAbove) "Move Block Up" else "Move Glued Block Up",
                        disabledCond = (index == 0)
                    )
                    // move current block down
                    MenuButton(
                        buttonFuncs["Move Block Down"],
                        if (gluedBelow) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardDoubleArrowDown,
                        if (gluedBelow) "Move Block Down" else "Move Glued Block Down",
                        disabledCond = (index == numContentBlocks - 1)
                    )
                    // delete current block
                    MenuButton(
                        buttonFuncs["Delete Block"],
                        Icons.Default.Delete,
                        "Delete"
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
            if (hoveredOtherButton != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(Colors.darkGrey.copy(alpha = 0.8f))
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = hoveredOtherButton.toString(),
                        fontSize = 12.sp,
                        color = Colors.white.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}



@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AddBlockFrameButton(article: Note, index: Int, direction: String, selectAtIndex: (Int) -> Unit, board: Board) {
    // these buttons add a new (empty) ContentBlock above/below (depends on direction) the currently selected block
    // by default, insertBlock() creates a new Text block (assume that people use this the most)
    var showBlockTypes by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(
            onClick = { showBlockTypes = !showBlockTypes },
            colors = iconButtonColours(),
            modifier = Modifier.hoverable(interactionSource = interactionSource).size(45.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Block Type",
                modifier = Modifier.size(35.dp)
            )
        }

        // Tooltip text that follows the mouse cursor
        if (isHovered) {
            Box(
                modifier = Modifier
                    .align(if (direction == "DOWN") Alignment.BottomCenter else Alignment.TopCenter)
                    .background(Colors.darkGrey.copy(alpha = 0.8f))
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Add New Block",
                    fontSize = 12.sp,
                    color = Colors.white.copy(alpha = 0.8f)
                )
            }
        }
    }

    if (showBlockTypes) { InsertBlockTypesMenu(article, index, direction, selectAtIndex, board) }
}

@Composable
fun InsertBlockTypesMenu(article: Note, index: Int, direction: String, selectAtIndex: (Int) -> Unit, board: Board) {
    val atAddIndex = when (direction) {
        "UP" -> index
        else -> index + 1 // the "DOWN" case
    }

    println("DEBUG: CLICKED for index $index, direction $direction")

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        for (type in BlockType.entries) {
            TextButton(
                colors = textButtonColours(),
                onClick = {
                    articleModel.addBlock(atAddIndex, direction, type, article, board)
                    selectAtIndex(atAddIndex)
                }
            ) { Text(type.name) }
        }
    }
}
