package shared.persistence

import androidx.compose.ui.graphics.Path
import article.entities.ContentBlock
import boards.entities.Board
import individual_board.entities.Note
import org.bson.types.ObjectId

interface IPersistence {
    // Connects and returns whether it was successful
    fun connect(): Boolean

    // Boards
    fun readBoards(): List<Board>
    fun addBoard(board: Board)
    fun deleteBoard(boardId: ObjectId, noteIds: List<ObjectId>)
    fun updateBoard(boardId: ObjectId, name: String, desc: String, notes: List<ObjectId>)
    fun updateBoardAccessed(boardId: ObjectId)

    // Notes
    fun readNotes(): MutableMap<ObjectId, MutableList<Note>>
    fun addNote(board: Board, note: Note)
    fun deleteNote(noteId: ObjectId, boardId: ObjectId)
    fun updateNote(noteId: ObjectId, title: String, desc: String)
    fun updateNoteAccessed(noteId: ObjectId, boardId: ObjectId)

    // ContentBlocks: TODO
    fun readContentBlocks(): MutableMap<ObjectId, MutableList<ContentBlock>>
    fun insertContentBlock(article: Note, contentBlock: ContentBlock, index: Int, boardId: ObjectId) // insert to index
    fun addContentBlock(article: Note, contentBlock: ContentBlock, boardId: ObjectId) // add to end
    fun duplicateContentBlock(article: Note, contentBlock: ContentBlock, index: Int, boardId: ObjectId)
    fun swapContentBlocks(article: Note, index1: Int, index2: Int, boardId: ObjectId)
    fun deleteContentBlock(article: Note, contentBlockId: ObjectId, boardId: ObjectId)
    fun updateContentBlock(block: ContentBlock, text: String, pathsContent: MutableList<Path>, language:String, article: Note, boardId: ObjectId)

}
