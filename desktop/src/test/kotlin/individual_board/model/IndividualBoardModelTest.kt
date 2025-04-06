package individual_board.model

import boards.entities.Board
import boards.model.BoardModel
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import shared.persistence.DBStorage
import shared.persistence.IPersistence
import kotlin.test.*
import individual_board.entities.*
import java.time.Instant

class IndividualBoardModelTest() {
    lateinit var mockDB: IPersistence
    lateinit var boardModel: BoardModel
    lateinit var board: Board
    lateinit var indvBoardModel: IndvBoardModel

    @BeforeTest
    fun setup() {
        mockDB = DBStorage("cs346-test-db")
        indvBoardModel = IndvBoardModel(mockDB)

        // Clear the DB before starting
        runBlocking {
            mockDB.clearDB()
        }

        board = Board(ObjectId(), name="name1", desc="desc2")
        mockDB.addBoard(board)

        board.notes = mutableListOf()

        indvBoardModel.noteDict[board.id] = mutableListOf()
    }

    @Test
    fun add() {
        val oldCount = indvBoardModel.noteDict[board.id]?.size ?: 0

        val note = Note(
            id = ObjectId(),
            title = "Test Note",
            desc = "Note description",
            relatedNotes = listOf(),
            tag = "blue",
            datetimeCreated = Instant.now().toString(),
            datetimeUpdated = Instant.now().toString(),
            datetimeAccessed = Instant.now().toString()
        )

        indvBoardModel.addNote(note, board)

        val notes = indvBoardModel.noteDict[board.id]
        
        assertEquals(oldCount + 1, notes?.size)
        assertNotNull(notes)
        assertTrue(notes.any { it.id == note.id })
        assertTrue(board.notes.contains(note.id))
    }

    @Test
    fun delete() {
        val note1 = Note(
            id = ObjectId(),
            title = "Note 1",
            desc = "First note",
            relatedNotes = listOf(),
            tag = "orange",
            datetimeCreated = "2021-01-01T00:00:00Z",
            datetimeUpdated = "2021-01-01T00:00:00Z",
            datetimeAccessed = "2021-01-01T00:00:00Z"
        )

        val note2 = Note(
            id = ObjectId(),
            title = "Note 2",
            desc = "Second note",
            relatedNotes = listOf(note1.id),
            tag = "pink",
            datetimeCreated = "2021-01-02T00:00:00Z",
            datetimeUpdated = "2021-01-02T00:00:00Z",
            datetimeAccessed = "2021-01-02T00:00:00Z"
        )

        val note1WithRelation = note1.copy(relatedNotes = listOf(note2.id))

        indvBoardModel.noteDict[board.id] = mutableListOf(note1WithRelation, note2)
        board.notes = mutableListOf(note1.id, note2.id)

        indvBoardModel.del(note1WithRelation, board)

        val notesAfterDeletion = indvBoardModel.noteDict[board.id]
        assertNotNull(notesAfterDeletion)
        assertFalse(notesAfterDeletion.any { it.id == note1.id })
        assertFalse(board.notes.contains(note1.id))

        val remainingNote = notesAfterDeletion.find { it.id == note2.id }
        assertNotNull(remainingNote)
        assertFalse(remainingNote!!.relatedNotes.contains(note1.id))
    }

    @Test
    fun update() {
        val note = Note(
            id = ObjectId(),
            title = "Accessed Note",
            desc = "Testing update note accessed",
            relatedNotes = listOf(),
            tag = "purple",
            datetimeCreated = Instant.now().toString(),
            datetimeUpdated = Instant.now().toString(),
            datetimeAccessed = "2021-01-01T00:00:00Z"
        )
        indvBoardModel.noteDict[board.id] = mutableListOf(note)
        val oldAccessed = note.datetimeAccessed
        indvBoardModel.updateNoteAccessed(note, board)
        val updatedNote = indvBoardModel.noteDict[board.id]?.find { it.id == note.id }
        assertNotNull(updatedNote)
        assertNotEquals(oldAccessed, updatedNote.datetimeAccessed)
    }

    @Test
    fun sort() {
        val noteA = Note(
            id = ObjectId(),
            title = "a",
            desc = "first",
            relatedNotes = listOf(),
            tag = "tag1",
            datetimeCreated = "2021-01-01T00:00:00Z",
            datetimeUpdated = "2021-01-01T00:00:00Z",
            datetimeAccessed = "2021-01-01T00:00:00Z"
        )
        val noteB = Note(
            id = ObjectId(),
            title = "b",
            desc = "second",
            relatedNotes = listOf(),
            tag = "tag2",
            datetimeCreated = "2021-01-02T00:00:00Z",
            datetimeUpdated = "2021-01-02T00:00:00Z",
            datetimeAccessed = "2021-01-03T00:00:00Z"
        )
        val noteC = Note(
            id = ObjectId(),
            title = "c",
            desc = "third",
            relatedNotes = listOf(),
            tag = "tag3",
            datetimeCreated = "2021-01-03T00:00:00Z",
            datetimeUpdated = "2021-01-03T00:00:00Z",
            datetimeAccessed = "2021-01-02T00:00:00Z"
        )

        // Add the notes in an unsorted order.
        indvBoardModel.noteDict[board.id] = mutableListOf(noteB, noteC, noteA)

        // Sort by Title (alphabetical, case-insensitive).
        indvBoardModel.sortByTitle(board.id)
        val sortedByTitle = indvBoardModel.noteDict[board.id]!!
        assertEquals("a", sortedByTitle[0].title.lowercase())
        assertEquals("b", sortedByTitle[1].title.lowercase())
        assertEquals("c", sortedByTitle[2].title.lowercase())

        // Sort by Last Created (descending).
        indvBoardModel.sortByDatetimeCreated(board.id)
        val sortedByCreated = indvBoardModel.noteDict[board.id]!!
        assertEquals("2021-01-03T00:00:00Z", sortedByCreated[0].datetimeCreated)
        assertEquals("2021-01-02T00:00:00Z", sortedByCreated[1].datetimeCreated)
        assertEquals("2021-01-01T00:00:00Z", sortedByCreated[2].datetimeCreated)

        // Sort by Last Updated (descending).
        indvBoardModel.sortByDatetimeUpdated(board.id)
        val sortedByUpdated = indvBoardModel.noteDict[board.id]!!
        assertEquals("2021-01-03T00:00:00Z", sortedByUpdated[0].datetimeUpdated)
        assertEquals("2021-01-02T00:00:00Z", sortedByUpdated[1].datetimeUpdated)
        assertEquals("2021-01-01T00:00:00Z", sortedByUpdated[2].datetimeUpdated)

        // Sort by Last Accessed (descending).
        indvBoardModel.sortByDatetimeAccessed(board.id)
        val sortedByAccessed = indvBoardModel.noteDict[board.id]!!
        assertEquals("2021-01-03T00:00:00Z", sortedByAccessed[0].datetimeAccessed)
        assertEquals("2021-01-02T00:00:00Z", sortedByAccessed[1].datetimeAccessed)
        assertEquals("2021-01-01T00:00:00Z", sortedByAccessed[2].datetimeAccessed)
    }
}