package boards.view

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import boards.entities.Board
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import individual_board.view.IndividualBoardScreen
import org.bson.types.ObjectId
import shared.*

class BoardViewScreen: Screen{
    @Composable
    override fun Content() {
        BoardsView()
    }
}

@Composable
fun BoardButton(
    board: Board,
    onDelete: (Board) -> Unit,
    onEdit: (Board) -> Unit,
    ) {

    val navigator = LocalNavigator.currentOrThrow

    Box (
        modifier = Modifier.padding(15.dp),
    ) {
        Button(
            onClick = {
                println("DEBUG: Clicked ${board.name}")
                boardModel.updateAccessed(board)
                ScreenManager.push(navigator, IndividualBoardScreen(board))
            },
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(15.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("${board.name} \n", textAlign = TextAlign.Center)
                Text(board.desc, textAlign = TextAlign.Center)
            }
        }
        ActionMenu(
            onEdit = { onEdit(board) },
            onDelete = { onDelete(board) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        )
    }
}


@Composable
fun BoardsView() {
    // NOTE: you technically can directly access boardViewModel because it's global right now
    // however, i'm not sure if that'll still be the case after database integration
    var boardViewModel by remember { mutableStateOf(boardViewModel) }

    val openAddDialog = remember {mutableStateOf(false) }
    val boardToEdit = remember { mutableStateOf<Board?> (null) }
    val boardToDelete = remember { mutableStateOf<Board?>(null) }

    fun addBoard(name: String, desc: String) {
        boardModel.add(Board(ObjectId(), name, desc))
    }

    fun deleteBoard(board: Board) {
        boardModel.del(board)
    }

    fun editBoard(board: Board, name: String, desc: String) {
        boardModel.update(board, name, desc)
    }

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
                for (board in boardViewModel.boardList) {
                    item {
                        BoardButton(
                            board = board,
                            onDelete = { boardToDelete.value = board },
                            onEdit = { boardToEdit.value = board }
                        )
                    }
                }
            }
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState = state)
            )

            AddButton(
                onClick = {
                    openAddDialog.value = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd) // bottom-right pos
                    .padding(16.dp)
            )
        }

    }

    when {
        openAddDialog.value -> {
            AddBoardDialog(
                onDismissRequest = {
                    openAddDialog.value = false
                },
                onConfirmation = { boardName, boardDesc ->
                    addBoard(boardName, boardDesc)
                    openAddDialog.value = false
                }
            )
        }
        boardToEdit.value != null -> {
            EditBoardDialog(
                onDismissRequest = {
                    boardToEdit.value = null
                },
                onConfirmation = { boardName, boardDesc ->
                    boardToEdit.value?.let {
                        editBoard(it, boardName, boardDesc)
                    }
                    boardToEdit.value = null
                },
                boardName = boardToEdit.value?.name ?: "",
                boardDesc = boardToEdit.value?.desc ?: ""
            )
        }

        boardToDelete.value != null -> {
            ConfirmationDialog(
                onDismissRequest = { boardToDelete.value = null }, // Cancel deletion
                onConfirmation = {
                    deleteBoard(boardToDelete.value!!)
                    boardToDelete.value = null
                },
                dialogTitle = "Delete ${boardToDelete.value?.name}",
                dialogText = "Are you sure you want to delete '${boardToDelete.value?.name}'? This action cannot be undone."
            )
        }
    }
}


