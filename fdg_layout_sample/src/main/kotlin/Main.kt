import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import fdg_layout.*
import kotlin.random.Random

// Your custom class, i.e. what each node represents
data class Data(
    var name: String,
    var color: Color,
)

// Your data list
var sampleDataList = mutableListOf(
    Data("A", Color.Red),
    Data("B", Color.Green),
    Data("C", Color.Blue),
    Data("D", Color.Yellow),
    Data("E", Color.Cyan),
)

// Your function to convert your data list to a graph with Nodes and Edges
fun initializeFdgLayoutGraph(fdgLayoutModel: FdgLayoutModel<Data>, dataList: List<Data>) {
    // Nodes
    dataList.forEach { data ->
        val newNode = Node(
            // Randomly generate the position of the node
            pos = Vec(
                x = Random.nextFloat() * (fdgLayoutModel.startDisMultiplier * fdgLayoutModel.canvasWidth * 2) - (fdgLayoutModel.startDisMultiplier * fdgLayoutModel.canvasWidth),
                y = Random.nextFloat() * (fdgLayoutModel.startDisMultiplier * fdgLayoutModel.canvasHeight * 2) - (fdgLayoutModel.startDisMultiplier * fdgLayoutModel.canvasHeight)
            ),
            mass = 5f,
            data = data
        )
        fdgLayoutModel.nodes.add(newNode)
    }

    // Edges (arbitrary for now, but you can create edges from a 'relatedData' property in your data)
    fdgLayoutModel.edges.addAll(
        listOf(
            Edge(0, 1),
            Edge(1, 2),
            Edge(0, 2),
            )
    )
}

// Initialize the model and view model
val fdgLayoutModel = FdgLayoutModel<Data>()
val fdgLayoutViewModel = FdgLayoutViewModel<Data>(fdgLayoutModel)

// The View for the graph
@Composable
fun View() {
    // Initialize the graph with your data
    LaunchedEffect(sampleDataList) {
        fdgLayoutModel.initializeGraph {
            initializeFdgLayoutGraph(fdgLayoutModel, sampleDataList)
        }
    }

    // Enable physics (you may set this to false if you navigate to another screen
    LaunchedEffect(Unit) {
        fdgLayoutModel.togglePhysics(true)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        FdgLayoutView(
            fdgLayoutViewModel = fdgLayoutViewModel,
            onNodeClick = { data ->
                data.color = Color(
                    (0..255).random(),
                    (0..255).random(),
                    (0..255).random(),
                )
                // You may reinitialize the graph when nodes are altered
                fdgLayoutModel.initializeGraph {
                    initializeFdgLayoutGraph(fdgLayoutModel, sampleDataList)
                }
            },
            getLabel = { data -> data.name },
            getColor = { data -> data.color },
        )
    }
}

fun main() {
    application {
        val windowState = rememberWindowState()
        Window(onCloseRequest = ::exitApplication,
            title = "FDG Layout Sample",
            state = windowState
        ) {
            View()
        }
    }
}

