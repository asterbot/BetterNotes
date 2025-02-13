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


// Globals
import globals.boardViewModel
import globals.boardModel
import globals.individualBoardModel
import globals.individualBoardViewModel

class BoardViewScreen: Screen{
    @Composable
    override fun Content() {
        BoardsView()
    }
}

@Composable
fun BoardButton(
    board: Board,
    onBoardDeleted: () -> Unit
) {
    val navigator = LocalNavigator.currentOrThrow
    Button(
        modifier = Modifier.padding(15.dp),
        colors = ButtonDefaults.buttonColors(if (!board.canDelete) Color(0xffB1CCD3) else Color(0xffffd7d4)),
        onClick = {
            if (board.canDelete) {
                println("DEBUG: Deleting ${board.name}")
                onBoardDeleted()
            }
            else{
                println("DEBUG: Clicked ${board.name}")
                navigator.push(IndividualBoardScreen(board))
            }

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
) {

    var boardList by remember { mutableStateOf(boardViewModel.boardList.toList()) }

    // possible values: "addBoard" (doing it this way so we can have more alerts if needed)
    var currAlert by remember { mutableStateOf("") }

    // Received by form
    var newCourse by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Boards", style = MaterialTheme.typography.h2)

        Box(
            Modifier.fillMaxSize()
                .padding(15.dp)
                .background(Color(0xFFF0EDEE))
                .weight(1f)
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
                        BoardButton(board = board, onBoardDeleted = {
                            board.canDelete = false // need to do this so it is found
                            boardModel.del(board)
                            boardList = boardViewModel.boardList.toList()
                            boardList = boardList.map{ it.copy(canDelete = false) }
                            individualBoardModel.removeBoard(board.id)

                        })
                    }
                }
            }
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState = state)
            )
        }

        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            Button(
                onClick = {
                    currAlert="addBoard"
                }
            ) {
                Text("Add Board")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    boardList = boardList.map{ it.copy(canDelete = true) }
                }
            ) {
                Text("Remove Board")
            }
        }
    }
    if (currAlert!=""){
        AlertDialog(
            onDismissRequest = {
                currAlert=""

                newCourse = ""
                newDescription = ""
            },
            title = { Text(text =
                when(currAlert){
                    "addBoard" -> "Add Board"
                    "removeBoard" -> "Remove Board"
                    else -> "Invalid op... how did you get here?"
                })
            },
            text = {
                Column {
                    TextField(
                        value = newCourse,
                        onValueChange = { newCourse = it },
                        label = { Text("Course Code") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = newDescription,
                        onValueChange = { newDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (currAlert){
                            "addBoard" -> {
                                boardModel.add(Board(boardModel.newBoardId(), newCourse, newDescription))
                                individualBoardModel.addBlankBoard(boardModel.newBoardId())
                            }
                        }
                        boardList = boardModel.boardList.toList()
                        currAlert = ""
                        newCourse = ""
                        newDescription = ""
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        currAlert = ""
                        newCourse = ""
                        newDescription = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

}
