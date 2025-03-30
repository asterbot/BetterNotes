package shared

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import boards.view.BoardViewScreen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import login.view.LoginViewScreen


/*
Used for content which is meant to be sticky (i.e. shown regardless of which screen you're on)
*/

@Composable
fun AppScaffold() {
    // This allows us to create "sticky" content (stays on all screens regardless of navigation)

    val openAlertDialog = remember { mutableStateOf(false) }

    val loggedIn by derivedStateOf { LoginManager.loggedIn }

    // Create the navigator with the starting screen
    Box(modifier = Modifier.fillMaxSize()) {
        // The navigator goes in the background
        Navigator(LoginViewScreen()) { _ ->
            // CurrentScreen will render the current screen from the navigator
            CurrentScreen()

            if (loggedIn){
                userButton(
                    modifier = Modifier.align(Alignment.TopStart)
                        .padding(8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (loggedIn){
                    NavButtons()
                    Spacer(modifier = Modifier.height(5.dp))
                }
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

@Composable
fun userButton(modifier: Modifier = Modifier){
    var expanded by remember { mutableStateOf(false) }

    val changePasswordDialog = remember { mutableStateOf(false) }
    val passwordConfirmError = remember { mutableStateOf(false) }
    val incorrectPasswordError = remember { mutableStateOf(false) }
    val deleteAccountDialog = remember { mutableStateOf(false) }

    val navigator = LocalNavigator.currentOrThrow

    Row(
        modifier = modifier
    ){
        IconButton(
            onClick = {
                expanded = !expanded
            },
            colors = iconButtonColours()
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                modifier = Modifier.size(25.dp)
            )
        }


        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Add the greeting item at the top of the dropdown
            DropdownMenuItem(
                text = { Text("Hi ${loginModel.currentUser}!", fontWeight = FontWeight.Bold,
                    color = LocalContentColor.current) },
                onClick = { /* No action needed for greeting */ },
                enabled = false,
                // Override the default colors to maintain full opacity when disabled
                colors = MenuDefaults.itemColors(
                    disabledTextColor = LocalContentColor.current,
                    disabledLeadingIconColor = LocalContentColor.current,
                    disabledTrailingIconColor = LocalContentColor.current
                )
            )

            HorizontalDivider(thickness = 2.dp)

            DropdownMenuItem(
                text = { Text("Change Password") },
                onClick = {
                    changePasswordDialog.value = true
                    expanded = !expanded
                }
            )
            DropdownMenuItem(
                text = { Text("Log Out") },
                onClick = {
                    expanded = !expanded
                    LoginManager.logOut()
                    navigator.push(LoginViewScreen())
                }
            )

            HorizontalDivider(thickness = 2.dp)

            DropdownMenuItem(
                text = { Text("Delete Account", color = Color.Red) },
                onClick = {
                    deleteAccountDialog.value = true
                    expanded = !expanded
                }
            )
        }

    }
    when{
        changePasswordDialog.value -> {
            ChangePasswordDialog(
                onDismissRequest = { changePasswordDialog.value = false },
                onConfirmation = {
                        oldPassword, newPassword, confirmPassword ->
                    if (newPassword != confirmPassword){
                        changePasswordDialog.value = false
                        passwordConfirmError.value = true
                        return@ChangePasswordDialog
                    }
                    val result = dbStorage.updatePassword(oldPassword, newPassword)
                    if (!result) {
                        incorrectPasswordError.value = true
                    }
                    changePasswordDialog.value = false
                }
            )
        }
        passwordConfirmError.value -> {
            WarningDialog(
                onDismissRequest = { passwordConfirmError.value = false },
                onConfirmation = { passwordConfirmError.value = false },
                dialogTitle = "Password and Confirmed password do not match",
                dialogText = "Please ensure the new password and confirmation match"
            )
        }
        incorrectPasswordError.value -> {
            WarningDialog(
                onDismissRequest = { incorrectPasswordError.value = false },
                onConfirmation = { incorrectPasswordError.value = false },
                dialogTitle = "Incorrect Password",
                dialogText = "You must enter your password correctly"
            )
        }
        deleteAccountDialog.value ->{
            DeleteAccountDialog(
                onDismissRequest = { deleteAccountDialog.value = false },
                onConfirmation = {
                    currentPassword ->
                        deleteAccountDialog.value = false
                        if (dbStorage.deleteUser(currentPassword)){
                            LoginManager.logOut()
                            navigator.push(LoginViewScreen())
                        }
                        else{
                            incorrectPasswordError.value = true
                        }
                 },
            )
        }
    }
}
