package shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import individual_board.entities.Note


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
                Spacer(modifier = Modifier.height(8.dp))
                // Autocomplete field for related notes
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { androidx.compose.material.Text("Add Related Note") },
                    modifier = Modifier.fillMaxWidth()
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
                    Text("Related Notes:")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        relatedNotes.forEach { note ->
                            Surface(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clickable {
                                        // Remove the note when clicked (optional)
                                        relatedNotes = relatedNotes - note
                                        suggestions = suggestions.filter { it !in relatedNotes }
                                    },
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = note.title,
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
                    relatedNotes = emptyList()
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
    type: String?,
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
    var relatedNotes by remember { mutableStateOf(initialRelatedNotes.toMutableStateList()) }

    var isError by remember { mutableStateOf(false) }

    // Update suggestions on query change
    LaunchedEffect(query) {
        suggestions = onGetOtherNotes(query)
            .filter { it !in relatedNotes }
    }

    AlertDialog(
        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
        title = { Text(text = "Edit ${type}") },
        text = {
            Column(modifier = Modifier.padding(16.dp)) {
                // Title field
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
                // Description field
                TextField(
                    value = newNoteDesc,
                    onValueChange = { newNoteDesc = it },
                    label = { androidx.compose.material.Text("Note Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Related note search input
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { androidx.compose.material.Text("Add Related Note") },
                    modifier = Modifier.fillMaxWidth()
                )
                // Suggestions list
                LazyColumn {
                    items(suggestions) { note ->
                        Text(
                            text = note.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (note !in relatedNotes) {
                                        relatedNotes.add(note)
                                        query = ""
                                        suggestions = emptyList()
                                    }
                                }
                                .padding(8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Chips for current related notes
                if (relatedNotes.isNotEmpty()) {
                    Text("Related Notes:")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        relatedNotes.forEach { note ->
                            Surface(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clickable {
                                        relatedNotes.remove(note)
                                        suggestions = onGetOtherNotes(query).filter { it !in relatedNotes }
                                    },
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = note.title,
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
                        relatedNotes.clear()
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
                    relatedNotes.clear()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}
