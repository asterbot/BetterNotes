package article.model
import article.entities.BlockType
import article.entities.ContentBlock
import article.entities.TextBlock
import shared.IPublisher

// NOTE: should pass in board probably
class ArticleModel() : IPublisher() {
    // NOTE: change to be empty when first init
//    var contentBlocks = mutableListOf<ContentBlock>(
//        TextBlock(
//            text = "Default text 1 :D"
//        ),
//        TextBlock(
//            text = "Default text 2 :P"
//        )
//    )
    var contentBlocks = mutableListOf<ContentBlock>()

    init {
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

    // TODO: later
    fun saveBlock(index: Int, text: String) {
        if (index in 0..(contentBlocks.size - 1)) {
            if (contentBlocks[index] is TextBlock) {
                (contentBlocks[index] as TextBlock).text = text
                notifySubscribers()
            }
        }
    }
}