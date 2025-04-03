package login.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                    colors = outlinedTextFieldColours(),
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    colors = outlinedTextFieldColours(),
                    value = password,
                    onValueChange = { password = it },
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
                    }
                    ,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (dbStorage.authenticate(username, password)) {
                            LoginManager.logIn()
                            loginModel.changeCurrentUser(username)
                            initializeModels()
                            navigator.push(BoardViewScreen())
                        }
                        else{
                            openSignInWarning.value = true
                        }
                    },
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
                        val result = loginModel.addUser(username, password)
                        if (!result){
                            openSignUpDialog.value = false
                            openSignUpWarning.value = true
                        }
                        else{
                            openSignUpDialog.value = false
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
        }
    }
}
