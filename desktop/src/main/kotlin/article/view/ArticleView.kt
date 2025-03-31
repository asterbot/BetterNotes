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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import article.entities.*
import boards.entities.Board
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import individual_board.entities.Note
import individual_board.view.IndividualBoardScreen
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import shared.*
import space.kscience.kmath.ast.parseMath
import space.kscience.kmath.ast.rendering.FeaturedMathRendererWithPostProcess
import space.kscience.kmath.ast.rendering.LatexSyntaxRenderer
import space.kscience.kmath.ast.rendering.renderWithStringBuilder
import java.io.File
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

    var contentBlocksList by remember { mutableStateOf(articleViewModel) }

    var prevSelectedBlock by remember { mutableStateOf<Int?>(null) }
    var selectedBlock by remember { mutableStateOf<Int?>(null) }
    var currEditedText = remember {mutableStateOf<String?>(null) } // keep track of the text currently being changed
    var debugState by remember { mutableStateOf(false) }

    // detect when we change blocks (i.e, change focus)
    // we will only push to the db when focus shifts, so that we don't spam the db with each character change
    LaunchedEffect(selectedBlock) {
        println("In LaunchedEffect (switching block focus)")
        if (prevSelectedBlock != selectedBlock) {
            if (prevSelectedBlock != null && currEditedText.value != null) {
                println("I moved from block $prevSelectedBlock to block $selectedBlock")
                println("The text [${currEditedText.value}] gets pushed to block $prevSelectedBlock")
                val currBlock = contentBlocksList.contentBlocksList[prevSelectedBlock!!]
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

    fun selectAtIndex(index: Int) { selectedBlock = index } // used with inserting blocks

    val menuButtonFuncs: Map<String, (Int) -> Unit> = mapOf(
        "Toggle Glue Above" to { index ->
            articleModel.toggleGlueUpwards(index, article, board)
        },
        "Toggle Glue Below" to { index ->
            articleModel.toggleGlueDownwards(index, article, board)
        },
        "Duplicate Block" to { index ->
            articleModel.duplicateBlock(index, article, board)
            selectedBlock = index + 1
        },
        "Move Block Up" to { index ->
            val newIndex = articleModel.moveBlockUp(index, article, board)
            selectedBlock = (if (newIndex == null) selectedBlock else newIndex)
        },
        "Move Block Down" to { index ->
            val newIndex = articleModel.moveBlockDown(index, article, board)
            selectedBlock = (if (newIndex == null) selectedBlock else newIndex)
        },
        "Delete Block" to { index ->
            articleModel.deleteBlock(index, article, board)
            selectedBlock = null
        }
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(5.dp)
            .clickable(
                // passing in empty MutableInteractionSource means no ripple effect (i.e. box not grayed out when hovered over)
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { selectedBlock = null }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text( // article name
                text = "Article: ${article.title}",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )
            Text( // title
                text = "From Board ${board.name}",
                fontSize = 18.sp,
            )

            // row containing any useful functionality (as buttons)
            // so far, "return to previous course" and "DEBUG" buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(
                    colors = textButtonColours(),
                    onClick = { ScreenManager.push(navigator, IndividualBoardScreen(board)) }
                ) { Text("Back to current course") }
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
                                    println("\tCANVAS HAS ${(contentBlock as CanvasBlock).paths.size} PATHS")
                                } else {
                                    println("\t$contentBlock")
                                }
                                println("Glued Below? ${contentBlock.gluedBelow}")
                            }
                        }
                        // println("FROM VIEWMODEL: ${contentBlocksList.contentBlocksList}")
                        println("FROM VIEWMODEL:")
                        for (contentBlock in contentBlocksList.contentBlocksList) {
                            println("Glued Above? ${contentBlock.gluedAbove}")
                            if (contentBlock.blockType == BlockType.CANVAS) {
                                println("\tCANVAS HAS ${(contentBlock as CanvasBlock).paths.size} PATHS")
                            } else {
                                println("\t$contentBlock")
                            }
                            println("Glued Below? ${contentBlock.gluedBelow}")
                        }
                        debugState = !debugState
                    }
                ) { Text(text = "DEBUG") }
            }

            if (contentBlocksList.contentBlocksList.isEmpty()) {
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
                    contentBlocksList.contentBlocksList, // itemsIndexed iterates over this collection
                    key = { index: Int, block: ContentBlock -> block.id } // Jetpack Compose uses keys to track recompositions
                ) { index: Int, block: ContentBlock ->
                    {} // honestly I'm not sure what this does, but it's needed

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
                        numContentBlocks = contentBlocksList.contentBlocksList.size,
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
            if (contentBlocksList.contentBlocksList.size == 0) {
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
    selectAtIndex: (Int) -> Unit,
    board: Board,
    gluedAbove: Boolean,
    gluedBelow: Boolean,
    numContentBlocks: Int,
    currEditedText: MutableState<String?>,
    debugState: Boolean,
) {
    var block by remember { mutableStateOf(articleViewModel.contentBlocksList[blockIndex]) }

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
                            .background(if (glueParam) Colors.lightTeal else Colors.medTeal)
                    )
                }
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((if (glueParam) 0 else 25).dp)
                        .background(Colors.lightTeal)
                )
                if (dir == "Below") {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(if (glueParam) Colors.lightTeal else Colors.medTeal)
                    )
                }
            }


            GluedBlockBorder(gluedAbove, "Above")

            Box(
                modifier = Modifier
                    .background(Colors.lightTeal)
                    .padding(horizontal=5.dp)
                    .clip(RoundedCornerShape(5.dp)),
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
                            modifier = Modifier.background(Color.White).fillMaxWidth()
                        ) {
                            Text(text=(block as TextBlock).text)
                        }
                    }

                    if (block.blockType == BlockType.CODE && !isSelected) {
                        Box(
                            modifier = Modifier.background(Colors.black).fillMaxWidth()
                        ) {
                            Text(
                                text=(block as CodeBlock).text,
                                color = Color.Green,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.W200
                            )
                        }
                    }


                    if (block.blockType == BlockType.MARKDOWN && !isSelected) {
                        val markdownHandler = MarkdownHandler((block as MarkdownBlock).text)
                        markdownHandler.renderMarkdown()
                    }

                    if (block.blockType == BlockType.MATH && !isSelected) {
                        // Render math here
                        var latex = ""
                        try {
                            // Try to parse the "flaky" math
                            val rawMath = ((block as MathBlock).text).parseMath()
                            val syntax = FeaturedMathRendererWithPostProcess.Default.render(rawMath)
                            latex = LatexSyntaxRenderer.renderWithStringBuilder(syntax)
                        } catch (e: Exception) {
                            // Parsing error, render latex as is
                            latex = (block as MathBlock).text
                        }
                        LatexRenderer(latex)
                    }

                    if (block.blockType == BlockType.CANVAS) {
                        EditableCanvas(
                            block = block,
                            onCanvasUpdate = { paths, height ->
                                articleModel.saveBlock(
                                    blockIndex,
                                    pathsContent = paths,
                                    canvasHeight = height,
                                    gluedAbove = gluedAbove,
                                    gluedBelow = gluedBelow,
                                    article = article,
                                    board = board
                                )
                            }
                        )
                    }


                    if (block.blockType == BlockType.MEDIA) {
                        addMedia(isSelected)
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


fun cropImage() {

}

@Composable
fun addMedia(isSelected: Boolean = true) {
    var filePath by remember { mutableStateOf<String?>(null) }

    val launcher = rememberFilePickerLauncher { file ->
        if (file != null) {
            filePath = file.absolutePath()
            println(filePath)
        } else {
            println("No file selected")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isSelected && filePath == null) {
            TextButton(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = textButtonColours(),
                onClick = { launcher.launch() },
            ) {
                Text("Pick a file")
            }
        }

        filePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                // Read file bytes and create an ImageBitmap.
                val imageBytes = file.readBytes()
                val imageBitmap = org.jetbrains.skia.Image.makeFromEncoded(imageBytes).toComposeImageBitmap()
                Image(
                    painter = BitmapPainter(image = imageBitmap),
                    contentDescription = "everyone's favorite bird",
                    modifier = Modifier.fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    cropImage()
                                    println("crop mode :)")
                                }
                            )
                        }
                )
            } else {
                Text(text = "File not found.")
            }
        }
    }
}


