package individual_board.model
import boards.entities.Board
import individual_board.entities.Note
import individual_board.entities.addNote
import individual_board.entities.removeNote
//import individual_board.entities.Section
import org.bson.types.ObjectId
import article.entities.*
import shared.ConnectionManager
import shared.IPublisher
import shared.boardModel
import shared.persistence.IPersistence


class IndvBoardModel(val persistence: IPersistence) : IPublisher() {
    // maps board ID to list of notes
    var noteDict = mutableMapOf<ObjectId, MutableList<Note>>()

    init {
        persistence.connect()
        if (ConnectionManager.isConnected) {
            noteDict = persistence.readNotes()
            notifySubscribers()
        }
    }

    fun initialize() {
        // Called when there is a reconnection
        if (ConnectionManager.isConnected) {
            noteDict = persistence.readNotes()
            notifySubscribers()
        }
    }

    /*
    For all the functions below, first modify local data structures, then do same on DB and then notifySubscribers()
    */

    fun addNote(note: Note, board: Board) {
        noteDict[board.id]?.add(note)
        board.notes += note.id

        if (ConnectionManager.isConnected){
            persistence.addNote(board,note)
        }

        notifySubscribers()
    }

    fun updateNote(note: Note, boardId: ObjectId?, title: String, desc: String){
        noteDict[boardId]?.let { notes ->
            val index = notes.indexOfFirst {it.id == note.id }
            if (index != -1) {
                val updatedSection = (notes[index] as Note).copy(title = title, desc = desc)
                notes[index] = updatedSection
            }
        }

        if (ConnectionManager.isConnected) {
            persistence.updateNote(note.id, title, desc)
        }

        notifySubscribers()
    }

    fun del(note: Note, board: Board) {
        noteDict[board.id]?.removeNote(note)
        board.notes = board.notes.filterNot { it == note.id } // delete from the board's notes

        if (ConnectionManager.isConnected) {
            persistence.deleteNote(note.id, board.id)
        }

        notifySubscribers()
    }

}
