package individual_board.entities

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

/* Articles */

class Article(
    id: Int = 0,
    title: String,
    desc: String,
    parentNotes: MutableList<Note>? = null,
    relatedNotes: MutableList<Note>? = null,
    var contentBlocks: MutableList<ContentBlock>
) : Note(id, title, desc, parentNotes, relatedNotes)

// Content Blocks

sealed class ContentBlock {
    abstract val type: String
}

data class MarkdownBlock (
    // Boilerplate
    val text: String
) : ContentBlock() {
    override val type = "markdown"
}

data class CodeBlock (
    // Boilerplate
    val code: String,
    val language: String? = null
) : ContentBlock() {
    override val type = "code"
}

fun MutableList<ContentBlock>.addContentBlock(element: ContentBlock): Boolean {
    this.add(element)
    return true
}

fun MutableList<ContentBlock>.removeContentBlock(element: ContentBlock): Boolean {
    this.remove(element)
    return true
}

private fun MutableList<Note>.reindex() {
    var count = 0
    for (note in this) {
        note.id = count++
    }
}