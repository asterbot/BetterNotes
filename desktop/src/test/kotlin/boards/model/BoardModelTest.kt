package boards.model
import androidx.compose.ui.graphics.Path
import article.entities.ContentBlock
import kotlin.test.*
import boards.entities.*
import individual_board.entities.Note
import kotlinx.coroutines.runBlocking
import login.entities.User
import org.bson.types.ObjectId
import shared.persistence.DBStorage
import shared.persistence.IPersistence

class BoardModelTest() {
    lateinit var mockDB: IPersistence
    lateinit var boardModel: BoardModel
    lateinit var board: Board

    @BeforeTest
    fun setup() {
        mockDB = DBStorage("cs346-test-db")

        boardModel = BoardModel(mockDB)

        // Clear the DB before starting
        runBlocking {
            mockDB.clearDB()
        }

        // Add sample data
        val board1 = Board(ObjectId(), name="name1", desc="desc2")
        val board2 = Board(ObjectId(), name="name2", desc="desc2")
        boardModel.boardList = mutableListOf(
            board1,
            board2,
        )

        mockDB.addBoard(board1)
        mockDB.addBoard(board2)

        // New board to test with
        board = Board(ObjectId(), name="name3", desc="desc3")
    }

    @Test
    fun add() {
        val oldCount = boardModel.boardList.size
        boardModel.add(board)
        assertEquals(oldCount + 1, boardModel.boardList.size)
        assertEquals(oldCount + 1, mockDB.readBoards().size)
    }

    @Test
    fun del() {
        boardModel.add(board)
        val oldCount = boardModel.boardList.size
        boardModel.del(board)
        assertEquals(oldCount - 1, boardModel.boardList.size)
        assertEquals(oldCount - 1, mockDB.readBoards().size)
    }

    @Test
    fun update() {
        boardModel.add(board)
        val oldCount = boardModel.boardList.size
        boardModel.update(board, "newName", "newDesc")
        assertEquals(oldCount, boardModel.boardList.size)
        assertEquals("newName", boardModel.boardList[2].name)
        assertEquals("newDesc", boardModel.boardList[2].desc)
        assertEquals("newName", mockDB.readBoards()[2].name)
        assertEquals("newDesc", mockDB.readBoards()[2].desc)
    }

    @Test
    fun sort() {
        val board1 = Board(ObjectId(), name="name1", desc="desc2")
        val board2 = Board(ObjectId(), name="name2", desc="desc2")
        boardModel.add(board1)
        boardModel.add(board2)

        boardModel.sortByTitle(false)

        assertEquals(boardModel.boardList[0].name, "name1")

        boardModel.sortByTitle(true)
        assertEquals(boardModel.boardList[0].name, "name2")
    }
}
