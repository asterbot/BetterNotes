package individual_board.view
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import article.view.ArticleScreen
import boards.entities.Board
import boards.view.BoardViewScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import article.entities.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

import individual_board.entities.Note
//import individual_board.entities.Section
import individual_board.entities.addNote
import individual_board.view.IndvBoardViewModel
import org.bson.types.ObjectId
import shared.*

data class IndividualBoardScreen(
    val board: Board
): Screen{
    @Composable
    override fun Content() {
        IndividualBoardView(board)
    }
}

@Composable
fun AddNoteOptions(
    onAddSection: () -> Unit,
    onAddArticle: () -> Unit
) {
    Column {
        Button(
            onClick = onAddSection,
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Add Section")
        }
        Button(
            onClick = onAddArticle,
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Add Article")
        }
    }
}

@Composable
fun NoteButton(
    note: Note,
    board: Board,
    onDelete: (Note) -> Unit,
    onEdit: (Note) -> Unit
) {
    val navigator = LocalNavigator.currentOrThrow
    Box (
        modifier = Modifier.padding(15.dp),
    ) {
        Button(
            modifier = Modifier.padding(15.dp)
                .fillMaxWidth(0.5f),
            onClick = {
                println("DEBUG: Clicked ${note.title}")
                if (note.type=="article") {
                    individualBoardModel.updateNoteAccessed(note, board)
                    navigator.push(ArticleScreen(board, note))
                }
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (note.type=="section") Colors.darkTeal else Colors.lightTeal
            )
        ) {
            Column(
                modifier = Modifier.padding(15.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("${note.title} \n", textAlign = TextAlign.Center)
                note.desc?.let { Text(it, textAlign = TextAlign.Center) }
            }
        }
        ActionMenu(
            onEdit = { onEdit(note) },
            onDelete = { onDelete(note) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )
    }
}

@Composable
fun IndividualBoardView(
    board: Board,
) {
    val navigator = LocalNavigator.currentOrThrow

    individualBoardViewModel = IndvBoardViewModel(individualBoardModel, board.id)

    var noteList by remember { mutableStateOf(individualBoardViewModel) }

    val openAddSectionDialog = remember { mutableStateOf(false) }
    val openAddArticleDialog = remember { mutableStateOf(false) }
    val noteToEdit = remember { mutableStateOf<Note?>(null) }
    val noteToDelete = remember { mutableStateOf<Note?>(null) }


    fun addNote(title: String, desc: String, type: String){
        individualBoardModel.addNote(Note(ObjectId(), title, desc, type), board)
    }

    fun deleteNote(note: Note) {
        individualBoardModel.del(note, board)
    }

    fun editNote(note:Note, title: String, desc: String){
        println("DEBUG: Edit ${note.type} called")
        individualBoardModel.updateNote(note, board.id, title, desc)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = board.name,
            style = MaterialTheme.typography.h2,
            modifier = Modifier.padding(16.dp)
        )

        Text(
            text = board.desc,
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(16.dp)
        )

        Button(
            onClick = { navigator.push(BoardViewScreen()) },
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Back to All Boards")
        }

        Box(
            Modifier.fillMaxSize()
                .padding(15.dp)
                .background(Color(0xFFF0EDEE))
                .weight(1f)
        ) {
            val state = rememberLazyListState()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = state,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (noteList.noteList.isEmpty()) item {
                    Text(
                        text = "No notes available",
                        modifier = Modifier
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                else {
                    for (note in noteList.noteList) {
                        item {
                            NoteButton(
                                note = note,
                                board = board,
                                onDelete = { noteToDelete.value = note },
                                onEdit = { noteToEdit.value = note }
                            )
                        }
                    }
                }
            }
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState = state)
            )

            AddNoteMenu(
                onAddSection = { openAddSectionDialog.value = true },
                onAddArticle = { openAddArticleDialog.value = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd) // bottom-right pos
                    .padding(16.dp)
            )
        }

        when {
            openAddArticleDialog.value -> {
                AddNoteDialog(
                    type = "Article",
                    onDismissRequest = { openAddArticleDialog.value = false },
                    onConfirmation = { title, desc ->
//                        addArticle(title, desc)
                        addNote(title, desc, "article")
                        openAddArticleDialog.value = false
                    }
                )
            }

            openAddSectionDialog.value -> {
                AddNoteDialog(
                    type = "Section",
                    onDismissRequest = { openAddSectionDialog.value = false },
                    onConfirmation = { title, desc ->
//                        addSection(title, desc)
                        addNote(title, desc, "section")
                        openAddSectionDialog.value = false
                    }
                )
            }

            noteToEdit.value != null -> {
//                println("DEBUG: THIS IS SECTION: ${noteToEdit.value is Section}")
                EditNoteDialog(
                    type = noteToEdit.value!!.type,
                    noteTitle = noteToEdit.value?.title ?: "",
                    noteDesc = noteToEdit.value?.desc ?: "",
                    onDismissRequest = { noteToEdit.value = null },
                    onConfirmation = { title, desc ->
                        editNote(noteToEdit.value as Note, title, desc)
                        noteToEdit.value = null
                    }
                )
            }

            noteToDelete.value != null -> {
                ConfirmationDialog(
                    onDismissRequest = { noteToDelete.value = null }, // Cancel deletion
                    onConfirmation = {
                        deleteNote(noteToDelete.value!!)
                        noteToDelete.value = null
                    },
                    dialogTitle = "Delete ${noteToDelete.value?.title}",
                    dialogText = "Are you sure you want to '${noteToDelete.value?.title}'? This action cannot be undone."
                )
            }
        }
    }
}
