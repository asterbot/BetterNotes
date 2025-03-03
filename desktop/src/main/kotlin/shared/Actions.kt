package shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = {
            onClick()
        },
        modifier = modifier
    ) {
        Icon(Icons.Filled.Add, "Add Board Button")
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
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
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