package shared

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

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
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Cancel")
            }
        }
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
                    label = { androidx.compose.material.Text("Board Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isError,
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Input field for description
                TextField(
                    value = boardDesc,
                    onValueChange = { boardDesc = it },
                    label = { androidx.compose.material.Text("Board Description") },
                    modifier = Modifier.fillMaxWidth()
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
                }
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
                }
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddNoteDialog(
    type: String,
    onDismissRequest: () -> Unit,
    onConfirmation: (boardName: String, boardDesc: String) -> Unit
) {
    var noteTitle by remember { mutableStateOf(TextFieldValue("")) }
    var noteDesc by remember { mutableStateOf(TextFieldValue("")) }

    var isError by remember { mutableStateOf(false) }

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
                    label = { androidx.compose.material.Text("Note Title") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isError,
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Input field for description
                TextField(
                    value = noteDesc,
                    onValueChange = { noteDesc = it },
                    label = { androidx.compose.material.Text("Note Description") },
                    modifier = Modifier.fillMaxWidth()
                )
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
                            noteDesc.text
                        )
                        noteTitle = TextFieldValue("")
                        noteDesc = TextFieldValue("")
                    }
                }
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
                }
            ) {
                Text("Cancel")
            }
        }
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
        icon = { Icons.Default.Add },
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
                    label = { androidx.compose.material.Text("Board Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isError,
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Input field for description
                TextField(
                    value = newBoardDesc,
                    onValueChange = { newBoardDesc = it },
                    label = { androidx.compose.material.Text("Board Description") },
                    modifier = Modifier.fillMaxWidth()
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
                }
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
                }
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditNoteDialog(
    type: String,
    onDismissRequest: () -> Unit,
    onConfirmation: (noteTitle: String, noteDesc: String) -> Unit,
    noteTitle: String,
    noteDesc: String
) {
    var newNoteTitle by remember { mutableStateOf(TextFieldValue(noteTitle)) }
    var newNoteDesc by remember { mutableStateOf(TextFieldValue(noteDesc)) }

    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        icon = { Icons.Default.Add },
        title = { Text(text = "Edit ${type}") },
        text = {
            Column(modifier = Modifier.padding(16.dp)) {
                // Input field for title
                TextField(
                    value = newNoteTitle,
                    onValueChange = { newText ->
                        newNoteTitle = newText
                        isError = newText.text.isBlank()
                    },
                    label = { androidx.compose.material.Text("Note Title") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isError,
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Input field for description
                TextField(
                    value = newNoteDesc,
                    onValueChange = { newNoteDesc = it },
                    label = { androidx.compose.material.Text("Note Description") },
                    modifier = Modifier.fillMaxWidth()
                )
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
                    }
                    else {
                        onConfirmation(
                            newNoteTitle.text,
                            newNoteDesc.text
                        )
                        newNoteTitle = TextFieldValue("")
                        newNoteDesc = TextFieldValue("")
                    }
                }
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
                }
            ) {
                Text("Cancel")
            }
        }
    )
}