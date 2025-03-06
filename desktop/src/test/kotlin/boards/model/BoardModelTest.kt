package boards.model
import kotlin.test.*
import boards.entities.*

class BoardModelTest() {
    lateinit var boardModel: BoardModel
    lateinit var board: Board

    @BeforeTest
    fun setup() {
        boardModel = BoardModel()
        boardModel.boardList = mutableListOf(
            Board(id=1, name="name1", desc="desc2"),
            Board(id=2, name="name2", desc="desc2"),
        )
        board = Board(id=3, name="name3", desc="desc3")
    }

    @Test
    fun add() {
        val oldCount = boardModel.boardList.size
        boardModel.add(board)
        assertEquals(oldCount + 1, boardModel.boardList.size)
    }

    @Test
    fun del() {
        boardModel.add(board)
        val oldCount = boardModel.boardList.size
        boardModel.del(board)
        assertEquals(oldCount - 1, boardModel.boardList.size)
    }

    @Test
    fun update() {
        boardModel.add(board)
        val oldCount = boardModel.boardList.size
        boardModel.update(board, "newName", "newDesc")
        assertEquals(oldCount, boardModel.boardList.size)
        assertEquals("newName", boardModel.boardList[2].name)
        assertEquals("newDesc", boardModel.boardList[2].desc)
    }
}