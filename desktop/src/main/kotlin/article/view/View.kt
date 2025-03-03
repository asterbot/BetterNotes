package article.view
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import boards.entities.Board
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyUp
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import article.entities.MarkdownHandler
import individual_board.view.IndividualBoardScreen

data class ArticleScreen(var board: Board): Screen{
    @Composable
    override fun Content() {
        Article(board)
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


//     Effect to continuously add characters when a key is held down
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
fun Article(board: Board){
    var isDrawingCanvasOpen by remember { mutableStateOf(false) }
    var markdownRendered by remember { mutableStateOf(false) }
    val navigator = LocalNavigator.currentOrThrow
    var text by remember { mutableStateOf("") }
    val markdownHandler = MarkdownHandler(text)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Articles", style = MaterialTheme.typography.h2)
        Row(
            //modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CanvasButton(
                onToggleCanvas = { isDrawingCanvasOpen = !isDrawingCanvasOpen },
                isCanvasOpen = isDrawingCanvasOpen
            )

            Button(
                onClick = {
                    navigator.push(IndividualBoardScreen(board))
                })
            {
                Text("Back to current course")
            }

            MarkdownButton(
                onToggleRender = { markdownRendered = !markdownRendered },
            )
        }

        // Show drawing canvas if it's open
        if (isDrawingCanvasOpen) {
            DrawingCanvas()
        }


        EditableTextBox(onTextChange = {text = it })
        if (markdownRendered) {
            markdownHandler.renderMarkdown()
        }


    }
}
