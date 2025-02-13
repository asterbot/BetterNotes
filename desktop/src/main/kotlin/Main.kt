import androidx.compose.ui.window.application
import androidx.compose.ui.window.Window

// Boards page imports
import boards.view.BoardViewScreen

import cafe.adriel.voyager.navigator.Navigator


//import individual_board.view.ViewModel as IndividualBoardViewModel
//import boards.view.ViewModel as BoardViewModel
//import individual_board.model.Model as IndividualBoardModel
//import boards.model.Model as BoardModel
//
//val boardModel = BoardModel()
//val boardViewModel = BoardViewModel(boardModel)
//
//val individualBoardModel = IndividualBoardModel()
//val individualBoardViewModel = IndividualBoardViewModel(individualBoardModel)

fun main() {
    application {
        Window(onCloseRequest = ::exitApplication) {
            // Starting screen
            Navigator(BoardViewScreen())
        }
    }
}
