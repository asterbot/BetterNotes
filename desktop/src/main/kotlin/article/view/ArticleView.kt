package article.view

import LatexRenderer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
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
import shared.*
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
                        onCanvasUpdate = {
                            articleModel.saveBlock(blockIndex, pathsContent=it, article=article, board = board)
                        })
                }

                if (isSelected) {
                    AddBlockFrameButton(article, blockIndex, "DOWN", selectAtIndex, board)
                }

            }
        }
    }
}

@Composable
fun EditableCanvas(
    block: ContentBlock,
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
            .height((block as CanvasBlock).height.dp)
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
        TextButton(
            colors = textButtonColours(),
            onClick = { isErasing = !isErasing },
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
