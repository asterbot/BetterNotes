package mainview

import androidx.compose.runtime.*
import boards.view.BoardsView
import individual_board.view.IndividualBoardView
import boards.view.ViewModel as BoardViewModel
import individual_board.view.ViewModel as IndividualBoardViewModel

@Composable
fun MainView(boardViewModel: BoardViewModel, individualBoardViewModel: IndividualBoardViewModel) {
    var currentView by remember { mutableStateOf("BoardsView") }
    var currentIndividualBoard by remember { mutableStateOf("") }
    var individualBoardViewModel = individualBoardViewModel

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
            onIndividualNote = {
                selectedNoteId -> selectedNoteId.toString()
                currentView = "IndividualNoteView"
            },
            boardName = currentIndividualBoard,
            individualBoardViewModel = individualBoardViewModel
        )
    }
}
