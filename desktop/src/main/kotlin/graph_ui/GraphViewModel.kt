package graph_ui

import androidx.compose.runtime.*
import individual_board.model.IndvBoardModel
import kotlinx.coroutines.*
import shared.ISubscriber

class GraphViewModel(
    private val model: GraphModel,
): ISubscriber {

    // NOTE: I have tried val nodes = mutableStateListOf<Node>()
    //      then doing nodes.clear() and nodes.addAll(model.nodes) in update()
    // However, this causes a bug in the canvas where while the nodes are clearing,
    //      it is also iterating in the view, causing some out of bounds error
    // I know this looks like a weird solution but I've tried a lot of things that don't work
    var nodes by mutableStateOf(mutableStateListOf<Node>())
    var edges by mutableStateOf(mutableStateListOf<Edge>())

    private val vmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init{
        model.subscribe(this)
        update()
        vmScope.launch {
            model.runSimulationLoop()
        }
    }

    fun initDimensions(width: Float, height: Float){
        model.initDimensions(width, height)
        println("Width: $width, Height: $height")
    }

    fun onPress() {
        model.onPress()
    }

    fun onRelease() {
        model.onRelease()
    }

    fun onMouseMove(x: Float, y: Float) {
        model.onMouseMove(x, y)
    }

    override fun update() {
        nodes = model.nodes.toMutableStateList()
        edges = model.edges.toMutableStateList()
    }

}
