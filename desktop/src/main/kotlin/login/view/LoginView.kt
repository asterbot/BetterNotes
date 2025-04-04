package login.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import boards.view.BoardViewScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import shared.*

class LoginViewScreen: Screen{
    @Composable
    override fun Content() {
        LoginView()
    }
}

@Composable
fun LoginView(){
    val navigator = LocalNavigator.currentOrThrow

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val openSignUpDialog = remember { mutableStateOf(false) }
    val openSignInWarning = remember { mutableStateOf(false) }
    val openSignUpWarning = remember { mutableStateOf(false) }
    val emptyUsernameWarning = remember { mutableStateOf(false) }
    val openUnsafePasswordWarning = remember { mutableStateOf(false) }

    val keepSignedIn = remember { mutableStateOf(false) }

    // Focus management
    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        usernameFocusRequester.requestFocus()
    }
    val loginAction = {
        if (dbStorage.authenticate(username, password)) {
            LoginManager.logIn()
            loginModel.changeCurrentUser(username)
            initializeModels()
            navigator.push(BoardViewScreen())
            if (keepSignedIn.value) {
                loginModel.saveUser(username, password)
            }
        } else {
            openSignInWarning.value = true
        }
    }

    fun sanitizeInput(input: String): String {
        return input.filter { it.code in 32..126 } // printable ASCII only
    }

    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Colors.lightTeal)
        ) {
            Text("BetterNotes", modifier = Modifier.align(Alignment.Center), fontSize = 40.sp)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Login",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = sanitizeInput(it) },
                    label = { Text("Username") },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(usernameFocusRequester),
                    colors = outlinedTextFieldColours()
                )


                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    colors = outlinedTextFieldColours(),
                    value = password,
                    onValueChange = { password = sanitizeInput(it) },
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                        .focusRequester(passwordFocusRequester)
                        .onPreviewKeyEvent {
                            when {
                                (it.key == Key.Enter) && (it.type == KeyEventType.KeyUp) -> {
                                    loginAction()
                                    true
                                }
                                else -> false
                            }
                    },
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ){
                    Checkbox(
                        colors = checkboxColours(),
                        checked = keepSignedIn.value,
                        onCheckedChange = { keepSignedIn.value = it }
                    )
                    Text("Stay logged in")
                }


                Button(
                    onClick = { loginAction() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textButtonColours()
                ) {
                    Text(text = "Log in", color = Color.White)
                }
                Button(
                    onClick = { openSignUpDialog.value = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textButtonColours()
                ) {
                    Text(text = "New here? Sign up!", color = Color.White)
                }
            }
        }

        when{
            openSignUpDialog.value -> {
                SignUpDialog(
                    onDismissRequest = {
                        openSignUpDialog.value = false
                    },
                    onConfirmation = { username, password ->
                        // Add to DB here
                        if (false in loginModel.passwordCriteriaMet(password)){
                            openSignUpDialog.value = false
                            openUnsafePasswordWarning.value = true
                        }
                        else if (username==""){
                            openSignUpDialog.value = false
                            emptyUsernameWarning.value = true
                        }
                        else{
                            val result = loginModel.addUser(username, password)
                            if (!result){
                                openSignUpDialog.value = false
                                openSignUpWarning.value = true
                            }
                            else{
                                openSignUpDialog.value = false
                            }
                        }
                    }
                )
            }
            openSignInWarning.value -> {
                WarningDialog(
                    onDismissRequest = { openSignInWarning.value = false },
                    onConfirmation = { openSignInWarning.value = false },
                    dialogTitle = "Warning",
                    dialogText = "Unable to log you in. This could be due to either Incorrect credentials or bad connection.",
                )
            }
            openSignUpWarning.value -> {
                WarningDialog(
                    onDismissRequest = { openSignUpWarning.value = false },
                    onConfirmation = { openSignUpWarning.value = false },
                    dialogTitle = "Warning",
                    dialogText = "This username is already taken. Please choose a different username"
                )
            }
            openUnsafePasswordWarning.value -> {
                WarningDialog(
                    onDismissRequest = { openUnsafePasswordWarning.value = false },
                    onConfirmation = { openUnsafePasswordWarning.value = false },
                    dialogTitle = "Warning",
                    dialogText = "Please ensure the password matches the criteria given"
                )
            }
            emptyUsernameWarning.value -> {
                WarningDialog(
                    onDismissRequest = { emptyUsernameWarning.value=false },
                    onConfirmation =  { emptyUsernameWarning.value = false },
                    dialogTitle = "Warning",
                    dialogText = "Username must not be empty"
                )
            }

        }
    }
}
