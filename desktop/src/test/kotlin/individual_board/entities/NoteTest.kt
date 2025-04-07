package individual_board.entities
import org.bson.types.ObjectId
import java.time.Instant
import kotlin.test.*


class NoteTest {
    private lateinit var noteList: MutableList<Note>
    private lateinit var note1: Note
    private lateinit var note2: Note

    @BeforeTest
    fun setup() {
        noteList = mutableListOf()
        note1 = Note(
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
        note2 = Note(
            id = ObjectId(),
            title = "Note 2",
            desc = "Description 2",
            tag = "default",
            contentBlocks = mutableListOf(),
            relatedNotes = mutableListOf(),
            datetimeCreated = Instant.now().toString(),
            datetimeUpdated = Instant.now().toString(),
            datetimeAccessed = Instant.now().toString(),
            userId = "dummy-user"
        )
    }

    @Test
    fun removeNoteTest() {
        // add two notes for testing removal
        noteList.add(note1)
        noteList.add(note2)
        val oldCount = noteList.size

        val result = noteList.removeNote(note1)
        assertTrue(result)
        assertEquals(oldCount - 1, noteList.size)
        assertFalse(noteList.contains(note1),)
    }
}