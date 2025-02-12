package individual_board.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import boards.view.BoardViewScreen
import boards.view.CanvasButton
import boards.view.DrawingCanvas
import boards.view.ViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import androidx.compose.runtime.*


data class IndividualBoardScreen(val boardName: String, val boardView: ViewModel): Screen{
    @Composable
    override fun Content() {
        IndividualBoardView(boardName, boardView)
    }
}

@Composable
fun IndividualBoardView(boardName: String, boardView: ViewModel) {
    var isDrawingCanvasOpen by remember { mutableStateOf(false) }
    var navigator = LocalNavigator.currentOrThrow
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Selected Board: $boardName", style = MaterialTheme.typography.h2)
        CanvasButton(
            onToggleCanvas = { isDrawingCanvasOpen = !isDrawingCanvasOpen },
            isCanvasOpen = isDrawingCanvasOpen
        )

        // Show drawing canvas if it's open
        if (isDrawingCanvasOpen) {
            DrawingCanvas()
        }

        Button(
            onClick = {
                navigator.push(BoardViewScreen(boardView))
        })
        {
            Text("Back to All Boards")
        }
    }
}
