package shared

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import individual_board.entities.Note

fun sanitizeInput(input: String): String {
    return input.filter { it.code in 32..126 } // printable ASCII only
}

@Composable
fun ConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
) {
    AlertDialog(
        icon = { Icons.Default.Info },
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(text = dialogText)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                },
                colors = transparentTextButtonColours()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                },
                colors = transparentTextButtonColours()
            ) {
                Text("Cancel")
            }
        },
        containerColor = Colors.veryLightTeal
    )
}

@Composable
fun AddBoardDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (boardName: String, boardDesc: String) -> Unit
) {
    var boardName by remember { mutableStateOf(TextFieldValue("")) }
    var boardDesc by remember { mutableStateOf(TextFieldValue("")) }

    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        icon = { Icons.Default.Add },
        title = { Text(text = "Add Board") },
        text = {
            Column(modifier = Modifier.padding(16.dp)) {
                // Input field for title
                TextField(
                    value = boardName,
                    onValueChange = { newText ->
                        boardName = newText
                        isError = newText.text.isBlank()
                    },
                    label = { Text("Board Name", color = Colors.darkGrey) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isError,
                    colors = textFieldColours()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Input field for description
                TextField(
                    value = boardDesc,
                    onValueChange = { boardDesc = it },
                    label = { Text("Board Description", color = Colors.darkGrey) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColours()
                )
            }
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (boardName.text.isBlank()) {
                        isError = true
                    }
                    else {
                        onConfirmation(
                            boardName.text,
                            boardDesc.text
                        )
                        boardName = TextFieldValue("")
                        boardDesc = TextFieldValue("")
                    }
                },
                colors = transparentTextButtonColours()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    boardName = TextFieldValue("")
                    boardDesc = TextFieldValue("")
                },
                colors = transparentTextButtonColours()
            ) {
                Text("Cancel")
            }
        },
        containerColor = Colors.veryLightTeal
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddNoteDialog(
    type: String,
    onDismissRequest: () -> Unit,
    onConfirmation: (noteName: String, noteDesc: String, relatedNotes: List<Note>) -> Unit,
    onGetOtherNotes: (String) -> List<Note>
) {
    var noteTitle by remember { mutableStateOf(TextFieldValue("")) }
    var noteDesc by remember { mutableStateOf(TextFieldValue("")) }

    // related notes
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf(listOf<Note>()) }
    var relatedNotes by remember { mutableStateOf(listOf<Note>()) }

    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        suggestions = onGetOtherNotes(query)
    }

    suggestions = suggestions.filter { it !in relatedNotes }

    AlertDialog(
        icon = { Icons.Default.Add },
        title = { Text(text = "Add ${type}") },
        text = {
            Column(modifier = Modifier.padding(16.dp)) {
                // Input field for title
                TextField(
                    value = noteTitle,
                    onValueChange = { newText ->
                        noteTitle = newText
                        isError = newText.text.isBlank()
                    },
                    label = { Text("Note Title", color = Colors.darkGrey) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isError,
                    colors = textFieldColours()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Input field for description
                TextField(
                    value = noteDesc,
                    onValueChange = { noteDesc = it },
                    label = { Text("Note Description", color=Colors.darkGrey) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColours()
                )
                Spacer(modifier = Modifier.height(8.dp))

                val suggestionListState = rememberLazyListState()

                TextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Add Related Note", color=Colors.darkGrey) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColours()
                )

                // Make the height of the suggestions block dynamic
                val suggestionsHeight = remember(suggestions.size) {
                    val itemHeight = 40
                    val totalHeight = minOf(suggestions.size * itemHeight, 2*itemHeight) // Cap at 2 blocks
                    totalHeight.dp
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(suggestionsHeight) // Use the calculated height
                ) {
                    LazyColumn(
                        state = suggestionListState,
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth()
                    ) {
                        items(suggestions) { note ->
                            Text(
                                text = note.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Add the note if it's not already selected
                                        if (note !in relatedNotes) {
                                            relatedNotes = relatedNotes + note
                                        }
                                        // Clear the query and suggestions once a selection is made
                                        query = ""
                                        suggestions = suggestions.filter { it !in relatedNotes }
                                    }
                                    .padding(8.dp)
                            )
                        }
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(suggestionListState),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Autocomplete field for related notes
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Add tags", color=Colors.darkGrey) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColours()
                )
                LazyColumn {
                    items(suggestions) { note ->
                        Text(
                            text = note.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Add the note if it's not already selected
                                    if (note !in relatedNotes) {
                                        relatedNotes = relatedNotes + note
                                    }
                                    // Clear the query and suggestions once a selection is made
                                    query = ""
                                    suggestions = suggestions.filter { it !in relatedNotes }
                                }
                                .padding(8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Display selected related notes as chips (or simple texts)
                if (relatedNotes.isNotEmpty()) {
                    Text("Related Notes:", color=Colors.darkGrey)
                    FlowRow(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalArrangement = Arrangement.Start) {
                        relatedNotes.forEach { note ->
                            Surface(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clickable {
                                        relatedNotes = relatedNotes - note
                                        suggestions = suggestions.filter { it !in relatedNotes }
                                    },
                                shape = MaterialTheme.shapes.small,
                                color = Colors.medTeal.copy(alpha=.2f)
                            ) {
                                Text(
                                    text = note.title,
                                    modifier = Modifier.padding(8.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (noteTitle.text.isBlank()) {
                        isError = true
                    }
                    else {
                        onConfirmation(
                            noteTitle.text,
                            noteDesc.text,
                            relatedNotes
                        )
                        noteTitle = TextFieldValue("")
                        noteDesc = TextFieldValue("")
                        relatedNotes = emptyList()
                    }
                },
                colors = transparentTextButtonColours()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    noteTitle = TextFieldValue("")
                    noteDesc = TextFieldValue("")
                    relatedNotes = emptyList()
                },
                colors = transparentTextButtonColours()
            ) {
                Text("Cancel")
            }
        },
        containerColor = Colors.veryLightTeal
    )
}

@Composable
fun EditBoardDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (boardName: String, boardDesc: String) -> Unit,
    boardName: String,
    boardDesc: String
) {
    var newBoardName by remember { mutableStateOf(TextFieldValue(boardName)) }
    var newBoardDesc by remember { mutableStateOf(TextFieldValue(boardDesc)) }

    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        icon = { Icons.Default.Edit },
        title = { Text(text = "Edit Board") },
        text = {
            Column(modifier = Modifier.padding(16.dp)) {
                // Input field for title
                TextField(
                    value = newBoardName,
                    onValueChange = { newText ->
                        newBoardName = newText
                        isError = newText.text.isBlank()
                    },
                    label = { Text("Board Name", color = Colors.darkGrey) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isError,
                    colors = textFieldColours()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Input field for description
                TextField(
                    value = newBoardDesc,
                    onValueChange = { newBoardDesc = it },
                    label = { Text("Board Description", color = Colors.darkGrey) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColours()
                )
            }
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newBoardName.text.isBlank()) {
                        isError = true
                    }
                    else {
                        onConfirmation(
                            newBoardName.text,
                            newBoardDesc.text
                        )
                        newBoardName = TextFieldValue("")
                        newBoardDesc = TextFieldValue("")
                    }
                },
                colors = transparentTextButtonColours()
            ) {
                Text("Save")
            }

        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    newBoardName = TextFieldValue("")
                    newBoardDesc = TextFieldValue("")
                },
                colors = transparentTextButtonColours()
            ) {
                Text("Cancel")
            }
        },
        containerColor = Colors.veryLightTeal
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditNoteDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (noteTitle: String, noteDesc: String, relatedNotes: List<Note>) -> Unit,
    noteTitle: String,
    noteDesc: String,
    initialRelatedNotes: List<Note>,
    onGetOtherNotes: (String) -> List<Note>
) {
    var newNoteTitle by remember { mutableStateOf(TextFieldValue(noteTitle)) }
    var newNoteDesc by remember { mutableStateOf(TextFieldValue(noteDesc)) }

    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf(emptyList<Note>()) }
    var relatedNotes by remember { mutableStateOf(initialRelatedNotes) }

    var isError by remember { mutableStateOf(false) }

    // Update suggestions on query change
    LaunchedEffect(query) {
        suggestions = onGetOtherNotes(query)
    }

    suggestions = suggestions.filter { it !in relatedNotes }

    AlertDialog(
        icon = { Icons.Default.Edit },
        title = { Text(text = "Edit note") },
        text = {
            Column(modifier = Modifier.padding(16.dp)) {
                // Title field
                TextField(
                    value = newNoteTitle,
                    onValueChange = { newText ->
                        newNoteTitle = newText
                        isError = newText.text.isBlank()
                    },
                    label = { Text("Note Title", color = Colors.darkGrey) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isError,
                    colors = textFieldColours()
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = newNoteDesc,
                    onValueChange = { newNoteDesc = it },
                    label = { Text("Note Description", color=Colors.darkGrey) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColours()
                )
                Spacer(modifier = Modifier.height(8.dp))

                val suggestionListState = rememberLazyListState()

                TextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Add Related Note", color = Colors.darkGrey) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColours()
                )

                // Make the height of the suggestions block dynamic
                val suggestionsHeight = remember(suggestions.size) {
                    val itemHeight = 40
                    val totalHeight = minOf(suggestions.size * itemHeight, 2*itemHeight) // Cap at 2 blocks
                    totalHeight.dp
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(suggestionsHeight) // Use the calculated height
                ) {
                    // Suggestions list
                    LazyColumn(
                        state = suggestionListState,
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth()
                    ) {
                        items(suggestions) { note ->
                            Text(
                                text = note.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (note !in relatedNotes) {
                                            relatedNotes = relatedNotes + note
                                        }
                                        query = ""
                                        suggestions = suggestions.filter { it !in relatedNotes }
                                    }
                                    .padding(8.dp)
                            )
                        }
                    }

                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(suggestionListState),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                // Chips for current related notes
                if (relatedNotes.isNotEmpty()) {
                    Text("Related Notes:")
                    FlowRow(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalArrangement = Arrangement.Start) {
                        relatedNotes.forEach { note ->
                            Surface(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clickable {
                                        relatedNotes = relatedNotes - note
                                        suggestions = onGetOtherNotes(query).filter { it !in relatedNotes }
                                    },
                                shape = MaterialTheme.shapes.small,
                                color = Colors.medTeal.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (note.title.length > 12) note.title.take(12) + "…" else note.title,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }


            }
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newNoteTitle.text.isBlank()) {
                        isError = true
                    } else {
                        onConfirmation(
                            newNoteTitle.text,
                            newNoteDesc.text,
                            relatedNotes
                        )
                        newNoteTitle = TextFieldValue("")
                        newNoteDesc = TextFieldValue("")
                        relatedNotes = emptyList()
                    }
                },
                colors = transparentTextButtonColours()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    newNoteTitle = TextFieldValue("")
                    newNoteDesc = TextFieldValue("")
                    relatedNotes = emptyList()
                },
                colors = transparentTextButtonColours()
            ) {
                Text("Cancel")
            }
        },
        containerColor = Colors.veryLightTeal
    )
}

@Composable
fun PasswordCriteriaDisplay(password: String){
    Text("Password must contain:")
    val result = loginModel.passwordCriteriaMet(password)
    loginModel.passwordCriteria.forEachIndexed { i, criteria ->
        val color = if (result[i]) Colors.medTeal else Colors.errorColor
        Row{
            Icon(imageVector = if (result[i]) Icons.Default.Done else Icons.Default.Close,
                contentDescription = criteria.first,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(criteria.first, color=color, fontSize=12.sp)
        }
    }
}

@Composable
fun SignUpDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (userName: String, password: String) -> Unit
) {
    var username by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }

    var passwordVisible by remember { mutableStateOf(false) }


    AlertDialog(
        icon = { Icons.Default.Add },
        title = { Text(text = "Sign up!") },
        text = {
            Column(modifier = Modifier.padding(16.dp)) {
                // Input field for title
                OutlinedTextField(
                    colors = outlinedTextFieldColours(),
                    value = username,
                    onValueChange = { input ->
                        val sanitized = sanitizeInput(input.text)
                        username = input.copy(text = sanitized)
                    },
                    label = { Text("Username", color = Colors.darkGrey) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Input field for description
                OutlinedTextField(
                    colors = outlinedTextFieldColours(),
                    value = password,
                    onValueChange = { input ->
                        val sanitized = sanitizeInput(input.text)
                        password = input.copy(text = sanitized)
                    },
                    label = { Text("Password", color = Colors.darkGrey) },
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
                )
                Spacer(modifier = Modifier.height(8.dp))
                PasswordCriteriaDisplay(password.text)
            }
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation(
                        username.text,
                        password.text,
                    )
                    username = TextFieldValue("")
                    password = TextFieldValue("")
                },
                colors = transparentTextButtonColours()
            ) {
                Text("Create Account!")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    username = TextFieldValue("")
                    password = TextFieldValue("")
                },
                colors = transparentTextButtonColours()
            ) {
                Text("Cancel")
            }
        },
        containerColor = Colors.veryLightTeal
    )
}

@Composable
fun WarningDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
) {
    AlertDialog(
        icon = { Icons.Default.Warning },
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(text = dialogText)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                },
                colors = transparentTextButtonColours()
            ) {
                Text("OK")
            }
        },
        containerColor = Colors.veryLightTeal
    )
}

