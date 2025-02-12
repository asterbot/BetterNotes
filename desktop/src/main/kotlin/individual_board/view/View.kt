package individual_board.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun IndividualBoardView(onBoardsView: () -> Unit, boardName: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Selected Board: $boardName", style = MaterialTheme.typography.h2)
        Button(onClick = onBoardsView) {
            Text("Back to All Boards")
        }
    }
}
