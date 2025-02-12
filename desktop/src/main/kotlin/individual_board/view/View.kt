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
import individual_board.view.ViewModel as IndividualBoardViewModel
import boards.view.ViewModel as BoardViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import individual_board.entities.Note
import individual_board.entities.Article
import individual_board.entities.MarkdownBlock
import individual_board.entities.CodeBlock
import individual_board.entities.Section
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import individual_board.entities.ContentBlock
import globals.boardViewModel
import globals.boardModel
import globals.individualBoardModel
import globals.individualBoardViewModel

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
        colors = ButtonDefaults.buttonColors(Color(0xffB1CCD3)),
        onClick = {
            println("DEBUG: Clicked ${note.title}")
            // Implement navigator soon
        }
    ) {
        Column {
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
    board: Board,
) {
    var navigator = LocalNavigator.currentOrThrow

    val noteList by remember { mutableStateOf(individualBoardModel.noteDict[board.id]?.toList()) }

    println("DEBUG: noteList, $noteList")

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Selected Board: ${board.name}, ${board.id}", style = MaterialTheme.typography.h2)

        LazyColumn (
            contentPadding = PaddingValues(10.dp),
        ){
            for (note in noteList!!) {
                item {
                    NoteRowView(note)
                }
            }

        }

        Button(
            onClick = {
                navigator.push(BoardViewScreen())
            })
        {
            Text("Back to All Boards")
        }
    }
}