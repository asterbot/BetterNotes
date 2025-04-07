package boards.view

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                boardModel.updateAccessed(board)
                ScreenManager.push(navigator, IndividualBoardScreen(board))
            },
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Colors.medTeal
            )
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    board.name,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    board.desc,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
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
    var boardViewModel by remember { mutableStateOf(boardViewModel) }
    val navigator = LocalNavigator.currentOrThrow

    val openAddDialog = remember {mutableStateOf(false) }
    val boardToEdit = remember { mutableStateOf<Board?> (null) }
    val boardToDelete = remember { mutableStateOf<Board?>(null) }

    // Sorting
    var selectedSort by remember { mutableStateOf(boardModel.currentSortType) }
    var reverseOrder by remember { mutableStateOf(boardModel.currentIsReversed) }
    val sortOptions = listOf("Title", "Last Created", "Last Updated", "Last Accessed")
    var expandedSort by remember { mutableStateOf(false) }

    fun applySorting(type: String, reverse: Boolean) {
        when (type) {
            "Title" -> boardModel.sortByTitle(reverse)
            "Last Created" -> boardModel.sortByDatetimeCreated(reverse)
            "Last Updated" -> boardModel.sortByDatetimeUpdated(reverse)
            "Last Accessed" -> boardModel.sortByDatetimeAccessed(reverse)
        }
    }

    LaunchedEffect(selectedSort, reverseOrder) {
        applySorting(selectedSort, reverseOrder)
    }

    // searching
    var query by remember { mutableStateOf("") }

    val searchFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }

    // compute the filtered board list based on the query
    val filteredBoards = if (query.isBlank()) {
        boardViewModel.boardList
    } else {
        boardViewModel.boardList.filter { it.name.contains(query, ignoreCase = true) or it.desc.contains(query, ignoreCase = true) }
    }

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
        modifier = Modifier.fillMaxSize().background(Colors.veryLightTeal)
            .onPreviewKeyEvent {
                when {
                    (it.isCtrlPressed && it.key == Key.N) -> {
                        openAddDialog.value = true
                        true
                    }
                    else -> false
                }
    },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Boards", style = MaterialTheme.typography.h2)

        // search bar
        OutlinedTextField(
            value = query,
            onValueChange = { newQuery -> query = newQuery },
            label = { Text("Search boards") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Colors.medTeal
                )
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .padding(8.dp)
                .focusRequester(searchFocusRequester),
            colors = outlinedTextFieldColours()
        )

        // sorting
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Sort by: ", modifier = Modifier.padding(end = 8.dp))

            Box {
                Text(
                    text = selectedSort,
                    modifier = Modifier
                        .background(Colors.lightGrey, shape = RoundedCornerShape(4.dp))
                        .padding(8.dp)
                        .clickable {
                            expandedSort = true
                        }
                )
                DropdownMenu(
                    expanded = expandedSort,
                    onDismissRequest = { expandedSort = false },
                    containerColor = Colors.veryLightTeal
                ) {
                    sortOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                selectedSort = option
                                expandedSort = false
                                applySorting(selectedSort, reverseOrder)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))
            Text("Reverse: ")

            Switch(
                checked = reverseOrder,
                onCheckedChange = {
                    reverseOrder = it
                    applySorting(selectedSort, reverseOrder)
                },
                colors = switchColours(),
            )
        }


        BoxWithConstraints(
            Modifier.fillMaxSize()
                .padding(15.dp)
                .background(Colors.lightGrey.times(1.03f).copy(red = Colors.lightGrey.red))
                .weight(1f)
        ) {
            val maxWidthDp = maxWidth
            val columnWidth = 300.dp // adjust to whatever size makes sense for your grid items
            val columns = (maxWidthDp / columnWidth).toInt().coerceAtLeast(1)

            val state = rememberLazyGridState()
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(10.dp),
                columns = GridCells.Fixed(columns),
                state = state
            ) {
                for (board in filteredBoards) {
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
                onDismissRequest = { boardToDelete.value = null }, // cancel deletion
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