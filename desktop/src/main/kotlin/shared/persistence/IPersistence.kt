package shared.persistence

import article.entities.ContentBlock
import boards.entities.Board
import individual_board.entities.Note
import login.entities.User
import org.bson.types.ObjectId

interface IPersistence {
    // Connects and returns whether it was successful
    fun connect(): Boolean

    // Pings DB to check if connection is active or not, and updates global state
    suspend fun pingDB(): Boolean

    // Users
    fun addUser(user: User): Boolean
    fun authenticate(username: String, password: String): Boolean
    fun updatePassword(oldPassword: String, newPassword: String): Boolean
    fun deleteUser(password: String): Boolean

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
    fun updateNote(noteId: ObjectId, title: String, desc: String, relatedNotes: List<ObjectId>, tagColor: String, await: Boolean = false)
    fun updateNoteAccessed(noteId: ObjectId, boardId: ObjectId, await: Boolean = false)

    // ContentBlocks: TODO
    fun updateGlueStatus(
        contentBlockId: ObjectId,
        gluedAbove: Boolean,
        gluedBelow: Boolean,
        articleId: ObjectId,
        boardId: ObjectId,
        await: Boolean = false
    )

    fun readContentBlocks(): MutableMap<ObjectId, MutableList<ContentBlock>>
    fun insertContentBlock(
        article: Note,
        contentBlock: ContentBlock,
        index: Int,
        boardId: ObjectId,
        await: Boolean = false
    ) // insert to index

    fun addContentBlock(
        article: Note,
        contentBlock: ContentBlock,
        boardId: ObjectId,
        await: Boolean = false
    ) // add to end

    fun swapContentBlocks(
        articleId: ObjectId, upperBlockStart: Int, upperBlockEnd: Int,
        lowerBlockStart: Int, lowerBlockEnd: Int, boardId: ObjectId, await: Boolean = false
    )

    fun deleteContentBlock(articleId: ObjectId, contentBlockId: ObjectId, boardId: ObjectId, await: Boolean = false)
    fun updateContentBlock(
        block: ContentBlock, text: String, canvasHeight: Int,
        bList: MutableList<Byte>, language: String, gluedAbove: Boolean, gluedBelow: Boolean,
        article: Note, boardId: ObjectId, await: Boolean = false
    )
}