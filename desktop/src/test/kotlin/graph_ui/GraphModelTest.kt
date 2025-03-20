package graph_ui

import kotlin.test.*
import kotlinx.coroutines.*
import individual_board.entities.Note
import org.bson.types.ObjectId

class GraphModelTest {
 lateinit var graphModel: GraphModel

 @BeforeTest
 fun setup() {
  graphModel = GraphModel()

  val node1 = Node(Vec(100f, 100f), mass = 5f, note = Note(ObjectId(), "Note1", "Desc1"))
  val node2 = Node(Vec(200f, 200f), mass = 5f, note = Note(ObjectId(), "Note2", "Desc2"))
  val node3 = Node(Vec(300f, 300f), mass = 5f, note = Note(ObjectId(), "Note3", "Desc3"))

  graphModel.nodes.addAll(listOf(node1, node2, node3))

  graphModel.edges.add(Edge(0, 1)) // Connect node1 and node2
  graphModel.edges.add(Edge(1, 2)) // Connect node2 and node3
 }


 @Test
 fun testInitDimensions() {
  graphModel.initDimensions(800f, 600f)

  // Ensure the width and height were updated
  assertEquals(800f, graphModel.w)
  assertEquals(600f, graphModel.h)

  // Check that all nodes have new positions within the screen bounds
  graphModel.nodes.forEach { node ->
   assertTrue(node.pos.x in -16000f..16000f)
   assertTrue(node.pos.y in -12000f..12000f)
  }
 }

 @Test
 fun testApplyForces() {
  graphModel.applyForces()

  // Ensure that forces have been applied to nodes
  graphModel.nodes.forEach { node ->
   assertNotEquals(Vec(0f, 0f), node.force) // Forces should be non-zero
  }
 }

 @Test
 fun testOnMouseMove() {
  graphModel.onMouseMove(500f, 300f)

  // Check that the mouse position updates
  assertEquals(Vec(500f, 300f), graphModel.mousePos)
 }

 @Test
 fun testOnPress() {
  graphModel.onMouseMove(150f, 150f)
  graphModel.onPress()

  // Ensure a node was selected
  assertTrue(graphModel.closeNodeIndex in graphModel.nodes.indices)
  assertTrue(graphModel.mouseClicked)
 }

 @Test
 fun testOnRelease() {
  graphModel.onRelease()

  // Ensure dragging is disabled
  assertFalse(graphModel.mouseClicked)
  assertEquals(0.2f, graphModel.lerpValue)
 }







}

