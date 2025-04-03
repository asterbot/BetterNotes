package shared

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun AddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = {
            onClick()
        },
        modifier = modifier,
        containerColor = Colors.lightTeal
    ) {
        Icon(Icons.Filled.Add, "Add Board Button", tint = Colors.darkGrey)
    }
}

@Composable
fun ActionMenu(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
    ) {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options", tint=Colors.white)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = modifier
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = { onEdit(); expanded = false },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit") }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = { onDelete(); expanded = false },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete") }
            )
        }
    }
}

@Composable
fun AddNoteMenu(
    onAddSection: () -> Unit,
    onAddArticle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
    ) {
        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = Colors.lightTeal
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", tint = Colors.darkGrey)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Add Section") },
                onClick = { onAddSection(); expanded = false },
            )
            DropdownMenuItem(
                text = { Text("Add Article") },
                onClick = { onAddArticle(); expanded = false },
            )
        }
    }
}