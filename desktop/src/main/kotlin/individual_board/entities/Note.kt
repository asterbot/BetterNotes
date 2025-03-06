package individual_board.entities

import java.util.*

/* Notes */

open class Note(

    var id: Int = 0,
    var title: String,
    var desc: String? = "",
    var parentNotes: MutableList<Note>? = null,
    var relatedNotes: MutableList<Note>? = null
)

fun MutableList<Note>.addNote(element: Note): Boolean {
    this.add(element)
    this.reindex()
    return true
}

fun MutableList<Note>.removeNote(element: Note): Boolean {
    this.remove(element)
    this.reindex()
    return true
}

/* Sections */

class Section(
    id: Int = 0,
    title: String,
    desc: String,
    parentNotes: MutableList<Note>? = null,
    relatedNotes: MutableList<Note>? = null,
//    childrenNotes: MutableList<Note>? = null // to be implemented (some circular dependency things)
) : Note(id, title, desc, parentNotes, relatedNotes)



private fun MutableList<Note>.reindex() {
    var count = 0
    for (note in this) {
        note.id = count++
    }
}