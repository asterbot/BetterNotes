package view

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
import entities.Board

@Composable
fun BoardButton(board: Board, onLeftClickBoard: (Int) -> Unit) {
    Button(
        modifier = Modifier.padding(15.dp),
        colors = ButtonDefaults.buttonColors(Color(0xffB1CCD3)),
        onClick = {
            println("Clicked ${board.name}")
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
    onIndividualBoard: (Int) -> Unit,
    boardViewModel: ViewModel
) {

    val boardList by remember { mutableStateOf(boardViewModel.boardList.toList()) }

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
                        BoardButton(board = board, onLeftClickBoard = onIndividualBoard)
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

@Composable
fun IndividualBoardView(onBoardsView: () -> Unit, boardName: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Selected Board: $boardName", style = MaterialTheme.typography.h2)
        Button(onClick = onBoardsView) {
            Text("Back to All Boards")
        }
    }
}

@Composable
fun MainView(boardViewModel: ViewModel) {
    var currentView by remember { mutableStateOf("BoardsView") }
    var currentIndividualBoard by remember { mutableStateOf("") }

    when (currentView) {
        "BoardsView" -> BoardsView(
            onIndividualBoard = { selectedBoardId ->
                currentIndividualBoard = selectedBoardId.toString()
                currentView = "IndividualBoardView"
            },
            boardViewModel = boardViewModel
        )

        "IndividualBoardView" -> IndividualBoardView(
            onBoardsView = { currentView = "BoardsView" },
            boardName = currentIndividualBoard
        )
    }
}