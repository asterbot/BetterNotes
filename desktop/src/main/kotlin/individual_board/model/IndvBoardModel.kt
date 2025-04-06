package individual_board.model
import boards.entities.Board
import individual_board.entities.Note
import individual_board.entities.removeNote
import org.bson.types.ObjectId
import shared.ConnectionManager
import shared.IPublisher
import shared.articleModel
import shared.dbQueue
import shared.persistence.Create
import shared.persistence.Delete
import shared.persistence.IPersistence
import shared.persistence.Update
import java.awt.Color
import java.time.Instant

class IndvBoardModel(val persistence: IPersistence) : IPublisher() {
    // maps board ID to list of notes
    var noteDict = mutableMapOf<ObjectId, MutableList<Note>>()

//    var currentTags = mutableListOf<String>() // name of tags
//    var tagsMap = mutableMapOf<String, Color>() // maps tags to colors

    var currentSortType: String = "Last Accessed"
    var currentIsReversed: Boolean = false

    private fun sortNoteList(noteList: MutableList<Note>) {
        when (currentSortType) {
            "Title" -> noteList.sortBy { (it.title).toString().lowercase() }
            "Last Created" -> noteList.sortByDescending { it.datetimeCreated }
            "Last Updated" -> noteList.sortByDescending { it.datetimeUpdated }
            "Last Accessed" -> noteList.sortByDescending { it.datetimeAccessed }
            else -> noteList.sortBy { it.title }
        }
        if (currentIsReversed) noteList.reverse()
    }

    // Public functions to sort notes for a given board.
    fun sortByTitle(boardId: ObjectId, reverse: Boolean = false) {
        currentSortType = "Title"
        currentIsReversed = reverse
        noteDict[boardId]?.let { notes ->
            sortNoteList(notes)
            notifySubscribers()
        }
    }

    fun sortByDatetimeCreated(boardId: ObjectId, reverse: Boolean = false) {
        currentSortType = "Last Created"
        currentIsReversed = reverse
        noteDict[boardId]?.let { notes ->
            sortNoteList(notes)
            notifySubscribers()
        }
    }

    fun sortByDatetimeUpdated(boardId: ObjectId, reverse: Boolean = false) {
        currentSortType = "Last Updated"
        currentIsReversed = reverse
        noteDict[boardId]?.let { notes ->
            sortNoteList(notes)
            notifySubscribers()
        }
    }

    fun sortByDatetimeAccessed(boardId: ObjectId, reverse: Boolean = false) {
        currentSortType = "Last Accessed"
        currentIsReversed = reverse
        noteDict[boardId]?.let { notes ->
            sortNoteList(notes)
            notifySubscribers()
        }
    }

    init {
        persistence.connect()
    }

    fun initialize() {
        // Called when there is a reconnection and/or login
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

        articleModel.contentBlockDict[note.id]= mutableListOf()

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
                        persistence.updateNote(updated.id, updated.title, updated.desc, updated.relatedNotes, tagColor = updated.tag)
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

        noteDict[board.id]?.let { notes ->
            sortNoteList(notes)
        }

        notifySubscribers()
    }

    fun updateNote(note: Note, boardId: ObjectId?, title: String, desc: String, relatedNotes: List<ObjectId>, tagColor: String) {

        val oldRelated = note.relatedNotes

        // Update the note itself in local list
        noteDict[boardId]?.let { notes ->
            val index = notes.indexOfFirst { it.id == note.id }
            if (index != -1) {
                val updatedNote = note.copy(title = title, desc = desc, relatedNotes = relatedNotes, tag = tagColor ,datetimeUpdated = Instant.now().toString(), datetimeAccessed = Instant.now().toString())
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
                        persistence.updateNote(relatedId, updated.title, updated.desc, updated.relatedNotes, tagColor)
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
                        persistence.updateNote(removedId, updated.title, updated.desc, updated.relatedNotes, tagColor)
                    } else {
                        dbQueue.addToQueue(Update(persistence, it, mutableMapOf("relatedNotes" to updated.relatedNotes)))
                    }
                }
            }
        }

        // Persist main note
        if (ConnectionManager.isConnected) {
            persistence.updateNote(note.id, title, desc, relatedNotes, tagColor)
        } else {
            dbQueue.addToQueue(Update(persistence, note, mutableMapOf("title" to title, "desc" to desc, "relatedNotes" to relatedNotes, "tag" to tagColor)))
        }

        noteDict[boardId]?.let { notes ->
            sortNoteList(notes)
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
                        persistence.updateNote(updated.id, updated.title, updated.desc, updated.relatedNotes, updated.tag)
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

        noteDict[board.id]?.let { notes ->
            sortNoteList(notes)
        }

        notifySubscribers()
    }

}
