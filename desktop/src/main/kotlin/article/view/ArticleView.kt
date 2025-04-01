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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.BitmapPainter
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
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import individual_board.entities.Note
import individual_board.view.IndividualBoardScreen
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.exists
import org.jetbrains.skia.Image.Companion.makeFromBitmap
import org.jetbrains.skia.Image.Companion.makeFromEncoded
import shared.*
import space.kscience.kmath.ast.parseMath
import space.kscience.kmath.ast.rendering.FeaturedMathRendererWithPostProcess
import space.kscience.kmath.ast.rendering.LatexSyntaxRenderer
import space.kscience.kmath.ast.rendering.renderWithStringBuilder
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.skia.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.ByteBuffer

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

    var selectedBlock by remember { mutableStateOf<Int?>(null) }
    var debugState by remember { mutableStateOf(false) }

    fun selectAtIndex(index: Int) { selectedBlock = index } // used with inserting blocks

    val menuButtonFuncs: Map<String, (Int) -> Unit> = mapOf(
        "Duplicate Block" to { index ->
            articleModel.duplicateBlock(index, article, board)
            // articleModel.duplicateBlock(index, article)
            selectedBlock = index + 1
        },
        "Move Block Up" to { index ->
            articleModel.moveBlockUp(index, article, board)
            selectedBlock = index - 1
        },
        "Move Block Down" to { index ->
            articleModel.moveBlockDown(index, article, board)
            selectedBlock = index + 1
        },
        "Delete Block" to { index ->
            articleModel.deleteBlock(index, article, board)
            selectedBlock = null
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
            Row(
                // TODO: buttons for main navigation (e.g. back to course, other articles, ...)
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // insert TextBlock at beginning
                TextButton(
                    colors = textButtonColours(),
                    onClick = { navigator.push(IndividualBoardScreen(board)) }
                ) { Text("Back to current course") }
                Button(
                    colors = textButtonColours(),
                    onClick = {
                        println("DEBUG (THE BLOCKS):")
                        println("FROM MODEL: ${articleModel.contentBlockDict}")
                        articleModel.contentBlockDict[article.id]?.let {articleContentBlocks ->
                            for (contentBlock in articleContentBlocks) {
                                if (contentBlock.blockType == BlockType.CANVAS) {
                                    println()
                                    println("\tCANVAS HAS ${(contentBlock as CanvasBlock).bList.size} PATHS")
                                } else if (contentBlock.blockType == BlockType.MEDIA) {
                                    println("\tMEDIA HAS ${((contentBlock as MediaBlock).bList.size)} BYTE ARRAY")
                                }
                                else {
                                    println("\t$contentBlock")
                                }
                            }
                        }
                        // println("FROM VIEWMODEL: ${contentBlocksList.contentBlocksList}")
                        for (contentBlock in contentBlocksList.contentBlocksList) {
                            if (contentBlock.blockType == BlockType.CANVAS) {
                                println("\tCANVAS HAS ${(contentBlock as CanvasBlock).bList.size} PATHS")
                            } else if (contentBlock.blockType == BlockType.MEDIA) {
                                println("\tMEDIA HAS ${((contentBlock as MediaBlock).bList.size)} BYTE ARRAY")
                            }
                            else {
                                println("\t$contentBlock")
                            }
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
                modifier = Modifier.fillMaxSize().padding(25.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
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
                        debugState = debugState,
                        board = board
                    )
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
    debugState: Boolean,
    board: Board
) {
    var block by remember { mutableStateOf(articleViewModel.contentBlocksList[blockIndex]) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onBlockClick() }
            .clip(RoundedCornerShape(10.dp))
    ) {
        Box(
            modifier = Modifier
                .background(Colors.lightTeal)
                .padding(horizontal = 15.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(10.dp)),
        ) {

            if (isSelected) {
                BlockFrameMenu(blockIndex, menuButtonFuncs)
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
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
                    )) {
                    if (!((block.blockType == BlockType.MARKDOWN || block.blockType == BlockType.MATH) && !isSelected)) {
                        EditableTextBox(
                            block = block,
                            onTextChange = {
                                if (block.blockType == BlockType.CODE) {
                                    articleModel.saveBlock(blockIndex, stringContent = it, article = article,
                                        language = (block as CodeBlock).language, board = board)
                                }
                                else {
                                    articleModel.saveBlock(blockIndex, stringContent = it, article = article, board = board)
                                }
                            }
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
                    try{
                        // Try to parse the "flaky" math
                        val rawMath = ((block as MathBlock).text).parseMath()
                        val syntax = FeaturedMathRendererWithPostProcess.Default.render(rawMath)
                        latex = LatexSyntaxRenderer.renderWithStringBuilder(syntax)
                    }
                    catch(e: Exception){
                        // Parsing error, render latex as is
                        latex = (block as MathBlock).text
                    }
                    LatexRenderer(latex)
                }



                if (block.blockType == BlockType.CANVAS) {
                    EditableCanvas(
                        block = block,
                        onCanvasUpdate = {bList, height ->
                            articleModel.saveBlock(blockIndex, bList=bList, canvasHeight=height, article=article, board = board)
                        }
                    )
                }

                if (block.blockType == BlockType.MEDIA) {
                    addMedia( block = block,
                        isSelected = isSelected,
                        onMediaUpdate = {bList ->
                            articleModel.saveBlock(blockIndex, bList = bList, article = article,board = board)
                        }
                    )
                }

                if (isSelected) {
                    AddBlockFrameButton(article, blockIndex, "DOWN", selectAtIndex, board)
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
    } catch (e: IOException) {
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
        modifier = Modifier.fillMaxSize()
    ) {
        if (isSelected && filePath == null && imageBytes.isEmpty()) {
            TextButton(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = textButtonColours(),
                onClick = { launcher.launch() },
            ) {
                Text("Pick a file")
            }
        }
    }

    if (imageBytes.isNotEmpty()) {
        onMediaUpdate(imageBytes)
//        val imageBitmap = makeFromEncoded(imageBytes.toByteArray()).toComposeImageBitmap()
        val imageBitmap = loadImageFromBytes(imageBytes.toByteArray())

        Image(
            bitmap = imageBitmap!!,
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
        println("File cannot be displayed")
    }
}

// original version with path and canvas
//@Composable
//fun EditableCanvas(
//    block: ContentBlock,
//    onCanvasUpdate: (MutableList<Path>, Int) -> Unit
//) {
//
//    var startPaths: MutableList<Path> = when (block.blockType) {
//        BlockType.CANVAS -> { (block as CanvasBlock).paths }
//        else -> mutableListOf()
//    }
//
//    val paths = remember { mutableStateListOf<Path>().apply { addAll(startPaths) } }
//    var currentPath by remember { mutableStateOf(Path()) }
//    var isDrawing by remember { mutableStateOf(false) }
//    var isOutsideBox by remember { mutableStateOf(false) }
//    var isErasing by remember { mutableStateOf(false) }
//
//    var canvasHeight by remember { mutableStateOf((block as CanvasBlock).canvasHeight) }
//    val resizeThreshold = LocalDensity.current.run { 30 }
//    var isResizing by remember {mutableStateOf(false)}
//
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(canvasHeight.dp)
//            .background(Color.White)
//            .pointerInput(Unit) {
//                detectDragGestures(
//                    onDragStart = { offset ->
//                        val boxHeight = size.height.toFloat()
//                        val isNearBottomEdge = offset.y in (boxHeight - resizeThreshold)..boxHeight
//                        if (isNearBottomEdge) {
//                            isResizing = true
//                            isDrawing = false
//                            println("DEBUG: RESIZING CANVAS")
//                        }
//                        else {
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
//                        if (isResizing) {
//                            val newHeight = max(50, (canvasHeight + 0.5*change.positionChange().y).toInt())
//                            canvasHeight = newHeight
//                        }
//                        else {
//                            val boxWidth = size.width
//                            val boxHeight = size.height
//
//                            val isInside = change.position.x in 0f..boxWidth.toFloat() &&
//                                    change.position.y in 0f..boxHeight.toFloat()
//
//                            if (isInside) {
//                                if (isErasing) {
//                                    // Remove paths near the cursor position
//                                    paths.removeAll { path -> isPointNearPath(change.position, path) }
//                                } else {
//                                    if (isOutsideBox) {
//                                        currentPath = Path().apply { moveTo(change.position.x, change.position.y) }
//                                        isOutsideBox = false
//                                    } else {
//                                        currentPath = Path().apply {
//                                            addPath(currentPath)
//                                            lineTo(change.position.x, change.position.y)
//                                        }
//                                    }
//                                }
//                            } else {
//                                if (!isOutsideBox && !isErasing) {
//                                    paths.add(currentPath)
//                                    onCanvasUpdate(paths, canvasHeight)
//                                    currentPath = Path()
//                                    isOutsideBox = true
//                                }
//                            }
//                        }
//                    },
//                    onDragEnd = {
//                        if (!isOutsideBox && !isErasing) {
//                            paths.add(currentPath)
//                            onCanvasUpdate(paths, canvasHeight)
//                        }
//                        isDrawing = false
//                        isResizing = false
//                        currentPath = Path()
//                    }
//                )
//            }
//    ) {
//        TextButton(
//            colors = textButtonColours(),
//            onClick = { isErasing = !isErasing },
//        ) { Text(if (!isErasing) "Erase" else "Draw") }
//
//        // drawing the existing path
//        Canvas(
//            modifier = Modifier.fillMaxSize()
//        ) {
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
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .background(Colors.lightGrey)
//                .align(Alignment.BottomEnd)
//                .height((resizeThreshold / LocalDensity.current.density).dp)
//
//        ) {
//            Column(
//                modifier = Modifier.fillMaxSize(),
//                horizontalAlignment = Alignment.CenterHorizontally,
//                verticalArrangement = Arrangement.Center
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Menu,
//                    contentDescription = "Canvas Height Slider",
//                    modifier = Modifier.size(resizeThreshold.dp - 5.dp),
//                    tint = Colors.darkGrey
//                )
//            }
//        }
//    }
//}
//
//
//fun isPointNearPath(point: Offset, path: Path, threshold: Float = 20f): Boolean {
//    val pathBounds = path.getBounds()
//    return (point.x in (pathBounds.left - threshold)..(pathBounds.right + threshold) &&
//            point.y in (pathBounds.top - threshold)..(pathBounds.bottom + threshold))
//}


// lines are saved, best version yet
// block: ContentBlock, onCanvasUpdate: (MutableList<Byte>, Int) -> Unit
//@Composable
//fun EditableCanvas(block: ContentBlock, onCanvasUpdate: (MutableList<Byte>, Int) -> Unit) {
//
//    val initialBytes = when (block.blockType) {
//        BlockType.CANVAS -> (block as CanvasBlock).bList
//        else -> mutableListOf()
//    }
//    var imageBytes by remember { mutableStateOf(initialBytes) }
//
//
//    var paths by remember { mutableStateOf(mutableListOf<PathData>()) }
//    var currentPath by remember { mutableStateOf(Path()) }
//
//
//    var isDrawing by remember { mutableStateOf(false) }
//    var isOutsideBox by remember { mutableStateOf(false) }
//    var isErasing by remember { mutableStateOf(false) }
//
//    var canvasWidth by remember { mutableStateOf(0) }
//    var canvasHeight by remember { mutableStateOf(100) }
//    val resizeThreshold = LocalDensity.current.run { 30 }
//    var isResizing by remember { mutableStateOf(false) }
//
//    // var bitmap by remember { mutableStateOf(byteListToImageBitmap(imageBytes)) } // Initialize bitmap
//    var myColor by remember {mutableStateOf(Color.Black)}
//    var myStroke by remember {mutableStateOf(5f) }
//    var canvasPaint by remember { mutableStateOf(Paint().apply { color = myColor }) }
//
//
//    // color wheel box
//    val controller = rememberColorPickerController()
//    Box(
//        modifier = Modifier.fillMaxWidth().height(150.dp)
//    ) {
//        HsvColorPicker(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(450.dp)
//                .padding(10.dp),
//            controller = controller,
//            initialColor = Color.Black,
//            onColorChanged = { colorEnvelope: ColorEnvelope ->
//                // do something
//                myColor = colorEnvelope.color
//                println("color changed to $myColor!!")
//            }
//
//        )
//    }
//
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(canvasHeight.dp)
//            .background(Color.White)
//            .pointerInput(Unit) {
//                detectDragGestures(
//                    onDragStart = { offset ->
//                        val boxHeight = size.height.toFloat()
//                        val isNearBottomEdge = offset.y in (boxHeight - resizeThreshold)..boxHeight
//                        if (isNearBottomEdge) {
//                            isResizing = true
//                            isDrawing = false
//                            println("DEBUG: RESIZING CANVAS")
//                        } else {
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
//                        if (isResizing) {
//                            val newHeight = max(50, (canvasHeight + 0.5 * change.positionChange().y).toInt())
//                            canvasHeight = newHeight
//                        } else {
//                            val boxWidth = size.width
//                            val boxHeight = size.height
//                            canvasWidth = boxWidth
//
//                            val isInside = change.position.x in 0f..boxWidth.toFloat() &&
//                                    change.position.y in 0f..boxHeight.toFloat()
//
//                            if (isInside) {
//                                if (isErasing) {
//                                    // Remove paths near the cursor position
//                                    paths.removeAll { path -> isPointNearPath(change.position, path) }
//                                } else {
//                                    if (isOutsideBox) {
//                                        currentPath = Path().apply { moveTo(change.position.x, change.position.y) }
//                                        isOutsideBox = false
//                                    } else {
//                                        currentPath = Path().apply {
//                                            addPath(currentPath)
//                                            lineTo(change.position.x, change.position.y)
//                                        }
//                                    }
//                                }
//                            } else {
//                                if (!isOutsideBox && !isErasing) {
//                                    paths.add(currentPath) // Add the completed path
//                                    currentPath = Path()
//                                    isOutsideBox = true
//                                }
//                            }
//                        }
//                    },
//                    onDragEnd = {
//                        if (!isOutsideBox && !isErasing) {
//                            paths.add(currentPath) // Add the current path to the list when drag ends
//                        }
//                        isDrawing = false
//                        isResizing = false
//                        currentPath = Path()
//                    }
//                )
//            }
//    ) {
//        TextButton(
//            onClick = { isErasing = !isErasing },
//        ) { Text(if (!isErasing) "Erase" else "Draw") }
//
//        var bitmap by remember { mutableStateOf(ImageBitmap(canvasWidth, canvasHeight)) } // Initialize bitmap
//
//        Canvas(
//            modifier = Modifier.fillMaxSize()
//        ) {
//            val canvas = Canvas(bitmap) // Canvas to draw on the bitmap
//            // Drawing the paths stored in the list (for persistence)
//            paths.forEach { path ->
//                // drawPath(path, color = Color(0xFF00FF00), style = Stroke(width = 4f))
//                canvas.drawPath(path, canvasPaint)
//            }
//
//            // Drawing the dragging path (on top of previous paths)
//            if (isDrawing && !isErasing) {
//                // drawPath(currentPath, color = myColor, style = Stroke(width = 4f))
//                canvas.drawPath(currentPath, canvasPaint)
//            }
//            // imageBytes.addAll(imageBitmapToByteArray(bitmap).toMutableList())  // Convert the current image to bytes
//            onCanvasUpdate(imageBytes, canvasHeight)
//            val byteArrayOutputStream = ByteArrayOutputStream()
//        }
//    }
//
//    var isRendering by remember { mutableStateOf(false) }
//    TextButton(
//        onClick = {
//            isRendering = !isRendering
//        },
//    ) { Text("Render") }
//
//    if (isRendering) {
//        Image(
//            bitmap = loadImageFromBytes(imageBytes.toByteArray())!!,
//            contentDescription = "everyone's favorite bird",
//            modifier = Modifier.fillMaxSize()
//        )
//    }
//
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(Color.LightGray)
//            //.align(Alignment.BottomCenter)
//            .height((resizeThreshold / LocalDensity.current.density).dp)
//    ) {
//        Column(
//            modifier = Modifier.fillMaxSize(),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center
//        ) {
//            Icon(
//                imageVector = Icons.Default.Menu,
//                contentDescription = "Canvas Height Slider",
//                modifier = Modifier.size(resizeThreshold.dp - 5.dp),
//                tint = Color.DarkGray
//            )
//        }
//    }
//}

// Check if a point is near any path for erasing purposes
//private fun isPointNearPath(point: Offset, path: Path, threshold: Float = 20f): Boolean {
//    val pathBounds = path.getBounds()
//    return (point.x in (pathBounds.left - threshold)..(pathBounds.right + threshold) &&
//            point.y in (pathBounds.top - threshold)..(pathBounds.bottom + threshold))
//}

data class PathData(val points: List<Offset>, val color: Color, val strokeWidth: Float)
@Composable
fun EditableCanvas(block: ContentBlock, onCanvasUpdate: (MutableList<Byte>, Int) -> Unit) {
//    val drawingRepository = remember { DrawingRepository() }
//    val scope = rememberCoroutineScope()

    var canvasHeight by remember { mutableStateOf(100) }

    var selectedColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableStateOf(5f) }
    var paths by remember { mutableStateOf(mutableListOf<PathData>()) }
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }

    var isDrawing by remember { mutableStateOf(true) }

//    var drawings by remember { mutableStateOf<List<Drawing>>(emptyList()) }

    // Load all drawings on initial composition
//    LaunchedEffect(Unit) {
//        scope.launch(Dispatchers.IO) {
//            drawings = drawingRepository.getAllDrawings()
//        }
//    }

    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Top control bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stroke width slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Stroke:")
                    Spacer(Modifier.width(8.dp))
                    Slider(
                        value = strokeWidth,
                        onValueChange = { strokeWidth = it },
                        valueRange = 1f..20f,
                        modifier = Modifier.width(150.dp)
                    )
                }

                val controller = rememberColorPickerController()
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                ) {
                    HsvColorPicker(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(450.dp)
                            .padding(10.dp),
                        controller = controller,
                        initialColor = Color.Black,
                        onColorChanged = { colorEnvelope: ColorEnvelope ->
                            // do something
                            selectedColor = colorEnvelope.color
                            println("color changed to $selectedColor!!")
                        }

                    )
                }
            }

            // Drawing canvas
            Box(modifier = Modifier.fillMaxWidth().height(canvasHeight.dp)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .pointerInput(isDrawing, selectedColor, strokeWidth) {
                            if (isDrawing) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentPath = listOf(offset)
                                    },
                                    onDrag = { change, _ ->
                                        currentPath = currentPath + change.position
                                    },
                                    onDragEnd = {
                                        paths = paths.toMutableList().apply {
                                            add(PathData(currentPath, selectedColor, strokeWidth))
                                        }
                                        currentPath = emptyList()
                                    }
                                )
                            }
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

// Convert canvas paths to bytes for storage
fun canvasToBytes(paths: List<PathData>, width: Int, height: Int): ByteArray {
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
fun bytesToPaths(bytes: ByteArray, width: Int, height: Int): MutableList<PathData> {
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

            paths.add(PathData(points, color, strokeWidth))
        }
    } catch (e: Exception) {
        println("Error reading path data: ${e.message}")
        // Return empty paths on error
    }

    return paths
}

// Extension function to render bitmap from Drawing
//@Composable
//fun DrawingPreview(drawing: Drawing, modifier: Modifier = Modifier) {
//    Canvas(modifier = modifier) {
//        val paths = bytesToPaths(drawing.imageData, drawing.width, drawing.height)
//
//        // Draw all paths
//        paths.forEach { path ->
//            for (i in 0 until path.points.size - 1) {
//                drawLine(
//                    color = path.color,
//                    start = path.points[i],
//                    end = path.points[i + 1],
//                    strokeWidth = path.strokeWidth
//                )
//            }
//        }
//    }
//}

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
            .padding(16.dp)
            .clickable { focusRequester.requestFocus() } // Ensure click brings focus
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
            cursorBrush = SolidColor(if (block.blockType == BlockType.CODE) Color.White else Color.Black)
        )
    }
}

