package boards.view

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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import individual_board.view.IndividualBoardScreen
import individual_board.view.ViewModel as IndividualBoardViewModel
import boards.view.ViewModel as BoardViewModel
import globals.boardViewModel
import globals.boardModel
import globals.individualBoardModel
import globals.individualBoardViewModel

class BoardViewScreen(): Screen{
    @Composable
    override fun Content() {
        BoardsView()
    }
}

@Composable
fun BoardButton(
    board: Board,
) {
    val navigator = LocalNavigator.currentOrThrow
    Button(
        modifier = Modifier.padding(15.dp),
        colors = ButtonDefaults.buttonColors(Color(0xffB1CCD3)),
        onClick = {
            println("DEBUG: Clicked ${board.name}")
//            onLeftClickBoard(board.id)
            navigator.push(IndividualBoardScreen(board))
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
                        BoardButton(board = board)
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
