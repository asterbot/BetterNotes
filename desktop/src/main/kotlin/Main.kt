

// Filekit (delete "core" from import and don't use $verison in dependency)
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import boards.view.BoardViewScreen
import cafe.adriel.voyager.core.screen.Screen
import io.github.vinceglb.filekit.FileKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import login.view.LoginViewScreen
import shared.*

// Concurrently executes both sections
fun pingDB(scope: CoroutineScope) { // this: CoroutineScope
    scope.launch {
        while (true) {
            delay(100L)
//            println("Pinging DB")
            dbStorage.pingDB()
        }
    }
}

fun main() {
    val backgroundScope = CoroutineScope(Dispatchers.IO)

    try{
        pingDB(backgroundScope)
    }
    catch (e: Exception){
        println("Mongo DB connection error")
        ConnectionManager.updateConnection(ConnectionStatus.DISCONNECTED)
    }


    // Initialize FileKit
    FileKit.init(appId = "cs-346-project")

    // Starting screen
    var startScreen : Screen = LoginViewScreen()

    val result = loginModel.getUser()
    if (result != null){
        val username = result.first
        val password = result.second
        if (dbStorage.authenticate(username, password)) {
            startScreen = BoardViewScreen()
            LoginManager.logIn()
            loginModel.changeCurrentUser(username)
            initializeModels()
        }
    }
    application {
        Window(onCloseRequest = ::exitApplication,
            title = "BetterNotes",
            icon = painterResource("betternotes_logo.png")
        ) {
            AppScaffold(startScreen)
        }
    }
}