@Composable
fun ChangePasswordDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (oldPassword: String, newPassword: String, confirmPassword: String) -> Unit,
) {

    var oldPassword by remember { mutableStateOf(TextFieldValue("")) }
    var newPassword by remember { mutableStateOf(TextFieldValue("")) }
    var confirmPassword by remember { mutableStateOf(TextFieldValue("")) }

    var isError by remember { mutableStateOf(false) }

    var passwordVisible1 by remember { mutableStateOf(false) }
    var passwordVisible2 by remember { mutableStateOf(false) }
    var passwordVisible3 by remember { mutableStateOf(false) }

    AlertDialog(
        icon = { Icons.Default.Add },
        title = { Text(text = "Change Password") },
        text = {
            Column(modifier = Modifier.padding(16.dp)) {
                // Input field for title
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("Old Password", color = Colors.darkGrey) },
                    visualTransformation = if (passwordVisible1) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible1 = !passwordVisible1 }) {
                            Icon(
                                imageVector = if (passwordVisible1) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible1) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedTextFieldColours()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Input field for description
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password", color = Colors.darkGrey) },
                    visualTransformation = if (passwordVisible2) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible2 = !passwordVisible2 }) {
                            Icon(
                                imageVector = if (passwordVisible2) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible2) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedTextFieldColours()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Input field for description
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password", color = Colors.darkGrey) },
                    visualTransformation = if (passwordVisible3) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    trailingIcon = {
                        androidx.compose.material.IconButton(onClick = { passwordVisible3 = !passwordVisible3 }) {
                            androidx.compose.material.Icon(
                                imageVector = if (passwordVisible3) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible3) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedTextFieldColours()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Password verifier
                PasswordCriteriaDisplay(newPassword.text)
            }

        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (oldPassword.text.isBlank()) {
                        isError = true
                    } else {
                        onConfirmation(oldPassword.text, newPassword.text, confirmPassword.text)
                        oldPassword = TextFieldValue("")
                        newPassword = TextFieldValue("")
                        confirmPassword = TextFieldValue("")
                    }
                },
                colors = transparentTextButtonColours()
            ) {
                Text("Change Password!")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    oldPassword = TextFieldValue("")
                    newPassword = TextFieldValue("")
                    confirmPassword = TextFieldValue("")
                },
                colors = transparentTextButtonColours()
            ) {
                Text("Cancel")
            }
        },
        containerColor = Colors.veryLightTeal
    )
}

@Composable
fun DeleteAccountDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (currentPassword: String) -> Unit,

    ) {
    var currentPassword by remember { mutableStateOf(TextFieldValue("")) }
    var passwordVisible1 by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        icon = { Icons.Default.Add },
        title = { Text(text = "Delete Account") },
        text = {
            Column(modifier = Modifier.padding(16.dp)) {
                // Input field for title
                Text("This will delete EVERYTHING you own. Are you sure?", color = Colors.errorColor)
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Confirm Password", color=Colors.darkGrey)},
                    visualTransformation = if (passwordVisible1) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible1 = !passwordVisible1 }) {
                            Icon(
                                imageVector = if (passwordVisible1) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible1) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedTextFieldColours()
                )
            }
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (currentPassword.text.isBlank()) {
                        isError = true
                    } else {
                        onConfirmation(currentPassword.text)
                        currentPassword = TextFieldValue("")
                    }
                },
                colors = transparentTextButtonColours()
            ) {
                Text("Goodbye, World!")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    currentPassword = TextFieldValue("")
                },
                colors = transparentTextButtonColours()
            ) {
                Text("Cancel")
            }
        },
        containerColor = Colors.veryLightTeal
    )
}
