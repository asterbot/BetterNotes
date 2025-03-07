package article.entities

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
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
) : Note(id, title, desc, parentNotes, relatedNotes) {
    override fun copy(title: String, desc: String): Article {
        return Article(
            id = this.id,
            title = title,
            desc = desc,
            parentNotes = this.parentNotes,
            relatedNotes = this.relatedNotes,
            contentBlocks = this.contentBlocks.map { it.copyBlock() }.toMutableList()
        )
    }
}

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
    ),
    CANVAS(
        { CanvasBlock() }
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
    var text: String = ""
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
    var code: String = "",
    val language: String? = null
) : ContentBlock() {
    override val type = BlockType.CODE
    override fun copyBlock(): ContentBlock {
        return CodeBlock(code, language).apply {
            this.id = UUID.randomUUID()  // Assign new ID
        }
    }
}

data class CanvasBlock (
    var paths: MutableList<Path> = mutableListOf<Path>(),
    var isEraserOn: Boolean = false,

) : ContentBlock() {
    override val type = BlockType.CANVAS
    override fun copyBlock(): ContentBlock {
        return CanvasBlock(paths, isEraserOn).apply { this.id = UUID.randomUUID() }
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