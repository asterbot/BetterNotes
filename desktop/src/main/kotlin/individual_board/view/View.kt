package individual_board.view
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.ui.text.style.TextAlign
import boards.entities.Board
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import individual_board.entities.Note
import individual_board.entities.Article
import individual_board.entities.MarkdownBlock
import individual_board.entities.CodeBlock
import individual_board.entities.ContentBlock


@Composable
fun NoteRowView(
    note: Note,
    onLeftClickNote: (Int) -> Unit
) {
    Button (
        modifier = Modifier.padding(15.dp),
        colors = ButtonDefaults.buttonColors(Color(0xffB1CCD3)),
        onClick = {
            println("DEBUG: Clicked ${note.title}")
            onLeftClickNote(note.id)
        }
    ) {
        Row {
            Text("${note.title} \n", textAlign = TextAlign.Center)
            note.desc?.let { Text(it, textAlign = TextAlign.Center) }
            if (note is Article) {
                for (contentBlock in note.contentBlocks) {
                    when (contentBlock) {
                        is MarkdownBlock -> {
                            Text(contentBlock.text)
                        }
                        is CodeBlock -> {
                            // Implement here
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IndividualBoardView(
    onBoardsView: () -> Unit,
    onIndividualNote: (Int) -> Unit,
    boardName: String,
    individualBoardViewModel: ViewModel,
    ) {

    val noteList by remember { mutableStateOf(individualBoardViewModel.noteList.toList()) }
    println("DEBUG: IndividualBoardView")
    println("DEBUG: $noteList")

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Selected Board: $boardName", style = MaterialTheme.typography.h2)

        Button(onClick = onBoardsView) {
            Text("Back to All Boards")
        }

        LazyColumn (
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(10.dp),
        ){

            for (note in noteList) {
                item {
                    NoteRowView(note, onLeftClickNote = onIndividualNote)
                }
            }
        }
    }
}
