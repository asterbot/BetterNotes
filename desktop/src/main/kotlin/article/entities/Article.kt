package article.entities

import individual_board.entities.Note
import java.util.*

/* Articles */

class Article(
    id: Int = 0,
    title: String,
    desc: String,
    parentNotes: MutableList<Note>? = null,
    relatedNotes: MutableList<Note>? = null,
    var contentBlocks: MutableList<ContentBlock> = mutableListOf()
) : Note(id, title, desc, parentNotes, relatedNotes)

// Content Blocks

enum class BlockType(
    val createDefaultBlock: () -> ContentBlock
    ) {
    PLAINTEXT(
        { TextBlock() }
    ),
    MARKDOWN(
        { MarkdownBlock() }
    ),
    CODE(
        { CodeBlock() }
    )
}

sealed class ContentBlock {
    abstract val type: BlockType
    var id: UUID = UUID.randomUUID()
    abstract fun copyBlock(): ContentBlock
}

data class TextBlock (
    // Boilerplate
    var text: String = ""
) : ContentBlock() {
    override val type = BlockType.PLAINTEXT
    override fun copyBlock(): ContentBlock {
        return TextBlock(text).apply {
            this.id = UUID.randomUUID()  // Assign new ID
        }
    }
}

data class MarkdownBlock (
    // Boilerplate
    val text: String = ""
) : ContentBlock() {
    override val type = BlockType.MARKDOWN
    override fun copyBlock(): ContentBlock {
        return MarkdownBlock(text).apply {
            this.id = UUID.randomUUID()  // Assign new ID
        }
    }
}

data class CodeBlock (
    // Boilerplate
    val code: String = "",
    val language: String? = null
) : ContentBlock() {
    override val type = BlockType.CODE
    override fun copyBlock(): ContentBlock {
        return CodeBlock(code, language).apply {
            this.id = UUID.randomUUID()  // Assign new ID
        }
    }
}

fun MutableList<ContentBlock>.addContentBlock(element: ContentBlock): Boolean {
    this.add(element)
    return true
}

fun MutableList<ContentBlock>.removeContentBlock(element: ContentBlock): Boolean {
    this.remove(element)
    return true
}

//private fun MutableList<Note>.reindex() {
//    var count = 0
//    for (note in this) {
//        note.id = count++
//    }
//}