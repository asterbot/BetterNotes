package fdg_layout

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import observer.ISubscriber

class FdgLayoutViewModel<NodeDataType>(
    private val model: FdgLayoutModel<NodeDataType>
): ISubscriber {
    // Yes this looks weird but it will not work the normal way
    var nodes by mutableStateOf(mutableStateListOf<Node<NodeDataType>>())
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