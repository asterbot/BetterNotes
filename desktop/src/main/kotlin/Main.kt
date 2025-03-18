import androidx.compose.foundation.layout.Box
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
import boards.model.BoardModel

// Navigator imports
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.CurrentScreen

// Boards page imports
import boards.view.BoardViewScreen
import shared.*

// Filekit (delete "core" from import and don't use $verison in dependency)
import io.github.vinceglb.filekit.FileKit

fun main() {
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
        Navigator(BoardViewScreen()) { _ ->
            // CurrentScreen will render the current screen from the navigator
            CurrentScreen()

            // StickyButton stays on top of all screens
            StickyButton(
                onClick = {
                    if (!ConnectionManager.isConnected) {
                        // Not connected, attempt to connect
                        if (!dbStorage.connect()){
                            openAlertDialog.value = true
                        }
                        else{
                            boardModel.initialize()
                            individualBoardModel.initialize()
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
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
fun StickyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionStatus by derivedStateOf { ConnectionManager.isConnected }

    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Colors.lightTeal
        )
    ) {
        Text(text="DB connection: ${connectionStatus}")
    }
}
