package shared.persistence

import androidx.compose.ui.graphics.Path
import article.entities.ContentBlock
import boards.entities.Board
import individual_board.entities.Note
import org.bson.types.ObjectId

interface IPersistence {
    // Connects and returns whether it was successful
    fun connect(): Boolean
    // Pings DB to check if connection is active or not, and updates global state
    suspend fun pingDB(): Boolean

    // Boards
    fun readBoards(): List<Board>
    fun addBoard(board: Board)
    fun deleteBoard(boardId: ObjectId, noteIds: List<ObjectId>)
    fun updateBoard(boardId: ObjectId, name: String, desc: String, notes: List<ObjectId>)
    fun updateBoardAccessed(boardId: ObjectId)

    // Notes
    fun readNotes(): MutableMap<ObjectId, MutableList<Note>>
    fun addNote(board: Board, note: Note, await: Boolean = false)
    fun deleteNote(noteId: ObjectId, boardId: ObjectId, await: Boolean = false)
    fun updateNote(noteId: ObjectId, title: String, desc: String, relatedNotes: List<ObjectId>, await: Boolean = false)
    fun updateNoteAccessed(noteId: ObjectId, boardId: ObjectId, await: Boolean = false)

    // ContentBlocks: TODO
    fun readContentBlocks(): MutableMap<ObjectId, MutableList<ContentBlock>>
    fun insertContentBlock(article: Note, contentBlock: ContentBlock, index: Int, boardId: ObjectId, await: Boolean = false) // insert to index
    fun addContentBlock(article: Note, contentBlock: ContentBlock, boardId: ObjectId, await: Boolean = false) // add to end
    fun swapContentBlocks(articleId: ObjectId, index1: Int, index2: Int, boardId: ObjectId, await: Boolean = false)
    fun deleteContentBlock(articleId: ObjectId, contentBlockId: ObjectId, boardId: ObjectId, await: Boolean = false)
    fun updateContentBlock(block: ContentBlock, text: String, pathsContent: MutableList<Path>, language:String, article: Note, boardId: ObjectId, await: Boolean = false)

}
