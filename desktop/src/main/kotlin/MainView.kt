package mainview

import androidx.compose.runtime.*
import boards.view.BoardsView
import individual_board.view.IndividualBoardView
import boards.view.ViewModel

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