//@Composable
//fun EditableCanvas(
//    block: ContentBlock,
//    canvasHeight: Dp,
//    onCanvasUpdate: (MutableList<Path>) -> Unit
//) {
//    var startPaths: MutableList<Path> = when (block.blockType) {
//        BlockType.CANVAS -> { (block as CanvasBlock).paths }
//        else -> mutableListOf()
//    }
//
//    val paths = remember { mutableStateListOf<Path>().apply { addAll(startPaths) } }
//    var currentPath by remember { mutableStateOf(Path()) }
//    var isDrawing by remember { mutableStateOf(false) }
//    var isOutsideBox by remember { mutableStateOf(false) }
//    var isErasing by remember { mutableStateOf(false) } // Eraser mode toggle
//
//    var boxWidth by remember { mutableStateOf(canvasHeight) }
//    var boxHeight by remember { mutableStateOf(canvasHeight) }
//    var isResizing by remember { mutableStateOf(false) }
//    var resizeHandleSize = 10.dp
//
//
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(boxHeight)
//                .background(Color.White)
//                .onGloballyPositioned { coordinates ->
//                    boxWidth = coordinates.size.width.dp
//                    boxHeight = coordinates.size.height.dp
//                }
//                .pointerInput(Unit) {
//                    detectDragGestures(
//                        onDragStart = { offset ->
//                            if (offset.y.dp >= boxHeight - resizeHandleSize && offset.y.dp <= boxHeight + resizeHandleSize) {
//                                isResizing = true
//                                isDrawing = false
//                            }
//                            else if (offset.x.dp in 0.dp..boxWidth && offset.y.dp in 0.dp..boxHeight) {
//                                isDrawing = true
//                                isOutsideBox = false
//
//                                if (isErasing) {
//                                    // Erase paths near the cursor
//                                    paths.removeAll { path -> isPointNearPath(offset, path) }
//                                } else {
//                                    currentPath = Path().apply { moveTo(offset.x, offset.y) }
//                                }
//                            }
//                        },
//                        onDrag = { change, dragAmount ->
//                            if (isResizing) {
//                                boxHeight = (boxHeight + dragAmount.y.dp)
//                            } else {
//                                val isInside = change.position.x.dp in 0.dp..boxWidth &&
//                                        change.position.y.dp in 0.dp..boxHeight
//
//                                if (isInside) {
//                                    if (isErasing) {
//                                        // Remove paths near the cursor position
//                                        paths.removeAll { path -> isPointNearPath(change.position, path) }
//                                    } else {
//                                        if (isOutsideBox) {
//                                            currentPath = Path().apply { moveTo(change.position.x, change.position.y) }
//                                            isOutsideBox = false
//                                        } else {
//                                            currentPath = Path().apply {
//                                                addPath(currentPath)
//                                                lineTo(change.position.x, change.position.y)
//                                            }
//                                        }
//                                    }
//                                } else {
//                                    if (!isOutsideBox && !isErasing) {
//                                        paths.add(currentPath)
//                                        onCanvasUpdate(paths)
//                                        currentPath = Path()
//                                        isOutsideBox = true
//                                    }
//                                }
//                            }
//                        },
//                        onDragEnd = {
//                            if (isResizing){
//                                isResizing = false
//                            }
//                            else if (!isOutsideBox && !isErasing) {
//                                paths.add(currentPath)
//                                onCanvasUpdate(paths)
//                            }
//                            isDrawing = false
//                            currentPath = Path()
//                        }
//                    )
//                }
//
//        ) {
//            Button(
//                onClick = { isErasing = !isErasing },
//                colors = ButtonDefaults.buttonColors(
//                    backgroundColor = Colors.medTeal,
//                    contentColor = Colors.white
//                ),
//                shape = CircleShape,
//                contentPadding = PaddingValues(10.dp),
//            ) { Text(if (!isErasing) "Erase" else "Draw" ) }
//
//            // drawing the existing path
//            Canvas(modifier = Modifier.fillMaxSize()) {
//                clipRect(0f, 0f, boxWidth.toPx(), boxHeight.toPx()) {
//                    paths.forEach { path ->
//                        drawPath(
//                            path = path,
//                            color = Color.Black,
//                            style = Stroke(width = 2f)
//                        )
//                    }
//                }
//
//                // drawing the dragging path
//                if (isDrawing && !isErasing) {
//                    drawPath(
//                        path = currentPath,
//                        color = Color.Black,
//                        style = Stroke(width = 2f)
//                    )
//                }
//            }
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(resizeHandleSize)
//                    .align(Alignment.BottomCenter)
//                    .background(Color.LightGray.copy(alpha = 0.5f))
//            )
//        }
//    }



