import androidx.compose.ui.window.application
import androidx.compose.ui.window.Window
import model.Model
import view.ViewModel
import view.MainView
import model.BoardModel

fun main() {
    var boardModel = BoardModel()
    var boardViewModel = ViewModel(boardModel)

    application {
        Window(onCloseRequest = ::exitApplication) {
            MainView(boardViewModel = boardViewModel)
        }
    }
}

