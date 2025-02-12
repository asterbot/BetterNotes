import androidx.compose.ui.window.application
import androidx.compose.ui.window.Window

// Boards page imports
import boards.view.ViewModel
import boards.model.Model
import boards.view.BoardViewScreen

import cafe.adriel.voyager.navigator.Navigator

fun main() {
    val boardModel = Model()
    val boardViewModel = ViewModel(boardModel)

    application {
        Window(onCloseRequest = ::exitApplication) {
            // Starting screen
            Navigator(BoardViewScreen(boardViewModel))
        }
    }
}
