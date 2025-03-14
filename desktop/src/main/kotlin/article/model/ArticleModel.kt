package article.model
import androidx.compose.ui.graphics.Path
import article.entities.*
import org.bson.types.ObjectId
import shared.ConnectionManager
import shared.IPublisher
import shared.persistence.IPersistence
import java.awt.Canvas
import javax.swing.text.StringContent

// NOTE: should pass in board probably
class ArticleModel(val persistence: IPersistence) : IPublisher() {
    var contentBlocks = mutableListOf<ContentBlock>()
    // maps Article ID to list of content blocks
    var contentBlockDict = mutableMapOf<ObjectId, MutableList<ContentBlock>>()

    init {
        persistence.connect()
        if (ConnectionManager.isConnected){
            // TODO: Load data from DB
        }
        println("DEBUG: initialized ArticleModel")
    }

    fun addBlock(index: Int, type: BlockType) {
        println("DEBUG: inserting empty block at index $index (attempt)")
        // index is a valid value, insert as normal
        if (index in 0..(contentBlocks.size - 1)) {
            contentBlocks.add(index, type.createDefaultBlock())
            println("DEBUG: inserted block at index $index")
            notifySubscribers()
        }
        // special case where we insert downwards (index out of range of existing block array, so append instead)
        else if (index == contentBlocks.size) {
            contentBlocks.add(type.createDefaultBlock())
            println("DEBUG: inserted block at index $index")
            notifySubscribers()
        }
    }

    fun duplicateBlock(index: Int) {
        println("DEBUG: duplicating block at index $index (attempt)")
        if (index in 0..(contentBlocks.size - 1)) {
            val dupBlock: ContentBlock = contentBlocks[index].copyBlock()
            contentBlocks.add(index + 1, dupBlock)
            println("DEBUG: duplicated block at index $index")
            notifySubscribers()
        }
    }

    fun moveBlockUp(index: Int) {
        println("DEBUG: moving up block at index $index (attempt)")
        if (index in 1..(contentBlocks.size - 1)) {
            val temp = contentBlocks[index]
            contentBlocks[index] = contentBlocks[index - 1]
            contentBlocks[index - 1] = temp
            println("DEBUG: swapped blocks with indices $index and ${index-1}")
            notifySubscribers()
        }
    }

    fun moveBlockDown(index: Int) {
        println("DEBUG: moving down block at index $index (attempt)")
        if (index in 0..(contentBlocks.size - 2)) {
            val temp = contentBlocks[index]
            contentBlocks[index] = contentBlocks[index + 1]
            contentBlocks[index + 1] = temp
            println("DEBUG: swapped blocks with indices $index and ${index+1}")
            notifySubscribers()
        }
    }

    fun deleteBlock(index: Int) {
        println("DEBUG: deleting block at index $index (attempt)")
        if (index in 0..(contentBlocks.size - 1)) {
            contentBlocks.removeAt(index)
            println("DEBUG: deleted block at index $index")
            notifySubscribers()
        }
    }

    // TODO: later (expand to other ContentBlock types)
    fun saveBlock(index: Int, stringContent: String = "", pathsContent: MutableList<Path> = mutableListOf()) {
        if (index in 0..(contentBlocks.size - 1)) {
            if (contentBlocks[index] is TextBlock) {
                (contentBlocks[index] as TextBlock).text = stringContent
            } else if (contentBlocks[index] is MarkdownBlock) {
                (contentBlocks[index] as MarkdownBlock).text = stringContent
            } else if (contentBlocks[index] is CodeBlock) {
                (contentBlocks[index] as CodeBlock).code = stringContent
            } else if (contentBlocks[index] is CanvasBlock) {
                (contentBlocks[index] as CanvasBlock).paths = pathsContent
                // var paths: MutableList<Path> = mutableListOf<Path>()
            } else if (contentBlocks[index] is MathBlock) {
                (contentBlocks[index] as MathBlock).text = stringContent
            }

            notifySubscribers()
        }
    }
}