package individual_board.view

import boards.view.BoardViewModel
import boards.entities.Board
import boards.model.BoardModel
import individual_board.model.IndvBoardModel
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import shared.persistence.DBStorage
import shared.persistence.IPersistence
import kotlin.test.*
import individual_board.entities.Note
import java.time.Instant


class IndividualBoardViewModel {
    lateinit var mockDB: IPersistence
    lateinit var boardModel: BoardModel
    lateinit var indvBoardModel: IndvBoardModel
    lateinit var indvBoardViewModel: IndvBoardViewModel

    @BeforeTest
    fun setUp() {
        mockDB = DBStorage("cs346-test-db")
        boardModel = BoardModel(mockDB)
        indvBoardModel = IndvBoardModel(mockDB)

        runBlocking {
            mockDB.clearDB()
        }

        val board1 = Board(ObjectId(), name="name1", desc="desc2")
        boardModel.boardList = mutableListOf(
            board1,
        )

        mockDB.addBoard(board1)

        indvBoardViewModel = IndvBoardViewModel(indvBoardModel, board1.id)
        indvBoardModel.noteDict[board1.id] = mutableListOf()
    }

    @Test
    fun update() {
        val oldCount = indvBoardViewModel.noteList.size
        val newNote = Note(
            id = ObjectId(),
            title = "Note 1",
            desc = "Description 1",
            tag = "default",
            contentBlocks = mutableListOf(),
            relatedNotes = mutableListOf(),
            datetimeCreated = Instant.now().toString(),
            datetimeUpdated = Instant.now().toString(),
            datetimeAccessed = Instant.now().toString(),
            userId = "dummy-user"
        )
        indvBoardModel.addNote(newNote, boardModel.boardList[0])
        indvBoardViewModel.update()
        assertEquals(oldCount + 1, indvBoardViewModel.noteList.size)
        assertEquals(newNote, indvBoardViewModel.noteList.last())
    }
}