package article.view
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import boards.entities.Board
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import individual_board.entities.ContentBlock
import individual_board.entities.TextBlock
import individual_board.view.IndividualBoardScreen
import shared.Colors


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
    startText: String = "",
    onTextChange: (String) -> Unit,
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(startText)) } // Holds user input and cursor position
    val focusRequester = remember { FocusRequester() } // Controls focus
    var keyPressed by remember { mutableStateOf<Key?>(null) } // Tracks key presses

    // Request focus on first composition
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Ensures that when the user types, the cursor is always at the end of the text
    LaunchedEffect(textFieldValue.text) {
        textFieldValue = textFieldValue.copy(selection = TextRange(textFieldValue.text.length))
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .clickable { focusRequester.requestFocus() } // Ensure click brings focus
    ) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newTextFieldValue ->
                textFieldValue = newTextFieldValue
                onTextChange(newTextFieldValue.text) // Update state in parent composable
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester) // Attach focus requester to manage focus
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




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockTypeSelector() {
    val blockTypes = listOf("None", "Plaintext")
    var expanded by remember { mutableStateOf(false) }
    var selectedBlockType by remember { mutableStateOf(blockTypes[0]) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            value = selectedBlockType,
            onValueChange = {},
            readOnly = true, // Prevents manual text input
            label = { Text(text="Select a Block Type", fontSize=10.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled=true)
                .fillMaxWidth(0.4f)
                .height(52.dp)
                .padding(horizontal=50.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            blockTypes.forEach { blockType ->
                DropdownMenuItem(
                    text = { Text(text=blockType, fontSize=14.sp) },
                    onClick = {
                        selectedBlockType = blockType
                        expanded = false // Close menu after selection
                    }
                )
            }
        }
    }
}




@Composable
fun Article(board: Board) {
    // the content that is assigned to an ArticleScreen (essentially the page view)
    var blocks by remember { mutableStateOf<List<ContentBlock>>(emptyList())}
    val navigator = LocalNavigator.currentOrThrow
    var selectedBlock by remember { mutableStateOf<Int?>(null) }

    // functions for the various buttons that appear for the block
    // these functionalities are available for all types of content blocks
    fun insertBlock(index: Int) {
        println("DEBUG: inserting empty block at index $index (attempt)")
        val updatedBlocks = blocks.toMutableList()
        println("INDEX FROM FUNCTION: $index")
        if (index in 0..(updatedBlocks.size)) {
            println("INDEX: $index")
            updatedBlocks.add(index, TextBlock(""))
            println("DEBUG: inserted block at index $index")
        }
        blocks = updatedBlocks
    }
    fun duplicateBlock(index: Int) {
        println("DEBUG: duplicating block at index $index (attempt)")
        val updatedBlocks = blocks.toMutableList()
        if (index in 0..(updatedBlocks.size-1)) {
            val duplicatedBlock = updatedBlocks[index].copyBlock()
             updatedBlocks.add(index+1, duplicatedBlock)
            println("DEBUG: duplicated block at index $index")
        }
        blocks = updatedBlocks
    }
    fun moveBlockUp(index: Int) {
        println("DEBUG: moving up block at index ${index} (attempt)")
        val updatedBlocks = blocks.toMutableList()
        if (index in 1..(updatedBlocks.size)-1) {
            updatedBlocks[index] = updatedBlocks[index-1].also { updatedBlocks[index-1] = updatedBlocks[index] }
            selectedBlock = index-1
            println("DEBUG: swapped blocks with indices ${index} and ${index-1}")
        }
        blocks = updatedBlocks

    }
    fun moveBlockDown(index: Int) {
        println("DEBUG: moving down block at index ${index} (attempt)")
        val updatedBlocks = blocks.toMutableList()
        if (index in 0..(updatedBlocks.size)-2) {
            updatedBlocks[index] = updatedBlocks[index+1].also { updatedBlocks[index+1] = updatedBlocks[index] }
            selectedBlock = index+1
            println("DEBUG: swapped blocks with indices ${index} and ${index+1}")
        }
        blocks = updatedBlocks
    }
    fun deleteBlock(index: Int) {
        println("DEBUG: deleting block at index ${index} (attempt)")
        val updatedBlocks = blocks.toMutableList()
        if (index in 0..<(updatedBlocks.size)) {
            updatedBlocks.removeAt(index)
            println("DEBUG: deleted block at index ${index}")
        }
        blocks = updatedBlocks
    }
    val menuButtonFuncs: Map<String, (Int) -> Unit> = mapOf(
        "Duplicate Block" to ::duplicateBlock,
        "Move Block Up" to ::moveBlockUp,
        "Move Block Down" to ::moveBlockDown,
        "Delete Block" to ::deleteBlock
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text( // title
            text = "Notes for ${board.name}",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Text( // title
            text = board.desc,
            fontSize = 20.sp
        )

        BlockTypeSelector()

        Row( // TODO: buttons for main navigation (e.g. back to course, other articles, ...)
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) { // row containing any useful functionality (as buttons)
            // insert block at beginning
            Button(
                onClick = {insertBlock(0)}
            ) { Text(text="Insert Block") }
            Button(
                onClick = {navigator.push(IndividualBoardScreen(board))}
            ) { Text("Back to current course") }
            Button(
                onClick = { println("DEBUG: $blocks") }
            ) {Text(text="DEBUG")}
        }

        LazyColumn( // lazy column stores all blocks
            modifier = Modifier.fillMaxSize().padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            println("--------------START--------------")
            itemsIndexed(blocks, key = { index: Int, block: ContentBlock -> block.id })
            {index: Int, block: ContentBlock ->
                val content = when (block) {
                    is TextBlock -> block.text
                    else -> ""
                }

                println("Rendering Block $index: $content")

                BlockFrame(
                    index, block, content,
                    insertBlock = { insertIndex -> insertBlock(insertIndex) },
                    menuButtonFuncs = menuButtonFuncs,
                    isSelected = (selectedBlock == index),
                    onBlockClick = {
                        // if currently selected, deselect (else, select as normal)
                        selectedBlock = if (selectedBlock == index) null else index
                        println("DEBUG: Selecting block at index $index")
                    },
                    updateBlockText = { blockIndex, newText ->
                        val blocksCopy = blocks.toMutableList()
                        val b = blocksCopy[blockIndex]
                        if (b is TextBlock) {
                            blocksCopy[blockIndex] = b.copy(text = newText)
                        }
                        blocks = blocksCopy
                    }
                )
            }
        }
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
fun BlockFrame(blockIndex: Int, block: ContentBlock, content: String,
               insertBlock: (Int) -> Unit,
               menuButtonFuncs: Map<String, (Int) -> Unit>,
               isSelected: Boolean,
               onBlockClick: () -> Unit,
               updateBlockText: (Int, String) -> Unit) {
    // creates a template for ContentBlocks to go into
    var text by remember { mutableStateOf(content) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isSelected) Color(0xFFC0C0C0) else Color.Transparent)
            .clickable {
                onBlockClick()
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
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
                    AddBlockFrameButton(blockIndex, "UP", insertBlock = insertBlock)
                }

//                // TODO: replace this with the actual content blocks
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .background(Colors.medTeal)
//                        .padding(horizontal = 50.dp)
//                        .defaultMinSize(minHeight = 50.dp)
//                )

                EditableTextBox (
                    startText = content,
                    onTextChange = {
                        text = it
                        updateBlockText(blockIndex, text)
                })

                if (isSelected) {
                    AddBlockFrameButton(blockIndex, "DOWN", insertBlock = insertBlock)
                }

            }
        }
    }
}

