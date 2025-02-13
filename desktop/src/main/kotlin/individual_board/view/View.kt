package individual_board.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import boards.entities.Board
import boards.view.BoardViewScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import individual_board.entities.Note
import individual_board.entities.Section
import individual_board.entities.Article
import individual_board.entities.MarkdownBlock
import individual_board.entities.CodeBlock
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import individual_board.entities.ContentBlock
import globals.boardViewModel
import globals.boardModel
import globals.individualBoardModel
import globals.individualBoardViewModel
import androidx.compose.foundation.lazy.items

data class IndividualBoardScreen(
    val board: Board
): Screen{
    @Composable
    override fun Content() {
        IndividualBoardView(board)
    }
}

@Composable
fun NoteRowView(
    note: Note,
) {
    Button (
        modifier = Modifier.padding(15.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (note is Article) Color(0xffB1CCD3) else Color(0xffD3B1CC)
        ),
        onClick = {
            println("DEBUG: Clicked ${note.title}")
            // Implement navigator soon
        }
    ) {
        Column {
            Text("${note.title} \n", textAlign = TextAlign.Center)

            note.desc?.let { Text(it, textAlign = TextAlign.Center) }

//            if (note is Article) {
//                for (contentBlock in note.contentBlocks) {
//                    when (contentBlock) {
//                        is MarkdownBlock -> {
//                            Text(contentBlock.text)
//                        }
//                        is CodeBlock -> {
//                            // Implement here
//                        }
//                    }
//                }
//            }
        }
    }
}

@Composable
fun IndividualBoardView(
    board: Board,
) {
    val navigator = LocalNavigator.currentOrThrow

    var noteList by remember {
        mutableStateOf(individualBoardModel.noteDict[board.id]?.toList() ?: emptyList())
    }

    var showDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    // Flag: true = adding Article, false = adding Section
    var addingArticle by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${board.name}",
            style = MaterialTheme.typography.h2,
            modifier = Modifier.padding(16.dp)
        )

        if (noteList.isEmpty()) {
            Text(
                text = "No notes available",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(10.dp)
            ) {
                items(noteList) { note ->
                    NoteRowView(note)
                }
            }
        }

        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            Button(
                onClick = {
                    addingArticle = false
                    showDialog = true
                }
            ) {
                Text("Add Section")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    addingArticle = true
                    showDialog = true
                }
            ) {
                Text("Add Article")
            }
        }

        Button(
            onClick = {
                navigator.push(BoardViewScreen())
            },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("Back to All Boards")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                newTitle = ""
                newDesc = ""
            },
            title = { Text(text = if (addingArticle) "Add Article" else "Add Section") },
            text = {
                Column {
                    TextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (addingArticle) {
                            val newArticle = Article(
                                id = 0, // TODO: Placeholder
                                title = newTitle,
                                desc = newDesc,
                                contentBlocks = mutableListOf()
                            )
                            individualBoardModel.addArticle(newArticle, board.id)
                        } else {
                            val newSection = Section(
                                id = 0, // TODO: Placeholder
                                title = newTitle,
                                desc = newDesc
                            )
                            individualBoardModel.addSection(newSection, board.id)
                        }
                        noteList = individualBoardModel.noteDict[board.id]?.toList() ?: emptyList()
                        showDialog = false
                        newTitle = ""
                        newDesc = ""
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDialog = false
                        newTitle = ""
                        newDesc = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

