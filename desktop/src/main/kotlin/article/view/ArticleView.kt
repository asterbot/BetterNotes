package article.view

import LatexRenderer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import article.entities.*
import boards.entities.Board
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mongodb.Block
import individual_board.entities.Note
import individual_board.view.IndividualBoardScreen
import org.bson.types.Code
import shared.Colors
import shared.articleModel
import shared.articleViewModel
import space.kscience.kmath.ast.parseMath
import space.kscience.kmath.ast.rendering.FeaturedMathRendererWithPostProcess
import space.kscience.kmath.ast.rendering.LatexSyntaxRenderer
import space.kscience.kmath.ast.rendering.renderWithStringBuilder

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
            articleModel.duplicateBlock(index, article)
            // articleModel.duplicateBlock(index, article)
            selectedBlock = index + 1
        },
        "Move Block Up" to { index ->
            articleModel.moveBlockUp(index, article)
            selectedBlock = index - 1
        },
        "Move Block Down" to { index ->
            articleModel.moveBlockDown(index, article)
            selectedBlock = index + 1
        },
        "Delete Block" to { index ->
            articleModel.deleteBlock(index, article)
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
                    onClick = { articleModel.addBlock(0, BlockType.PLAINTEXT, article) },
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
                        debugState = debugState
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
    debugState: Boolean
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
                    AddBlockFrameButton(article, blockIndex, "UP", selectAtIndex)
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
                                        language = (block as CodeBlock).language)
                                }
                                else {
                                    articleModel.saveBlock(blockIndex, stringContent = it, article = article)
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
                        100.dp,
                        onCanvasUpdate = {
                            articleModel.saveBlock(blockIndex, pathsContent=it, article=article)
                        })
                }

                if (isSelected) {
                    AddBlockFrameButton(article, blockIndex, "DOWN", selectAtIndex)
                }

            }
        }
    }
}

@Composable
fun EditableCanvas(
    block: ContentBlock,
    canvasHeight: Dp,
    onCanvasUpdate: (MutableList<Path>) -> Unit
) {
    var startPaths: MutableList<Path> = when (block.blockType) {
        BlockType.CANVAS -> { (block as CanvasBlock).paths }
        else -> mutableListOf()
    }

    val paths = remember { mutableStateListOf<Path>().apply { addAll(startPaths) } }
    var currentPath by remember { mutableStateOf(Path()) }
    var isDrawing by remember { mutableStateOf(false) }
    var isOutsideBox by remember { mutableStateOf(false) }
    var isErasing by remember { mutableStateOf(false) } // Eraser mode toggle


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(canvasHeight)
            .background(Color.White)
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
                                onCanvasUpdate(paths)
                                currentPath = Path()
                                isOutsideBox = true
                            }
                        }
                    },
                    onDragEnd = {
                        if (!isOutsideBox && !isErasing) {
                            paths.add(currentPath)
                            onCanvasUpdate(paths)
                        }
                        isDrawing = false
                        currentPath = Path()
                    }
                )
            }

    ) {
        Button(
            onClick = { isErasing = !isErasing },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Colors.medTeal,
                contentColor = Colors.white
            ),
            shape = CircleShape,
            contentPadding = PaddingValues(10.dp),
        ) { Text(if (!isErasing) "Erase" else "Draw" ) }

        // drawing the existing path
        Canvas(modifier = Modifier.fillMaxSize()) {
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

    }
}

fun isPointNearPath(point: Offset, path: Path, threshold: Float = 20f): Boolean {
    val pathBounds = path.getBounds()
    return (point.x in (pathBounds.left - threshold)..(pathBounds.right + threshold) &&
            point.y in (pathBounds.top - threshold)..(pathBounds.bottom + threshold))
}


//fun Path.getBounds(): Rect {
//    return this.getBounds()
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
fun AddBlockFrameButton(article: Note, index: Int, direction: String, selectAtIndex: (Int) -> Unit) {
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

    if (showBlockTypes) { InsertBlockTypesMenu(article, index, direction, selectAtIndex) }
}