@Composable
fun EditableCanvas(
    block: ContentBlock,
    onCanvasUpdate: (MutableList<Path>, Int) -> Unit
) {

    var startPaths: MutableList<Path> = when (block.blockType) {
        BlockType.CANVAS -> { (block as CanvasBlock).paths }
        else -> mutableListOf()
    }


    val paths = remember { mutableStateListOf<Path>().apply { addAll(startPaths) } }
    var currentPath by remember { mutableStateOf(Path()) }
    var isDrawing by remember { mutableStateOf(false) }
    var isOutsideBox by remember { mutableStateOf(false) }
    var isErasing by remember { mutableStateOf(false) }

    var canvasHeight by remember { mutableStateOf((block as CanvasBlock).canvasHeight) }
    val resizeThreshold = LocalDensity.current.run { 30 }
    var isResizing by remember {mutableStateOf(false)}

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(canvasHeight.dp)
            .background(Color.White)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val boxHeight = size.height.toFloat()
                        val isNearBottomEdge = offset.y in (boxHeight - resizeThreshold)..boxHeight
                        if (isNearBottomEdge) {
                            isResizing = true
                            isDrawing = false
                            println("DEBUG: RESIZING CANVAS")
                        }
                        else {
                            isDrawing = true
                            isOutsideBox = false

                            if (isErasing) {
                                // Erase paths near the cursor
                                paths.removeAll { path -> isPointNearPath(offset, path) }
                            } else {
                                currentPath = Path().apply { moveTo(offset.x, offset.y) }
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        if (isResizing) {
                            val newHeight = max(50, (canvasHeight + 0.5*change.positionChange().y).toInt())
                            canvasHeight = newHeight
                        }
                        else {
                            val boxWidth = size.width
                            val boxHeight = size.height

                            val isInside = change.position.x in 0f..boxWidth.toFloat() &&
                                    change.position.y in 0f..boxHeight.toFloat()

                            if (isInside) {
                                if (isErasing) {
                                    // Remove paths near the cursor position
                                    paths.removeAll { path -> isPointNearPath(change.position, path) }
                                } else {
                                    if (isOutsideBox) {
                                        currentPath = Path().apply { moveTo(change.position.x, change.position.y) }
                                        isOutsideBox = false
                                    } else {
                                        currentPath = Path().apply {
                                            addPath(currentPath)
                                            lineTo(change.position.x, change.position.y)
                                        }
                                    }
                                }
                            } else {
                                if (!isOutsideBox && !isErasing) {
                                    paths.add(currentPath)
                                    onCanvasUpdate(paths, canvasHeight)
                                    currentPath = Path()
                                    isOutsideBox = true
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        if (!isOutsideBox && !isErasing) {
                            paths.add(currentPath)
                            onCanvasUpdate(paths, canvasHeight)
                        }
                        isDrawing = false
                        isResizing = false
                        currentPath = Path()
                    }
                )
            }
    ) {
        TextButton(
            colors = textButtonColours(),
            onClick = { isErasing = !isErasing },
        ) { Text(if (!isErasing) "Erase" else "Draw") }

        // drawing the existing path
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            paths.forEach { path ->
                drawPath(
                    path = path,
                    color = Color.Black,
                    style = Stroke(width = 2f)
                )
            }

            // drawing the dragging path
            if (isDrawing && !isErasing) {
                drawPath(
                    path = currentPath,
                    color = Color.Black,
                    style = Stroke(width = 2f)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Colors.lightGrey)
                .align(Alignment.BottomEnd)
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
}

//@Composable
//fun EditableCanvas(
//    block: ContentBlock,
//    canvasHeight: Dp,
//    onCanvasUpdate: (MutableList<Path>) -> Unit
//) {
//    var startPaths: MutableList<Path> = when (block.blockType) {
//        BlockType.CANVAS -> { (block as CanvasBlock).paths }
//        else -> mutableListOf()
//    }
//
//    val paths = remember { mutableStateListOf<Path>().apply { addAll(startPaths) } }
//    var currentPath by remember { mutableStateOf(Path()) }
//    var isDrawing by remember { mutableStateOf(false) }
//    var isOutsideBox by remember { mutableStateOf(false) }
//    var isErasing by remember { mutableStateOf(false) } // Eraser mode toggle
//
//
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(canvasHeight)
//            .background(Color.White)
//            .pointerInput(Unit) {
//                detectDragGestures(
//                    onDragStart = { offset ->
//                        if (offset.x in 0f..size.width.toFloat() && offset.y in 0f..size.height.toFloat()) {
//                            isDrawing = true
//                            isOutsideBox = false
//
//                            if (isErasing) {
//                                // Erase paths near the cursor
//                                paths.removeAll { path -> isPointNearPath(offset, path) }
//                            } else {
//                                currentPath = Path().apply { moveTo(offset.x, offset.y) }
//                            }
//                        }
//                    },
//                    onDrag = { change, _ ->
//                        val boxWidth = size.width
//                        val boxHeight = size.height
//
//                        val isInside = change.position.x in 0f..boxWidth.toFloat() &&
//                                change.position.y in 0f..boxHeight.toFloat()
//
//                        if (isInside) {
//                            if (isErasing) {
//                                // Remove paths near the cursor position
//                                paths.removeAll { path -> isPointNearPath(change.position, path) }
//                            } else {
//                                if (isOutsideBox) {
//                                    currentPath = Path().apply { moveTo(change.position.x, change.position.y) }
//                                    isOutsideBox = false
//                                } else {
//                                    currentPath = Path().apply {
//                                        addPath(currentPath)
//                                        lineTo(change.position.x, change.position.y)
//                                    }
//                                }
//                            }
//                        } else {
//                            if (!isOutsideBox && !isErasing) {
//                                paths.add(currentPath)
//                                onCanvasUpdate(paths)
//                                currentPath = Path()
//                                isOutsideBox = true
//                            }
//                        }
//                    },
//                    onDragEnd = {
//                        if (!isOutsideBox && !isErasing) {
//                            paths.add(currentPath)
//                            onCanvasUpdate(paths)
//                        }
//                        isDrawing = false
//                        currentPath = Path()
//                    }
//                )
//            }
//
//    ) {
//        Button(
//            onClick = { isErasing = !isErasing },
//            colors = ButtonDefaults.buttonColors(
//                backgroundColor = Colors.medTeal,
//                contentColor = Colors.white
//            ),
//            shape = CircleShape,
//            contentPadding = PaddingValues(10.dp),
//        ) { Text(if (!isErasing) "Erase" else "Draw" ) }
//
//        // drawing the existing path
//        Canvas(modifier = Modifier.fillMaxSize()) {
//            paths.forEach { path ->
//                drawPath(
//                    path = path,
//                    color = Color.Black,
//                    style = Stroke(width = 2f)
//                )
//            }
//
//            // drawing the dragging path
//            if (isDrawing && !isErasing) {
//                drawPath(
//                    path = currentPath,
//                    color = Color.Black,
//                    style = Stroke(width = 2f)
//                )
//            }
//        }
//
//    }
//}


fun isPointNearPath(point: Offset, path: Path, threshold: Float = 20f): Boolean {
    val pathBounds = path.getBounds()
    return (point.x in (pathBounds.left - threshold)..(pathBounds.right + threshold) &&
            point.y in (pathBounds.top - threshold)..(pathBounds.bottom + threshold))
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
        modifier = Modifier.clickable { focusRequester.requestFocus() } // Ensure click brings focus
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
                modifier = Modifier.hoverable(interactionSource = interactionSource).size(40.dp),
                enabled = !disabledCond
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = desc,
                    modifier = Modifier.size(25.dp)
                )
            }
        }


        // buttons for glue toggling
        Box (
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Row(
                modifier = Modifier.align(Alignment.TopStart),
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
                    disabledCond = (index == numContentBlocks-1)
                )
            }
            if (hoveredGlueButton != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(Colors.darkGrey.copy(alpha = 0.9f))
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = hoveredGlueButton.toString(),
                        fontSize = 12.sp,
                        color = Colors.white.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // buttons for block manipulating (i.e. not glue)
        Box (
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
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
                    disabledCond = (index == numContentBlocks-1)
                )
                // delete current block
                MenuButton(
                    buttonFuncs["Delete Block"],
                    Icons.Default.Delete,
                    "Delete"
                )
            }
            if (hoveredOtherButton != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(Colors.darkGrey.copy(alpha = 0.9f))
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = hoveredOtherButton.toString(),
                        fontSize = 12.sp,
                        color = Colors.white.copy(alpha = 0.9f)
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
            modifier = Modifier.hoverable(interactionSource = interactionSource).size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Block Type",
                modifier = Modifier.size(30.dp)
            )
        }

        // Tooltip text that follows the mouse cursor
        if (isHovered) {
            Box(
                modifier = Modifier
                    .align(if (direction == "DOWN") Alignment.BottomCenter else Alignment.TopCenter)
                    .background(Colors.darkGrey.copy(alpha=0.9f))
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Add New Block",
                    fontSize = 12.sp,
                    color = Colors.white.copy(alpha=0.9f)
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
