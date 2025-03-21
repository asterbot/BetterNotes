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

        note.relatedNotes.forEach { relatedId ->
            val relatedNote = noteDict[board.id]?.find { it.id == relatedId }
            relatedNote?.let {
                if (!it.relatedNotes.contains(note.id)) {
                    val updated = it.copy(relatedNotes = it.relatedNotes + note.id)
                    val idx = noteDict[board.id]?.indexOfFirst { n -> n.id == relatedId }
                    if (idx != null && idx != -1) {
                        noteDict[board.id]?.set(idx, updated)
                    }

                    if (ConnectionManager.isConnected) {
                        persistence.updateNote(updated.id, updated.title, updated.desc, updated.relatedNotes)
                    } else {
                        dbQueue.addToQueue(Update(persistence, it, mutableMapOf("relatedNotes" to updated.relatedNotes)))
                    }
                }
            }
        }

        if (ConnectionManager.isConnected){
            persistence.addNote(board,note)
        }
        else{
            dbQueue.addToQueue(Create(persistence, note, boardDependency = board))
        }

        notifySubscribers()
    }

    fun updateNote(note: Note, boardId: ObjectId?, title: String, desc: String, relatedNotes: List<ObjectId>) {
        val oldRelated = note.relatedNotes

        // Update the note itself in local list
        noteDict[boardId]?.let { notes ->
            val index = notes.indexOfFirst { it.id == note.id }
            if (index != -1) {
                val updatedNote = note.copy(title = title, desc = desc, relatedNotes = relatedNotes)
                notes[index] = updatedNote
            }
        }

        // Ensure bidirectional relationships
        // First: Add this note's ID to new related notes
        for (relatedId in relatedNotes) {
            val relatedNote = noteDict[boardId]?.find { it.id == relatedId }
            relatedNote?.let {
                if (!it.relatedNotes.contains(note.id)) {
                    val updated = it.copy(relatedNotes = it.relatedNotes + note.id)
                    // Update the note in the list
                    val idx = noteDict[boardId]?.indexOfFirst { n -> n.id == relatedId }
                    if (idx != null && idx != -1) {
                        noteDict[boardId]?.set(idx, updated)
                    }

                    // Persist the update if online
                    if (ConnectionManager.isConnected) {
                        persistence.updateNote(relatedId, updated.title, updated.desc, updated.relatedNotes)
                    } else {
                        dbQueue.addToQueue(Update(persistence, it, mutableMapOf("relatedNotes" to updated.relatedNotes)))
                    }
                }
            }
        }

        // Second: Remove this note's ID from notes no longer related
        val removedRelations = oldRelated.filterNot { relatedNotes.contains(it) }
        for (removedId in removedRelations) {
            val removedNote = noteDict[boardId]?.find { it.id == removedId }
            removedNote?.let {
                if (it.relatedNotes.contains(note.id)) {
                    val updated = it.copy(relatedNotes = it.relatedNotes - note.id)
                    val idx = noteDict[boardId]?.indexOfFirst { n -> n.id == removedId }
                    if (idx != null && idx != -1) {
                        noteDict[boardId]?.set(idx, updated)
                    }

                    if (ConnectionManager.isConnected) {
                        persistence.updateNote(removedId, updated.title, updated.desc, updated.relatedNotes)
                    } else {
                        dbQueue.addToQueue(Update(persistence, it, mutableMapOf("relatedNotes" to updated.relatedNotes)))
                    }
                }
            }
        }

        // Persist main note
        if (ConnectionManager.isConnected) {
            persistence.updateNote(note.id, title, desc, relatedNotes)
        } else {
            dbQueue.addToQueue(Update(persistence, note, mutableMapOf("title" to title, "desc" to desc, "relatedNotes" to relatedNotes)))
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
        note.relatedNotes.forEach { relatedId ->
            val relatedNote = noteDict[board.id]?.find { it.id == relatedId }
            relatedNote?.let {
                if (it.relatedNotes.contains(note.id)) {
                    val updated = it.copy(relatedNotes = it.relatedNotes - note.id)
                    val idx = noteDict[board.id]?.indexOfFirst { n -> n.id == relatedId }
                    if (idx != null && idx != -1) {
                        noteDict[board.id]?.set(idx, updated)
                    }

                    if (ConnectionManager.isConnected) {
                        persistence.updateNote(updated.id, updated.title, updated.desc, updated.relatedNotes)
                    } else {
                        dbQueue.addToQueue(Update(persistence, it, mutableMapOf("relatedNotes" to updated.relatedNotes)))
                    }
                }
            }
        }

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