@Composable
fun InsertBlockTypesMenu(article: Note, index: Int, direction: String, selectAtIndex: (Int) -> Unit) {
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
                    articleModel.addBlock(atAddIndex, type, article)
                    selectAtIndex(atAddIndex)
                }
            ) {
                Text(type.name)
            }
        }
    }
}

//
//@Composable
//fun CanvasButton(
//    onToggleCanvas: () -> Unit,
//    isCanvasOpen: Boolean,
//    // isEraserOn: Boolean
//) {
//    Button(
//        modifier = Modifier.padding(15.dp),
//        colors = ButtonDefaults.buttonColors(Color(0xff74C365)),
//        onClick = {
//            println("Toggling canvas")
//            onToggleCanvas()
//        }
//    ) {
//        Text(if (isCanvasOpen) "Close Canvas" else "New Canvas", textAlign = TextAlign.Center)
//    }
//}
//
//
//@Composable
//fun DrawingCanvas() {
//    val paths = remember { mutableStateListOf<Path>() }
//    var currentPath by remember { mutableStateOf(Path()) }
//    var isDrawing by remember { mutableStateOf(false) }
//
//    Box(
//        modifier = Modifier.fillMaxSize()
//            .background(Color.White)
//            .pointerInput(Unit) {
//                detectDragGestures(
//                    onDragStart = { offset ->
//                        isDrawing = true
//                        currentPath = Path().apply { moveTo(offset.x, offset.y) }
//                    },
//                    onDrag = { change, _ ->
//                        currentPath = Path().apply {
//                            addPath(currentPath)
//                            lineTo(change.position.x, change.position.y)
//                        }
//                    },
//                    onDragEnd = {
//                        isDrawing = false
//                        paths.add(currentPath)
//                        currentPath = Path()
//                    }
//                )
//            }
//    ) {
//        Canvas(modifier = Modifier.fillMaxSize()) {
//            paths.forEach { path ->
//                drawPath(
//                    path = path,
//                    color = Color.Black,
//                    style = Stroke(width = 2f)
//                )
//            }
//            if (isDrawing) {
//                drawPath(
//                    path = currentPath,
//                    color = Color.Black,
//                    style = Stroke(width = 2f)
//                )
//            }
//        }
//    }
//}
//
//
//@Composable
//fun MarkdownButton(
//    onToggleRender: () -> Unit,
//) {
//    Button(
//        modifier = Modifier.padding(15.dp),
//        colors = ButtonDefaults.buttonColors(Color(0xffB1CCD3)),
//        onClick = {
//            onToggleRender()
//        }
//    ) { Text("Toggle Markdown") }
//}
//
//
//@Composable
//fun Article(board: Board, article: Article) { // NOTE: pass in "Article" to this as well?
//    // the content that is assigned to an ArticleScreen (essentially the page view)
//    var blocks by remember { mutableStateOf<List<ContentBlock>>(listOf(TextBlock(text="SOME DEFAULT VALUE")))}
//    val navigator = LocalNavigator.currentOrThrow
//    var selectedBlock by remember { mutableStateOf<Int?>(null) }
//    var debugState by remember { mutableStateOf(false) }
//
//    // functions for the various buttons that appear for the block
//    // these functionalities are available for all types of content blocks
//    fun insertBlock(index: Int, defaultBlock: ContentBlock) {
//        println("DEBUG: inserting empty block at index $index (attempt)")
//        val updatedBlocks = blocks.toMutableList()
//        if (index in 0..(updatedBlocks.size)) {
//            updatedBlocks.add(index, defaultBlock)
//            println("DEBUG: inserted block at index $index")
//        }
//        blocks = updatedBlocks
//    }
//    fun duplicateBlock(index: Int) {
//        println("DEBUG: duplicating block at index $index (attempt)")
//        val updatedBlocks = blocks.toMutableList()
//        if (index in 0..(updatedBlocks.size-1)) {
//            val duplicatedBlock = updatedBlocks[index].copyBlock()
//             updatedBlocks.add(index+1, duplicatedBlock)
//            println("DEBUG: duplicated block at index $index")
//        }
//        blocks = updatedBlocks
//    }
//    fun moveBlockUp(index: Int) {
//        println("DEBUG: moving up block at index ${index} (attempt)")
//        val updatedBlocks = blocks.toMutableList()
//        if (index in 1..(updatedBlocks.size)-1) {
//            updatedBlocks[index] = updatedBlocks[index-1].also { updatedBlocks[index-1] = updatedBlocks[index] }
//            selectedBlock = index-1
//            println("DEBUG: swapped blocks with indices ${index} and ${index-1}")
//        }
//        blocks = updatedBlocks
//
//    }
//    fun moveBlockDown(index: Int) {
//        println("DEBUG: moving down block at index ${index} (attempt)")
//        val updatedBlocks = blocks.toMutableList()
//        if (index in 0..(updatedBlocks.size)-2) {
//            updatedBlocks[index] = updatedBlocks[index+1].also { updatedBlocks[index+1] = updatedBlocks[index] }
//            selectedBlock = index+1
//            println("DEBUG: swapped blocks with indices ${index} and ${index+1}")
//        }
//        blocks = updatedBlocks
//    }
//    fun deleteBlock(index: Int) {
//        println("DEBUG: deleting block at index ${index} (attempt)")
//        val updatedBlocks = blocks.toMutableList()
//        if (index in 0..<(updatedBlocks.size)) {
//            updatedBlocks.removeAt(index)
//            selectedBlock = null
//            println("DEBUG: deleted block at index ${index}")
//        }
//        blocks = updatedBlocks
//    }
//    val menuButtonFuncs: Map<String, (Int) -> Unit> = mapOf(
//        "Duplicate Block" to ::duplicateBlock,
//        "Move Block Up" to ::moveBlockUp,
//        "Move Block Down" to ::moveBlockDown,
//        "Delete Block" to ::deleteBlock
//    )
//
//    Column(
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.spacedBy(10.dp)
//    ) {
//        Text( // title
//            text = "Notes for ${board.name}",
//            fontSize = 30.sp,
//            fontWeight = FontWeight.Bold
//        )
////        Text( // title
////            text = board.desc,
////            fontSize = 20.sp
////        )
//        Text( // article name
//            text = article.title,
//            fontSize = 20.sp
//        )
//
//        Row( // TODO: buttons for main navigation (e.g. back to course, other articles, ...)
//            horizontalArrangement = Arrangement.spacedBy(10.dp),
//        ) { // row containing any useful functionality (as buttons)
//            // insert block at beginning
//            Button(
//                onClick = {insertBlock(0, TextBlock("")) },
//            ) { Text(text="Insert Block") }
//            Button(
//                onClick = {navigator.push(IndividualBoardScreen(board))}
//            ) { Text("Back to current course") }
//            Button(
//                onClick = {
//                    println("DEBUG: $blocks")
//                    debugState = !debugState
//                }
//            ) {Text(text="DEBUG")}
//        }
//
//        LazyColumn( // lazy column stores all blocks
//            modifier = Modifier.fillMaxSize().padding(25.dp),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.spacedBy(20.dp)
//        ) {
//            println("--------------START--------------")
//            itemsIndexed(blocks, key = { index: Int, block: ContentBlock -> block.id })
//            {index: Int, block: ContentBlock ->
//                val content = when (block) {
//                    is TextBlock -> block.text
//                    else -> ""
//                }
//
//                println("Rendering Block $index: $content")
//
//                BlockFrame(
//                    index, block, content,
//                    insertBlock = { insertIndex, defaultBlock -> insertBlock(insertIndex, defaultBlock) },
//                    menuButtonFuncs = menuButtonFuncs,
//                    isSelected = (selectedBlock == index),
//                    onBlockClick = {
//                        // if currently selected, deselect (else, select as normal)
//                        selectedBlock = if (selectedBlock == index) null else index
//                        println("DEBUG: Selecting block at index $index")
//                    },
//                    updateBlockText = { blockIndex, newText ->
//                        println("INDEX: $blockIndex")
//                        println("THE NEW TEXT: $newText")
//                        val blocksCopy = blocks.toMutableList()
//                        val b = blocksCopy[blockIndex]
//                        if (b is TextBlock) {
//                            blocksCopy[blockIndex] = b.copy(text = newText)
//                        }
//                        blocks = blocksCopy
//                    },
//                    debugState = debugState
//                )
//            }
//        }
//    }
//}
//
//
//@Composable
//fun BlockFrameMenu(index: Int, buttonFuncs: Map<String, (Int) -> Unit>) {
//    // BlockFrameMenu consists of the buttons that do actions for all blocks (i.e. all types of ContentBlocks)
//    Box(
//        modifier = Modifier.fillMaxSize(),
//    ) {
//        Row(
//            modifier = Modifier.align(Alignment.TopEnd),
//            horizontalArrangement = Arrangement.spacedBy(5.dp)
//        ) {
//            @Composable
//            fun MenuButton(onClick: ((Int) -> Unit)?, desc: String) =
//                Button(
//                    onClick = {onClick?.invoke(index)},
//                    modifier = Modifier
//                        .height(30.dp)
//                        .widthIn(max=50.dp),
//                    contentPadding = PaddingValues(0.dp),
//                    colors = ButtonDefaults.buttonColors(
//                        backgroundColor = Colors.medTeal,
//                        contentColor = Colors.white
//                    )
//                ) {Text(text=desc)}
//
//            MenuButton(buttonFuncs["Duplicate Block"], "D") // duplicate current block
//            MenuButton(buttonFuncs["Move Block Up"], "MU") // move current block up
//            MenuButton(buttonFuncs["Move Block Down"], "MD") // move current block down
//            MenuButton(buttonFuncs["Delete Block"], "G") // delete current block
//        }
//    }
//}
//
//
//@Composable
//fun BlockFrame(blockIndex: Int, block: ContentBlock, content: String,
//               insertBlock: (Int, ContentBlock) -> Unit,
//               menuButtonFuncs: Map<String, (Int) -> Unit>,
//               isSelected: Boolean,
//               onBlockClick: () -> Unit,
//               updateBlockText: (Int, String) -> Unit,
//               debugState: Boolean)
//{
//    // creates a template for ContentBlocks to go into
//    var text by remember { mutableStateOf(content) }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(if (isSelected) Color(0xFFC0C0C0) else Color.Transparent)
//            .clickable {
//                onBlockClick()
//            }
//    ) {
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .background(Colors.lightTeal)
//                .padding(horizontal = 15.dp, vertical = 10.dp)
//        ) {
//            if (isSelected) {
//                BlockFrameMenu(blockIndex, menuButtonFuncs)
//            }
//
//            Column(
//                modifier = Modifier.fillMaxSize(),
//                horizontalAlignment = Alignment.CenterHorizontally,
//                verticalArrangement = Arrangement.spacedBy(5.dp)
//            ) {
//
//                if (isSelected) {
//                    AddBlockFrameButton(blockIndex, "UP", insertBlock = insertBlock)
//                }
//
//                // TODO: replace this with the actual content blocks
//                EditableTextBox(
//                    startText = content,
//                    onTextChange = {
//                        updateBlockText(blockIndex, text)
//                    }
//                )
//
//                if (isSelected) {
//                    AddBlockFrameButton(blockIndex, "DOWN", insertBlock = insertBlock)
//                }
//
//            }
//        }
//    }
//}
//
//
//@Composable
//fun EditableTextBox(
//    startText: String = "",
//    onTextChange: (String) -> Unit,
//) {
//    var textFieldValue by remember { mutableStateOf<String>(startText) }
//    val focusRequester = remember { FocusRequester() } // Controls focus
//
//    // Request focus on first composition
//    LaunchedEffect(Unit) {
//        focusRequester.requestFocus()
//    }
//
//    Column(
//        modifier = Modifier
//            .padding(16.dp)
//            .clickable { focusRequester.requestFocus() } // Ensure click brings focus
//    ) {
//        BasicTextField(
//            value = textFieldValue,
//            onValueChange = {
//                textFieldValue = it
//                onTextChange(it)
//            },
//            modifier = Modifier
//                .fillMaxWidth()
//                .focusRequester(focusRequester) // Attach focus requester to manage focus
//        )
//    }
//}
//
//
//@Composable
//fun AddBlockFrameButton(index: Int, direction: String, insertBlock: (Int, ContentBlock) -> Unit) {
//    // these buttons add a new (empty) ContentBlock above/below (depends on direction) the currently selected block
//    // by default, insertBlock() creates a new Text block (assume that people use this the most)
//    var showBlockTypes by remember { mutableStateOf(false) }
//
//    Button(
//        onClick = { showBlockTypes = !showBlockTypes },
//        colors = ButtonDefaults.buttonColors(
//            backgroundColor = Colors.medTeal,
//            contentColor = Colors.white
//        ),
//        shape = CircleShape,
//        contentPadding = PaddingValues(10.dp),
//    ) { Text(text = "+", fontSize = 20.sp) }
//
//    if (showBlockTypes) { InsertBlockTypesMenu(index, direction, insertBlock) }
//}
//
//@Composable
//fun InsertBlockTypesMenu(index: Int, direction: String, insertBlock: (Int, ContentBlock) -> Unit) {
//    val atAddIndex = when (direction) {
//        "UP" -> index
//        else -> index + 1 // the "DOWN" case
//    }
//
//    println("DEBUG: CLICKED for index $index, direction $direction")
//    // insertBlock(atAddIndex)
//
//
//    Row(
//        horizontalArrangement = Arrangement.spacedBy(5.dp),
//    ) {
//        for (type in BlockType.entries) {
//            Button(
//                onClick = {
//                    insertBlock(atAddIndex, type.defaultBlock.copyBlock())
//                }
//            ) {
//                Text(type.name)
//            }
//        }
//    }
//}
//
//
////@Composable
////fun Article(board: Board){
////    var isDrawingCanvasOpen by remember { mutableStateOf(false) }
////    var markdownRendered by remember { mutableStateOf(false) }
////    val navigator = LocalNavigator.currentOrThrow
////    var text by remember { mutableStateOf("") }
////    val markdownHandler = MarkdownHandler(text)
////
////    Column(
////        modifier = Modifier.fillMaxSize(),
////        horizontalAlignment = Alignment.CenterHorizontally
////    ) {
////        Text("Articles", style = MaterialTheme.typography.h2)
////        Row(
////            //modifier = Modifier.fillMaxSize(),
////            verticalAlignment = Alignment.CenterVertically,
////        ) {
////            CanvasButton(
////                onToggleCanvas = { isDrawingCanvasOpen = !isDrawingCanvasOpen },
////                isCanvasOpen = isDrawingCanvasOpen
////            )
////
////            Button(
////                onClick = {
////                    navigator.push(IndividualBoardScreen(board))
////                })
////            {
////                Text("Back to current course")
////            }
////
////            MarkdownButton(
////                onToggleRender = { markdownRendered = !markdownRendered },
////            )
////        }
////
////        // Show drawing canvas if it's open
////        if (isDrawingCanvasOpen) {
////            DrawingCanvas()
////        }
////
////
////        EditableTextBox(onTextChange = {text = it })
////        if (markdownRendered) {
////            markdownHandler.renderMarkdown()
////        }
////
////
////    }
////}
