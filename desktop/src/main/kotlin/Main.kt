import androidx.compose.ui.window.application
import androidx.compose.ui.window.Window

// Boards page imports
import boards.view.ViewModel
import boards.model.Model

// Main View
import mainview.MainView

fun main() {
    var boardModel = Model()
    var boardViewModel = ViewModel(boardModel)

    application {
        Window(onCloseRequest = ::exitApplication) {
            MainView(boardViewModel = boardViewModel)
        }
    }
}

