import androidx.compose.ui.window.application
import androidx.compose.ui.window.Window

// Boards page imports
import boards.view.ViewModel as BoardViewModel
import boards.model.Model as BoardModel

// Individual Board page imports
import individual_board.view.ViewModel as IndividualBoardViewModel
import individual_board.model.Model as IndividualBoardModel

// Main View
import mainview.MainView

fun main() {
    var boardModel = BoardModel()
    var boardViewModel = BoardViewModel(boardModel)

    var individualBoardModel = IndividualBoardModel(0)
    var individualBoardViewModel = IndividualBoardViewModel(individualBoardModel)

    application {
        Window(onCloseRequest = ::exitApplication) {
            MainView(
                boardViewModel = boardViewModel,
                individualBoardViewModel = individualBoardViewModel
            )
        }
    }
}

