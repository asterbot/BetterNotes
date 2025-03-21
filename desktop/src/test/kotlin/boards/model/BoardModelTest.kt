package boards.model
import androidx.compose.ui.graphics.Path
import article.entities.ContentBlock
import kotlin.test.*
import boards.entities.*
import individual_board.entities.Note
import org.bson.types.ObjectId
import shared.persistence.IPersistence

class BoardModelTest() {
    lateinit var mockDB: IPersistence
    lateinit var boardModel: BoardModel
    lateinit var board: Board

    @BeforeTest
    fun setup() {
        mockDB = object : IPersistence {
            override fun connect(): Boolean {return false}
            override fun readBoards(): List<Board> = mutableListOf()
            override fun addBoard(board: Board) {}
            override fun deleteBoard(boardId: ObjectId, noteIds: List<ObjectId>) {}
            override fun updateBoard(boardId: ObjectId, name: String, desc: String, notes: List<ObjectId>) {}
            override fun updateBoardAccessed(boardId: ObjectId) {}
            override fun readNotes(): MutableMap<ObjectId, MutableList<Note>> {return mutableMapOf()}
            override fun addNote(board: Board, note: Note) {}
            override fun deleteNote(noteId: ObjectId, boardId: ObjectId) {}
            override fun updateNote(noteId: ObjectId, title: String, desc: String) {}
            override fun updateNoteAccessed(noteId: ObjectId, boardId: ObjectId) {}
            override fun readContentBlocks(): MutableMap<ObjectId, MutableList<ContentBlock>> {return mutableMapOf()}
            override fun insertContentBlock(article: Note, contentBlock: ContentBlock, index: Int, boardId: ObjectId) {}
            override fun addContentBlock(article: Note, contentBlock: ContentBlock, boardId: ObjectId) {}
            override fun duplicateContentBlock(article: Note, contentBlock: ContentBlock, index: Int, boardId: ObjectId){}
            override fun swapContentBlocks(article: Note, index1: Int, index2: Int, boardId: ObjectId) {}
            override fun deleteContentBlock(article: Note, contentBlockId: ObjectId, boardId: ObjectId) {}
            override fun updateContentBlock(block: ContentBlock, text: String, pathsContent: MutableList<Path>, language: String, article: Note, boardId: ObjectId) {}
        }

        boardModel = BoardModel(mockDB)
        boardModel.boardList = mutableListOf(
            Board(ObjectId(), name="name1", desc="desc2"),
            Board(ObjectId(), name="name2", desc="desc2"),
        )
        board = Board(ObjectId(), name="name3", desc="desc3")
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