@Composable
fun BlockFrameMenu(index: Int, buttonFuncs: Map<String, (Int) -> Unit>) {
    // BlockFrameMenu consists of the buttons that do actions for all blocks (i.e. all types of ContentBlocks)

    var hoveredButton by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box (
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Row(
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                @Composable
                fun MenuButton(onClick: ((Int) -> Unit)?, icon: ImageVector, desc: String) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isHovered by interactionSource.collectIsHoveredAsState()

                    // toggle hoveredButton state given the button we are hovering over
                    if (isHovered) {
                        hoveredButton = desc
                    } else if (hoveredButton == desc) {
                        hoveredButton = null
                    }

                    IconButton(
                        onClick = { onClick?.invoke(index) },
                        colors = iconButtonColours(),
                        modifier = Modifier.hoverable(interactionSource = interactionSource)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = desc,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                MenuButton(
                    buttonFuncs["Duplicate Block"],
                    Icons.Default.AddCircle,
                    "Duplicate"
                ) // duplicate current block
                MenuButton(
                    buttonFuncs["Move Block Up"],
                    Icons.Default.KeyboardArrowUp,
                    "Move Up"
                ) // move current block up
                MenuButton(
                    buttonFuncs["Move Block Down"],
                    Icons.Default.KeyboardArrowDown,
                    "Move Down"
                ) // move current block down
                MenuButton(buttonFuncs["Delete Block"], Icons.Default.Delete, "Delete") // delete current block
            }
            if (hoveredButton != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(Colors.darkGrey.copy(alpha = 0.9f))
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = hoveredButton.toString(),
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
            modifier = Modifier.hoverable(interactionSource = interactionSource)
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
                    articleModel.addBlock(atAddIndex, type, article, board)
                    selectAtIndex(atAddIndex)
                }
            ) { Text(type.name) }
        }
    }
}
