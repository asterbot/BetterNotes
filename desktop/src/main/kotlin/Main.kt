import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.compose.ui.window.Window
import androidx.compose.runtime.*

// Navigator imports
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.CurrentScreen

// Boards page imports
import boards.view.BoardViewScreen
import shared.*
import kotlinx.coroutines.*

// Filekit (delete "core" from import and don't use $verison in dependency)
import io.github.vinceglb.filekit.FileKit
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import login.view.LoginView
import login.view.LoginViewScreen

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
    application {
        Window(onCloseRequest = ::exitApplication) {
            // Starting screen
            AppScaffold()
        }
    }
}

@Composable
fun AppScaffold() {
    // This allows us to create "sticky" content (stays on all screens regardless of navigation)

    val openAlertDialog = remember { mutableStateOf(false) }

    // Create the navigator with the starting screen
    Box(modifier = Modifier.fillMaxSize()) {
        // The navigator goes in the background
        Navigator(LoginViewScreen()) { _ ->
            // CurrentScreen will render the current screen from the navigator
            CurrentScreen()

            // This row stays on top of all screens
            DBStatus(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )

            when{
                openAlertDialog.value -> {
                    AlertDialog(
                        icon = {
                            Icon(Icons.Default.Info, contentDescription = "Icon")
                        },
                        title = {
                            Text(text = "Unable to connect to database")
                        },
                        text = {
                            Text(text = "Use offline or try to connect again later")
                        },
                        onDismissRequest = {
                            openAlertDialog.value = false
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    openAlertDialog.value = false
                                }
                            ) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    openAlertDialog.value = false
                                }
                            ) {
                                Text("Dismiss")
                            }
                        }
                    )

                }
            }
        }
    }
}

@Composable
fun DBStatus(
    modifier: Modifier = Modifier
) {
//    val connectionStatus by derivedStateOf { ConnectionManager.isConnected }
    val connectionStatus by derivedStateOf { ConnectionManager.connection }
    val isConnected by derivedStateOf { ConnectionManager.isConnected }

    Row(modifier = modifier) {
        val icon = when (connectionStatus) {
            ConnectionStatus.DISCONNECTED -> Icons.Default.Close
            ConnectionStatus.CONNECTED -> Icons.Default.Done
            else -> Icons.Default.Refresh
        }
        val color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        val statusText = when(connectionStatus) {
            ConnectionStatus.CONNECTING -> "Connecting to DB ..."
            ConnectionStatus.CONNECTED -> "Connected to DB"
            ConnectionStatus.DISCONNECTED -> "Disconnected from DB"
        }

        Icon(imageVector = icon, contentDescription = statusText, tint = color)
        Spacer(modifier = Modifier.width(8.dp))
        Text(statusText, color = color)
    }
}
