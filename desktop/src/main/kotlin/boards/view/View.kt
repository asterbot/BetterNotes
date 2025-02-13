package boards.view

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyUp
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import boards.entities.Board
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import individual_board.view.IndividualBoardScreen
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser


data class BoardViewScreen(val boardView: ViewModel): Screen{
    @Composable
    override fun Content() {
        BoardsView(boardView)
    }
}

@Composable
fun CanvasButton(
    onToggleCanvas: () -> Unit,
    isCanvasOpen: Boolean,
    // isEraserOn: Boolean
) {
    Button(
        modifier = Modifier.padding(15.dp),
        colors = ButtonDefaults.buttonColors(Color(0xff74C365)),
        onClick = {
            println("Toggling canvas")
            onToggleCanvas()
        }
    ) {
        Text(if (isCanvasOpen) "Close Canvas" else "New Canvas", textAlign = TextAlign.Center)
    }
}

@Composable
fun DrawingCanvas() {
    val paths = remember { mutableStateListOf<Path>() }
    var currentPath by remember { mutableStateOf(Path()) }
    var isDrawing by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color.White)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDrawing = true
                        currentPath = Path().apply { moveTo(offset.x, offset.y) }
                    },
                    onDrag = { change, _ ->
                        currentPath = Path().apply {
                            addPath(currentPath)
                            lineTo(change.position.x, change.position.y)
                        }
                    },
                    onDragEnd = {
                        isDrawing = false
                        paths.add(currentPath)
                        currentPath = Path()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            paths.forEach { path ->
                drawPath(
                    path = path,
                    color = Color.Black,
                    style = Stroke(width = 2f)
                )
            }
            if (isDrawing) {
                drawPath(
                    path = currentPath,
                    color = Color.Black,
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}

@Composable
fun MarkdownButton(
    onToggleRender: () -> Unit,
) {
    Button(
        modifier = Modifier.padding(15.dp),
        colors = ButtonDefaults.buttonColors(Color(0xffB1CCD3)),
        onClick = {
            onToggleRender()
        }
    ) { Text("Toggle Markdown") }
}

@Composable
fun EditableTextBox(
    onTextChange: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") } // Holds user input
    val focusRequester = remember { FocusRequester() } // Controls focus
    var keyPressed by remember { mutableStateOf<Key?>(null) }


    // Effect to continuously add characters when a key is held down
//    LaunchedEffect(keyPressed) {
//        keyPressed?.let { key ->
//            while (keyPressed == key) {
//                // text += key.toString()[5] // Append the pressed key (you can customize this)
//                delay(100L) // Adjust delay for input repeat speed
//            }
//        }
//    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .clickable { focusRequester.requestFocus() } // Ensure click brings focus (highlights when hovering)
    ) {
        BasicTextField(
            value = text,
            onValueChange = {
                text = it
                onTextChange(text) }, // Updates state
            modifier = Modifier
                    .fillMaxWidth()
                    // .focusRequester(focusRequester) // Attach focus requester
                    // .focusable(true) // Allow focus
                    .onKeyEvent { event -> // Handle key events
                        when {
                            event.type == KeyDown -> {
                                keyPressed = event.key // Start tracking key hold
                                true
                            }
                            event.type == KeyUp -> {
                                keyPressed = null // Stop key repeat
                                true
                            }
                            else -> false
                        }
                    }
        )
    }
}

@Composable
fun MarkdownRenderer(rawText: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // first, parse the raw text into an Abstract Syntax Tree (AST)
        val flavour = CommonMarkFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(rawText)

        parsedTree.children.forEach{node ->
            println("-----------------------------")
            printASTNode(node, rawText)
            renderMarkdownNode(node, rawText)
        }
    }
}

fun printASTNode(node: ASTNode, rawText: String) {
    print(node.type.toString() + " (text: [" + extractText(node, rawText) + "])\n")

    node.children.forEach {
        printASTNode(it, rawText)
    }
}

@Composable
fun renderMarkdownNode(node: ASTNode, rawText: String) {
    val headerTypes = arrayOf(MarkdownElementTypes.ATX_1, MarkdownElementTypes.ATX_2, MarkdownElementTypes.ATX_3)
    var t = extractText(node, rawText)
    when (node.type) {
        in headerTypes -> {
            t = t.trimStart('#', ' ')
            val fontSize = when (node.type) {
                MarkdownElementTypes.ATX_1 -> 30.sp
                MarkdownElementTypes.ATX_2 -> 24.sp
                MarkdownElementTypes.ATX_3 -> 18.sp
                else -> 0.sp // should never reach here
            }
            Text(
                text = t,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
        }
        MarkdownTokenTypes.EOL -> {}
        else -> Text(
            text = extractText(node, rawText)
        )
    }
}

fun extractText(node: ASTNode, rawText: String): String {
    return rawText.substring(node.startOffset, node.endOffset)
}


@Composable
fun BoardButton(board: Board, onLeftClickBoard: (Int) -> Unit) {
    Button(
        modifier = Modifier.padding(15.dp),
        colors = ButtonDefaults.buttonColors(Color(0xffB1CCD3)),
        onClick = {
            println("DEBUG: Clicked ${board.name}")
            onLeftClickBoard(board.id)
        }
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("${board.name} \n", textAlign = TextAlign.Center)
            Text(board.desc, textAlign = TextAlign.Center)
        }
    }
}


@Composable
fun BoardsView(
    boardViewModel: ViewModel
) {

    val boardList by remember { mutableStateOf(boardViewModel.boardList.toList()) }
    val navigator = LocalNavigator.currentOrThrow

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Boards", style = MaterialTheme.typography.h2)

        Box(
            Modifier.fillMaxSize()
                .padding(15.dp)
                .background(Color(0xFFF0EDEE))
        ) {
            val state = rememberLazyGridState()
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(10.dp),
                columns = GridCells.Fixed(3),
                state = state
            ) {
                for (board in boardList) {
                    item {
                        BoardButton(board = board, onLeftClickBoard = {
                            navigator.push(IndividualBoardScreen(board.name, boardViewModel))
                        })
                    }
                }
            }
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState = state)
            )
        }
    }
}