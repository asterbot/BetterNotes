package fdg_layout

import kotlinx.coroutines.*
import observer.IPublisher
import kotlin.random.Random

class FdgLayoutModel<NodeDataType>: IPublisher() {
    val nodes = mutableListOf<Node<NodeDataType>>()
    val edges = mutableListOf<Edge>()

    // Canvas dimensions
    var canvasWidth: Float = 600f
    var canvasHeight: Float = 600f

    // Physics parameters
    private val gravityConstant = 1.1f
    private val forceConstant = 14000f
    val startDisMultiplier = 20f // Start spread of nodes. Not that relevant

    // Flag to run the simulation
    var physicsRunning = false

    // Variables relating to when mouse is clicked
    var closeNodeIndex = 0
    var mousePos = Vec(0.5f * canvasWidth, 0.5f * canvasHeight)
    var mouseClicked = false
    var lerpValue = 0.2f

    // Toggle physics (pause/resume)
    fun togglePhysics(isPhysicsRunning: Boolean) {
        physicsRunning = isPhysicsRunning
        println("Physics running: $physicsRunning")
    }

    // Initialize the graph (add nodes and edges)
    fun initializeGraph(graphBuilder: FdgLayoutModel<NodeDataType>.() -> Unit) {
        nodes.clear()
        edges.clear()
        graphBuilder()
        println("Graph initialized with ${nodes.size} nodes and ${edges.size} edges")
        println(nodes)
        println(edges)
        println("Dimensions: $canvasWidth x $canvasHeight")
        notifySubscribers()
    }

    fun initDimensions(width: Float, height: Float) {
        canvasWidth = width
        canvasHeight = height

        nodes.forEach { node ->
            node.pos.x = Random.nextFloat() * (startDisMultiplier * canvasWidth * 2) - (startDisMultiplier * canvasWidth)
            node.pos.y = Random.nextFloat() * (startDisMultiplier * canvasHeight * 2) - (startDisMultiplier * canvasHeight)
        }

        println("Width: $width, Height: $height")
        notifySubscribers()
    }

    fun applyForces() {
        if (nodes.isEmpty()) return

        var nodesCopy = nodes.toMutableList()

        nodesCopy.forEach { it.force = Vec(0f, 0f) }

        // Gravity towards canvas center
        val center = Vec(canvasWidth / 2, canvasHeight / 2)

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
        if (nodes.isEmpty() || mouseClicked) return

        mouseClicked = true

        val relativeMousePos = mousePos.copy()
        var closestNode: Node<NodeDataType>? = null
        var minDist = Float.MAX_VALUE
        var closestIndex = -1

        nodes.forEachIndexed { index, node ->
            val dist = relativeMousePos.copy().sub(node.pos).mag()
            if (dist < minDist) {
                minDist = dist
                closestIndex = index
            }
        }
        if (closestIndex != -1) {
            closeNodeIndex = closestIndex
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