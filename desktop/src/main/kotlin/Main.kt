import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import boards.view.BoardViewScreen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import shared.*
import kotlinx.coroutines.*

// Filekit (delete "core" from import and don't use $verison in dependency)
import io.github.vinceglb.filekit.FileKit
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.text.font.FontWeight
import cafe.adriel.voyager.core.screen.Screen
import login.view.LoginView
import login.view.LoginViewScreen
import kotlin.math.exp

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
    if (result!=null){
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
        Window(onCloseRequest = ::exitApplication) {
            AppScaffold(startScreen)
        }
    }
}

