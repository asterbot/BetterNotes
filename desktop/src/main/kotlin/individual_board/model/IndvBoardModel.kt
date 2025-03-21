package individual_board.model
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import boards.entities.Board
import graph_ui.Edge
import graph_ui.Node
import graph_ui.Vec
import individual_board.entities.Note
import individual_board.entities.removeNote
import kotlinx.coroutines.delay
import org.bson.types.ObjectId
import shared.ConnectionManager
import shared.IPublisher
import shared.articleModel
import shared.dbQueue
import shared.persistence.Create
import shared.persistence.Delete
import shared.persistence.IPersistence
import shared.persistence.Update
import java.time.Instant
import kotlin.random.Random


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
        persistence.connect()
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

        if (note.type == "article") {
            articleModel.contentBlockDict[note.id]= mutableListOf()
        }

        if (ConnectionManager.isConnected){
            persistence.addNote(board,note)
        }
        else{
            dbQueue.addToQueue(Create(persistence, note, boardDependency = board))
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
        else{
            dbQueue.addToQueue(Update(persistence, note, mutableMapOf("title" to title, "desc" to desc)))
        }

        notifySubscribers()
    }

    fun updateNoteAccessed(note: Note, board: Board) {
        note.datetimeAccessed = Instant.now().toString()
        if (ConnectionManager.isConnected) {
            persistence.updateNoteAccessed(note.id, board.id)
        }
    }

    fun del(note: Note, board: Board) {
        noteDict[board.id]?.removeNote(note)
        board.notes = board.notes.filterNot { it == note.id } // delete from the board's notes

        if (ConnectionManager.isConnected) {
            persistence.deleteNote(note.id, board.id)
        }
        else{
            dbQueue.addToQueue(Delete(persistence, note, boardDependency = board))
        }

        notifySubscribers()
    }

}
