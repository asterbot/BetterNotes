import fdg_layout.*
import kotlin.test.*


class FdgLayoutModelTest {

    private lateinit var model: FdgLayoutModel<String>

    @BeforeTest
    fun setup() {
        model = FdgLayoutModel()
    }

    @Test
    fun togglePhysics() {
        model.togglePhysics(true)
        assertTrue(model.physicsRunning)

        model.togglePhysics(false)
        assertFalse(model.physicsRunning)
    }

    @Test
    fun initializeGraph() {
        val node1: Node<String> = Node(pos=Vec(0f, 0f), data = "A")
        val node2: Node<String> = Node(pos=Vec(0f, 0f), data = "B")

        model.initializeGraph {
            nodes.add(node1)
            nodes.add(node2)
            edges.add(Edge(0, 1))
        }

        assertEquals(2, model.nodes.size)
        assertEquals(1, model.edges.size)
        assertEquals("A", model.nodes[0].data)
        assertEquals("B", model.nodes[1].data)
    }

    @Test
    fun initDimensionsUpdatesCanvasAndSpreadsNodes() {
        val node1: Node<String> = Node(pos=Vec(0f, 0f), data = "A")
        val node2: Node<String> = Node(pos=Vec(0f, 0f), data = "B")

        model.initializeGraph {
            nodes.add(node1)
            nodes.add(node2)
            edges.add(Edge(0, 1))
        }

        model.initDimensions(800f, 600f)

        assertEquals(800f, model.canvasWidth)
        assertEquals(600f, model.canvasHeight)

        model.nodes.forEach {
            val xInRange = it.pos.x >= -16000f && it.pos.x <= 16000f
            val yInRange = it.pos.y >= -12000f && it.pos.y <= 12000f
            assertTrue(xInRange && yInRange, )
        }
    }

    @Test
    fun applyForcesEmpty() {
        model.nodes.clear()
        model.applyForces()
    }

    @Test
    fun applyForcesNonEmpty() {
        val node1: Node<String> = Node(pos=Vec(0f, 0f), data = "A")
        val node2: Node<String> = Node(pos=Vec(0f, 0f), data = "B")

        model.initializeGraph {
            nodes.add(node1)
            nodes.add(node2)
            edges.add(Edge(0, 1))
        }

        model.applyForces()
        val afterForce = model.nodes.map { it.force }

        assertTrue(afterForce.any { it.x != 0f || it.y != 0f })
    }

    @Test
    fun mouseMove() {
        val node1: Node<String> = Node(pos=Vec(0f, 0f), data = "A")
        val node2: Node<String> = Node(pos=Vec(0f, 0f), data = "B")

        model.initializeGraph {
            nodes.add(node1)
            nodes.add(node2)
            edges.add(Edge(0, 1))
        }
        model.onMouseMove(100f, 150f)
        assertEquals(100f, model.mousePos.x)
        assertEquals(150f, model.mousePos.y)
    }

    @Test
    fun mouseRelease() {
        model.mouseClicked = true
        model.lerpValue = 0.5f
        model.onRelease()

        assertFalse(model.mouseClicked)
        assertEquals(0.2f, model.lerpValue)
    }
}