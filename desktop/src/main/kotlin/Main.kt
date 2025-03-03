import androidx.compose.ui.window.application
import androidx.compose.ui.window.Window

// Navigator imports
import cafe.adriel.voyager.navigator.Navigator

// Boards page imports
import boards.view.BoardViewScreen

fun main() {
    application {
        Window(onCloseRequest = ::exitApplication) {
            // Starting screen
            Navigator(BoardViewScreen())
        }
    }
}