@Composable
fun AddBlockFrameButton(index: Int, direction: String, insertBlock: (Int) -> Unit) {
    // these buttons add a new (empty) ContentBlock above/below (depends on direction) the currently selected block
    // by default, insertBlock() creates a new Text block (assume that people use this the most)
    Button(
        onClick = {
            val atAddIndex = when (direction) {
                "UP" -> index
                else -> index + 1 // the "DOWN" case
            }
            println("DEBUG: CLICKED for index ${index}, direction ${direction}")
            println("INDEX FROM BUTTON: $atAddIndex")
            insertBlock(atAddIndex)
        },
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Colors.medTeal,
            contentColor = Colors.white),
        shape = CircleShape,
        contentPadding = PaddingValues(10.dp),
    ) { Text(text = "+", fontSize = 20.sp) }
}

//
//@Composable
//fun Article(board: Board){
//    var isDrawingCanvasOpen by remember { mutableStateOf(false) }
//    var markdownRendered by remember { mutableStateOf(false) }
//    val navigator = LocalNavigator.currentOrThrow
//    var text by remember { mutableStateOf("") }
//    val markdownHandler = MarkdownHandler(text)
//
//    Column(
//        modifier = Modifier.fillMaxSize(),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text("Articles", style = MaterialTheme.typography.h2)
//        Row(
//            //modifier = Modifier.fillMaxSize(),
//            verticalAlignment = Alignment.CenterVertically,
//        ) {
//            CanvasButton(
//                onToggleCanvas = { isDrawingCanvasOpen = !isDrawingCanvasOpen },
//                isCanvasOpen = isDrawingCanvasOpen
//            )
//
//            Button(
//                onClick = {
//                    navigator.push(IndividualBoardScreen(board))
//                })
//            {
//                Text("Back to current course")
//            }
//
//            MarkdownButton(
//                onToggleRender = { markdownRendered = !markdownRendered },
//            )
//        }
//
//        // Show drawing canvas if it's open
//        if (isDrawingCanvasOpen) {
//            DrawingCanvas()
//        }
//
//
//        EditableTextBox(onTextChange = {text = it })
//        if (markdownRendered) {
//            markdownHandler.renderMarkdown()
//        }
//
//
//    }
//}
