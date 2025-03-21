package graph_ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import individual_board.entities.Note
import shared.Colors
import shared.graphViewModel

// NOTE: I figured out there was some dumb thing where Canvas objects take px offsets
//      and Button objects take dp offsets. All the px/dp stuff is the result of hours of
//      head smashing
@Composable
internal fun Float.pxToDp(): Dp {
    return (this / LocalDensity.current.density).dp
}

@Composable
fun GraphView(
    onClick: (Note) -> Unit
) {
    var graph by remember { mutableStateOf(graphViewModel) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        graph.onPress()
                        tryAwaitRelease()
                        graph.onRelease()
                    }
                )
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pos = event.changes.firstOrNull()?.position
                        if (pos != null) {
                            graph.onMouseMove(pos.x, pos.y)
                        }
                    }
                }
            }
    ) {
        // init dimensions
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        LaunchedEffect(width, height) {
            graph.initDimensions(width, height)
        }

        Box(modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart
        )
        {
            Canvas(modifier = Modifier.fillMaxSize()) {
                graph.edges.forEach { edge ->
                    val n1 = graph.nodes.getOrNull(edge.id1)
                    val n2 = graph.nodes.getOrNull(edge.id2)

                    if (n1 != null && n2 != null) {
                        drawLine(
                            color = Color.Black,
                            start = Offset(n1.pos.x, n1.pos.y),
                            end = Offset(n2.pos.x, n2.pos.y),
                            strokeWidth = 1f,
                        )
                    }
                }
            }

            graph.nodes.forEach { node ->
                var buttonSize by remember { mutableStateOf(IntSize.Zero) }
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    ),
                    contentPadding = PaddingValues(8.dp),
                    onClick = {
                        onClick(node.note)
                    },
                    shape = RoundedCornerShape(10.dp),
                    // don't even question it
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        buttonSize = coordinates.size
                    }.offset(
                        x = node.pos.x.pxToDp() - buttonSize.width.toFloat().pxToDp() / 2,
                        y = node.pos.y.pxToDp() - buttonSize.height.toFloat().pxToDp() / 2
                    ).width(70.dp
                    ).padding(0.dp
                    )
                ) {
                    Text(
                        text = node.note.title,
                        fontSize = 10.sp,
                        lineHeight = 10.sp,
                        color = Color.White,
                        modifier = Modifier.padding(0.dp)
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}