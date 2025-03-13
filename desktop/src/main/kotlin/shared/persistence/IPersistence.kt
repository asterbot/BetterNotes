package shared.persistence

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

    // Notes
    fun readNotes(): MutableMap<ObjectId, MutableList<Note>>
    fun addNote(board: Board, note: Note)
    fun deleteNote(noteId: ObjectId, boardId: ObjectId)
    fun updateNote(noteId: ObjectId, title: String, desc: String)

    // ContentBlocks: TODO

}
