package graph_ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import individual_board.entities.Note
import individual_board.model.IndvBoardModel
import individual_board.view.IndvBoardViewModel
import org.bson.types.ObjectId
import shared.IPublisher
import kotlin.random.Random
import kotlinx.coroutines.delay
import shared.ISubscriber

class GraphModel: IPublisher() {

    var nodes = mutableListOf<Node>()
    var edges = mutableListOf<Edge>()

    val gravityConstant = 1.1f
    val forceConstant = 14000f // The higher the value, the further apart the nodes

    var physicsRunning by mutableStateOf(true) // TODO: pause/resume when the screen is exited/clicked

    val startDisMultiplier = 20f // Start spread of nodes. Not that relevant
    val maxDis = 15f

    var w by mutableStateOf(600f) // Canvas width, default 600 but dynamically updated
    var h by mutableStateOf(600f) // Canvas height, default 600 but dynamically updated

    // Variables relating to when mouse is clicked
    var closeNodeIndex by mutableStateOf(0)
    var mousePos by mutableStateOf(Vec(0.5f * w, 0.5f * h))
    var mouseClicked by mutableStateOf(false)
    var lerpValue by mutableStateOf(0.2f) // Linear interpolation value for dragging nodes

    // Initialize the graph (add nodes and edges)
    fun initializeNotesByNoteList(noteList: List<Note>) {
        nodes.clear()
        edges.clear()

        val objectIdToIndex = mutableMapOf<ObjectId, Int>()

        // nodes
        noteList.forEachIndexed { index, note ->
            val newNode = Node(
                pos = Vec(
                    x = Random.nextFloat() * (startDisMultiplier * w * 2) - (startDisMultiplier * w),
                    y = Random.nextFloat() * (startDisMultiplier * h * 2) - (startDisMultiplier * h)
                ),
                mass = 5f,
                note = note
            )
            nodes.add(newNode)
            objectIdToIndex[note.id] = index
        }

        // edges
        noteList.forEach { note ->
            val thisIndex = objectIdToIndex[note.id] ?: return@forEach
            note.relatedNotes.forEach { childId ->
                val childIndex = objectIdToIndex[childId]
                if (childIndex != null) {
                    // Make sure edge doesn't already exist in the other direction
                    // (i.e. if A is connected to B, B should not be connected to A)
                    if (edges.none { it.id1 == childIndex && it.id2 == thisIndex }) {
                        edges.add(
                            Edge(
                                id1 = thisIndex,
                                id2 = childIndex,
                            )
                        )
                    }
                }
            }
        }
        notifySubscribers()
    }

    // Updates canvas dimensions
    fun initDimensions(width: Float, height: Float) {
        w = width
        h = height

        nodes.forEach { node ->
            node.pos.x = Random.nextFloat() * (startDisMultiplier * w * 2) - (startDisMultiplier * w)
            node.pos.y = Random.nextFloat() * (startDisMultiplier * h * 2) - (startDisMultiplier * h)
        }

        notifySubscribers()
    }

    // Applies physics forces (gravity, repulsion, and spring forces) using `w` and `h` for scaling.
    // NOTE: nodesCopy is necessary because we need to update all nodes at once
    fun applyForces() {
        if (nodes.isEmpty()) return

        var nodesCopy = nodes.toMutableList()

        nodesCopy.forEach { it.force = Vec(0f, 0f) }

        // Gravity towards canvas center
        val center = Vec(w / 2, h / 2)

        nodesCopy.forEach { node ->
            val gravity = node.pos.copy()
                .sub(center)
                .mult(-1f)
                .mult(gravityConstant)
            node.force.add(gravity)
        }

        // Repulsive force between nodes
        for (i in nodesCopy.indices) {
            for (j in i + 1 until nodesCopy.size) {
                val n1 = nodesCopy[i]
                val n2 = nodesCopy[j]
                val dir = n2.pos.copy().sub(n1.pos)
                val dist = dir.mag()

                if (dist > 0f) {
                    dir.div(dist * dist)
                }
                dir.mult(forceConstant)

                n1.force.sub(dir.copy())
                n2.force.add(dir)
            }
        }

        // Spring connection forces
        edges.forEach { edge ->
            val node1 = nodesCopy[edge.id1]
            val node2 = nodesCopy[edge.id2]
            val distVec = node1.pos.copy().sub(node2.pos)
            node1.force.sub(distVec).mult(0.9f)
            node2.force.add(distVec).mult(0.9f)
        }

        // Update nodes
        nodesCopy.forEachIndexed { index, node ->
            nodes[index].force = node.force
        }
    }

    /**
     * Runs the physics simulation loop.
     */
    suspend fun runSimulationLoop() {
        while (true) {
            if (nodes.isEmpty()) {
                delay(16)
                continue
            }
            if (physicsRunning) {
                applyForces()

                var nodesCopy = nodes.toMutableList()

                // Update node positions
                nodesCopy.forEach { node ->
                    val velocity = node.force.copy().div(node.mass)
                    node.pos.add(velocity)
                }

                nodes.clear()
                nodes.addAll(nodesCopy)

            }

            // If dragging, LERP closest node to mousePos
            if (mouseClicked && closeNodeIndex in nodes.indices && nodes.isNotEmpty()) {
                val targetNode = nodes[closeNodeIndex]
                targetNode.pos.lerp(mousePos, lerpValue)
                if (lerpValue < 0.95f) {
                    lerpValue += 0.002f
                }
            }

            notifySubscribers()

            delay(16)
        }
    }

    fun onPress() {
        if (nodes.isEmpty()) return

        mouseClicked = true

        val relativeMousePos = mousePos.copy()
        var closestNode: Node? = null

        for (node in nodes) {
            val adjustedDist = relativeMousePos.copy().sub(node.pos).mag() - node.mass / (2 * Math.PI.toFloat())
            val currentCloseNodeDist = relativeMousePos.copy().sub(nodes[closeNodeIndex].pos).mag() - nodes[closeNodeIndex].mass / (2 * Math.PI.toFloat())

            if (adjustedDist < currentCloseNodeDist) {
                closestNode = node
            }
        }
        closestNode?.let {
            closeNodeIndex = nodes.indexOf(it)
        }
    }

    fun onRelease() {
        mouseClicked = false
        lerpValue = 0.2f
    }

    fun onMouseMove(px: Float, py: Float) {
        mousePos = Vec(px, py)
    }
}
