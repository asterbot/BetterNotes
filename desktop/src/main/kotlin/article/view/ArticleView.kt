package article.view

import LatexRenderer
import androidx.compose.foundation.*
import androidx.compose.foundation.content.MediaType.Companion.Image
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import article.entities.*
import boards.entities.Board
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mongodb.Block
import individual_board.entities.Note
import individual_board.view.IndividualBoardScreen
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.bson.types.Code
import shared.Colors
import shared.articleModel
import shared.articleViewModel
import space.kscience.kmath.ast.parseMath
import space.kscience.kmath.ast.rendering.FeaturedMathRendererWithPostProcess
import space.kscience.kmath.ast.rendering.LatexSyntaxRenderer
import space.kscience.kmath.ast.rendering.renderWithStringBuilder
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.path
import java.io.File
import androidx.compose.ui.*
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.ui.geometry.Rect

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Bitmap
import kotlin.math.roundToInt

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
            Text( // title
                text = "Board ${board.name}",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Text( // article name
                text = "Article ${article.title}",
                fontSize = 20.sp
            )

            // row containing any useful functionality (as buttons)
            Row(
                // TODO: buttons for main navigation (e.g. back to course, other articles, ...)
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // insert TextBlock at beginning
                Button(
                    onClick = { articleModel.addBlock(0, BlockType.PLAINTEXT, article, board) },
                ) { Text(text = "Insert TextBlock") }
                Button(
                    onClick = { navigator.push(IndividualBoardScreen(board)) }
                ) { Text("Back to current course") }
                Button(
                    onClick = {
                        println("DEBUG (THE BLOCKS):")
                        println("FROM MODEL: ${articleModel.contentBlockDict}")
                        articleModel.contentBlockDict[article.id]?.let {articleContentBlocks ->
                            for (contentBlock in articleContentBlocks) {
                                if (contentBlock.blockType == BlockType.CANVAS) {
                                    println("\tCANVAS HAS ${(contentBlock as CanvasBlock).paths.size} PATHS")
                                } else {
                                    println("\t$contentBlock")
                                }
                            }
                        }
                        println("FROM VIEWMODEL: ${contentBlocksList.contentBlocksList}")
                        for (contentBlock in contentBlocksList.contentBlocksList) {
                            if (contentBlock.blockType == BlockType.CANVAS) {
                                println("\tCANVAS HAS ${(contentBlock as CanvasBlock).paths.size} PATHS")
                            } else {
                                println("\t$contentBlock")
                            }
                        }
                        debugState = !debugState
                    }
                ) { Text(text = "DEBUG") }
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
    ) {
        Box(
            modifier = Modifier
                .background(Colors.lightTeal)
                .padding(horizontal = 15.dp, vertical = 10.dp)
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
                                if (block.blockType == BlockType.CODE){
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
                        onCanvasUpdate = { updatedPaths, updatedHeight ->
                            articleModel.saveBlock(
                                blockIndex,
                                pathsContent = updatedPaths,
                                heightContent = updatedHeight,
                                article = article,
                                board = board
                            )
                        })
                }

                if (block.blockType == BlockType.MEDIA) {
                    addMedia(isSelected)
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
            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = { launcher.launch() }) {
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
    onCanvasUpdate: (MutableList<Path>, MutableState<Float>) -> Unit
) {
    var startPaths: MutableList<Path> = when (block.blockType) {
        BlockType.CANVAS -> { (block as CanvasBlock).paths }
        else -> mutableListOf()
    }

    val currentHeight = remember(block) {
        if (block.blockType == BlockType.CANVAS) (block as CanvasBlock).canvasHeight
        else mutableStateOf(50f)
    }


    val paths = remember { mutableStateListOf<Path>().apply { addAll(startPaths) } }
    var currentPath by remember { mutableStateOf(Path()) }
    var isDrawing by remember { mutableStateOf(false) }
    var isOutsideBox by remember { mutableStateOf(false) }
    var isErasing by remember { mutableStateOf(false) }

    val resizeHandleSize = 5.dp

    Layout(
        content = {
            // Content 0: The main canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(currentHeight.value.dp)
                    .background(Color.White)
            ) {
                // Drawing tools
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { isErasing = !isErasing },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Colors.medTeal,
                            contentColor = Colors.white
                        ),
                        shape = CircleShape,
                        contentPadding = PaddingValues(10.dp),
                    ) {
                        Text(if (!isErasing) "Erase" else "Draw")
                    }
                }

                // Drawing canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(currentHeight.value.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    if (offset.x in 0f..size.width.toFloat() && offset.y in 0f..size.height.toFloat()) {
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
                                    val position = change.position
                                    val isInside = position.x in 0f..size.width.toFloat() && change.position.y in 0f..size.height.toFloat()
                                    if (isInside) {
                                        if (isErasing) {
                                            // Remove paths near the cursor position
                                            paths.removeAll { path -> isPointNearPath(position, path) }
                                        } else {
                                            if (isOutsideBox) {
                                                currentPath = Path().apply { moveTo(position.x, position.y) }
                                                isOutsideBox = false
                                            } else {
                                                currentPath = Path().apply {
                                                    addPath(currentPath)
                                                    lineTo(position.x, position.y)
                                                }
                                            }
                                        }
                                    } else {
                                        if (!isOutsideBox && !isErasing) {
                                            paths.add(currentPath)
                                            onCanvasUpdate(paths, currentHeight)
                                            currentPath = Path()
                                            isOutsideBox = true
                                        }
                                    }

                                },
                                onDragEnd = {
                                    if (!isOutsideBox && !isErasing) {
                                        paths.add(currentPath)
                                        onCanvasUpdate(paths, currentHeight)
                                    }
                                    isDrawing = false
                                    currentPath = Path()
                                }
                            )
                        }
                ) {
                    // Draw existing paths
                    paths.forEach { path ->

                        drawPath(
                            path = path,
                            color = Color.Black,
                            style = Stroke(width = 2f)
                        )
                    }

                    // Draw current path being created
                    if (isDrawing && !isErasing) {
                        drawPath(
                            path = currentPath,
                            color = Color.Black,
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }

            // Content 1: The resize handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(resizeHandleSize)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            // Update height with drag, ensuring at least 50dp
                            currentHeight.value = (currentHeight.value + dragAmount.y).coerceAtLeast(50f)
                        }
                    }
            )
        }
    ) { measurables, constraints ->
        val canvasMeasurable = measurables[0]
        val handleMeasurable = measurables[1]

        val canvasHeight = currentHeight.value.dp.roundToPx()
        val handleHeight = resizeHandleSize.toPx().toInt()

        val canvasConstraints = constraints.copy(
            minHeight = 0,
            maxHeight = Constraints.Infinity
        )

        val canvasPlaceable = canvasMeasurable.measure(canvasConstraints)
        val handlePlaceable = handleMeasurable.measure(
            constraints.copy(minHeight = handleHeight, maxHeight = handleHeight)
        )

        // Calculate total height
        val totalHeight = canvasHeight + handleHeight

        // Create the layout
        layout(constraints.maxWidth, totalHeight) {
            canvasPlaceable.placeRelative(0, 0)
            handlePlaceable.placeRelative(0, canvasHeight)
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
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            @Composable
            fun MenuButton(onClick: ((Int) -> Unit)?, desc: String) =
                Button(
                    onClick = {onClick?.invoke(index)},
                    modifier = Modifier
                        .height(30.dp)
                        .widthIn(max=50.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Colors.medTeal,
                        contentColor = Colors.white
                    )
                ) {Text(text=desc)}

            MenuButton(buttonFuncs["Duplicate Block"], "D") // duplicate current block
            MenuButton(buttonFuncs["Move Block Up"], "MU") // move current block up
            MenuButton(buttonFuncs["Move Block Down"], "MD") // move current block down
            MenuButton(buttonFuncs["Delete Block"], "G") // delete current block
        }
    }
}



@Composable
fun AddBlockFrameButton(article: Note, index: Int, direction: String, selectAtIndex: (Int) -> Unit, board: Board) {
    // these buttons add a new (empty) ContentBlock above/below (depends on direction) the currently selected block
    // by default, insertBlock() creates a new Text block (assume that people use this the most)
    var showBlockTypes by remember { mutableStateOf(false) }

    Button(
        onClick = { showBlockTypes = !showBlockTypes },
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Colors.medTeal,
            contentColor = Colors.white
        ),
        shape = CircleShape,
        contentPadding = PaddingValues(10.dp),
    ) { Text(text = "+", fontSize = 20.sp) }

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
            Button(
                onClick = {
                    articleModel.addBlock(atAddIndex, type, article, board)
                    selectAtIndex(atAddIndex)
                }
            ) {
                Text(type.name)
            }
        }
    }
}
