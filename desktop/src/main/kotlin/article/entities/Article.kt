package article.entities

import androidx.compose.ui.graphics.Path
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.codecs.kotlinx.ObjectIdSerializer
import org.bson.types.ObjectId

/* Articles */

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
    ),
    MATH(
        { MathBlock() }
    ),
    MEDIA(
        { MediaBlock() }
    )
}

@Serializable
sealed class ContentBlock {
    abstract val blockType: BlockType
    @SerialName("_id")
    @Contextual abstract var id: ObjectId
    abstract fun copyBlock(): ContentBlock
}

@Serializable
data class TextBlock @OptIn(ExperimentalSerializationApi::class) constructor(
    @SerialName("_id")
    @Serializable(with = ObjectIdSerializer::class) override var id: ObjectId = ObjectId(),
    var text: String = ""
) : ContentBlock() {
    override val blockType = BlockType.PLAINTEXT
    override fun copyBlock(): ContentBlock {
        return TextBlock(text=text).apply {
            this.id = ObjectId()  // Assign new ID
        }
    }
}

@Serializable
data class MarkdownBlock (
    @SerialName("_id")
    @Contextual override var id: ObjectId = ObjectId(),
    var text: String = ""
) : ContentBlock() {
    override val blockType = BlockType.MARKDOWN
    override fun copyBlock(): ContentBlock {
        return MarkdownBlock(text=text).apply {
            this.id = ObjectId()  // Assign new ID
        }
    }
}

@Serializable
data class CodeBlock (
    @SerialName("_id")
    @Contextual override var id: ObjectId = ObjectId(),
    var text: String = "",
    val language: String = "kotlin",
) : ContentBlock() {
    override val blockType = BlockType.CODE
    override fun copyBlock(): ContentBlock {
        return CodeBlock(text=text, language=language).apply {
            this.id = ObjectId()  // Assign new ID
        }
    }
}

@Serializable
data class CanvasBlock (
    @SerialName("_id")
    @Contextual override var id: ObjectId = ObjectId(),
    var paths: MutableList<Path> = mutableListOf<Path>(),
    var height: Int = 200
) : ContentBlock() {
    override val blockType = BlockType.CANVAS
    override fun copyBlock(): ContentBlock {
        return CanvasBlock(paths=paths).apply { this.id = ObjectId() }
    }
}

@Serializable
data class MathBlock(
    @SerialName("_id")
    @Contextual override var id: ObjectId = ObjectId(),
    var text: String = "",
): ContentBlock() {
    override val blockType = BlockType.MATH
    override fun copyBlock(): ContentBlock {
        return MathBlock(text=text).apply {
            this.id = ObjectId()
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


@Serializable
data class MediaBlock(
    @SerialName("_id")
    @Contextual override var id: ObjectId = ObjectId(),
    var bList: MutableList<Byte> = mutableListOf()
): ContentBlock() {
    override val blockType = BlockType.MEDIA
    override fun copyBlock(): ContentBlock {
        return MediaBlock(bList=bList).apply {
            this.id = ObjectId()
        }
    }
}
