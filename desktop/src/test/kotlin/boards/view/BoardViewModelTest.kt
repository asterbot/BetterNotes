package boards.view
import boards.entities.Board
import boards.model.BoardModel
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import shared.persistence.DBStorage
import shared.persistence.IPersistence
import kotlin.test.*


class BoardViewModelTest() {
    lateinit var mockDB: IPersistence
    lateinit var boardModel: BoardModel
    lateinit var boardViewModel: BoardViewModel

    @BeforeTest
    fun setUp() {
        mockDB = DBStorage("cs346-test-db")

        boardModel = BoardModel(mockDB)

        // Clear the DB before starting
        runBlocking {
            mockDB.clearDB()
        }

        val board1 = Board(ObjectId(), name="name1", desc="desc2")
        val board2 = Board(ObjectId(), name="name2", desc="desc2")
        boardModel.boardList = mutableListOf(
            board1,
            board2,
        )

        mockDB.addBoard(board1)
        mockDB.addBoard(board2)
        boardViewModel = BoardViewModel(boardModel)
    }

    @Test
    fun update() {
        val oldCount = boardViewModel.boardList.size
        val newBoard = Board(ObjectId(), name="name3", desc="desc3")
        boardModel.add(newBoard)
        boardViewModel.update()
        assertEquals(oldCount + 1, boardViewModel.boardList.size)
        assertEquals(newBoard, boardViewModel.boardList.last())
    }
}