import androidx.compose.ui.window.application
import androidx.compose.ui.window.Window
import model.Model
import view.ViewModel
import view.App
import view.MainView

fun main() {
    var model = Model()
    var viewModel = ViewModel(model)

    application {
        Window(onCloseRequest = ::exitApplication) {
            MainView()
        }
    }
}

