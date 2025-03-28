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

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NavButtons()
                Spacer(modifier = Modifier.height(5.dp))
                DBStatus()
            }

            // This row stays on top of all screens

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

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when (connectionStatus) {
            ConnectionStatus.DISCONNECTED -> Icons.Default.Close
            ConnectionStatus.CONNECTED -> Icons.Default.Done
            else -> Icons.Default.Refresh
        }
        val color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        val statusText = when(connectionStatus) {
            ConnectionStatus.CONNECTING -> "Connecting to DB..."
            ConnectionStatus.CONNECTED -> "Connected to DB"
            ConnectionStatus.DISCONNECTED -> "Disconnected from DB"
        }

        Icon(imageVector = icon,
            contentDescription = "DB Status",
            tint = color,
            modifier = Modifier.size(18.dp)
            )
        Spacer(modifier = Modifier.width(4.dp))
        Text(statusText, color=color, fontSize=12.sp)
    }
}

@Composable
fun NavButtons(
    modifier: Modifier = Modifier
) {
    val currScreenIndex by derivedStateOf { ScreenManager.currScreenIndex }
    val visitedScreens by derivedStateOf { ScreenManager.visitedScreens }
    val navigator = LocalNavigator.currentOrThrow

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "Home" Button
        IconButton(
            onClick = {
                ScreenManager.push(navigator, BoardViewScreen())
            },
            colors = iconButtonColours()
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home Button",
                modifier = Modifier.size(25.dp)
            )
        }
        // "Backwards" Button
        IconButton(
            onClick = {
                ScreenManager.moveBack(navigator)
            },
            colors = iconButtonColours(),
            enabled = (currScreenIndex > 0)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back Button",
                modifier = Modifier.size(25.dp)
            )
        }
        // "Forward" Button
        IconButton(
            onClick = {
                ScreenManager.moveForward(navigator)
            },
            colors = iconButtonColours(),
            enabled = (currScreenIndex < visitedScreens.size - 1)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Forward Button",
                modifier = Modifier.size(25.dp)
            )
        }
    }
}