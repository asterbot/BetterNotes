package individual_board.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import boards.view.*
import cafe.adriel.voyager.core.screen.Screen
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
    var markdownRendered by remember { mutableStateOf(false) }
    var navigator = LocalNavigator.currentOrThrow
    var text by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Selected Board: $boardName", style = MaterialTheme.typography.h2)
        Row(
            //modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CanvasButton(
                onToggleCanvas = { isDrawingCanvasOpen = !isDrawingCanvasOpen },
                isCanvasOpen = isDrawingCanvasOpen
            )

            Button(
                onClick = {
                    navigator.push(BoardViewScreen(boardView))
                })
            {
                Text("Back to All Boards")
            }

            MarkdownButton(
                onToggleRender = { markdownRendered = !markdownRendered },
            )
        }

        // Show drawing canvas if it's open
        if (isDrawingCanvasOpen) {
            DrawingCanvas()
        }


        EditableTextBox(onTextChange = {text = it})
        if (markdownRendered) {
            MarkdownRenderer(text)
        }


    }
}
