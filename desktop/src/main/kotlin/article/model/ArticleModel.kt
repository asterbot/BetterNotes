package article.model
import individual_board.entities.Note
import individual_board.entities.addNote
import individual_board.entities.removeNote
import individual_board.entities.Section
import article.entities.Article
import article.entities.ContentBlock
import article.entities.MarkdownBlock
import article.entities.TextBlock
import shared.IPublisher

// NOTE: should pass in board probably
class ArticleModel() : IPublisher() {
    // NOTE: change to be empty when first init
    var contentBlocks = mutableListOf<ContentBlock>(
        TextBlock(
            text = "Default text 1 :D"
        ),
        TextBlock(
            text = "Default text 2 :P"
        )
    )

    init {
        println("DEBUG: initialized ArticleModel")
    }

    fun addTextBlock(index: Int = 0) {
        contentBlocks.add(
            index,
            TextBlock(
                text = "New"
            )
        )
        notifySubscribers()
    }

    fun duplicateBlock(index: Int) {
        if (index in 0..(contentBlocks.size - 1)) {
            contentBlocks.add(index + 1, contentBlocks[index].copyBlock())
            notifySubscribers()
        }
    }

    fun moveBlockUp(index: Int) {
        if (index in 1..(contentBlocks.size - 1)) {
            val temp = contentBlocks[index]
            contentBlocks[index] = contentBlocks[index - 1]
            contentBlocks[index - 1] = temp
            notifySubscribers()
        }
    }

    fun moveBlockDown(index: Int) {
        if (index in 0..(contentBlocks.size - 2)) {
            val temp = contentBlocks[index]
            contentBlocks[index] = contentBlocks[index + 1]
            contentBlocks[index + 1] = temp
            notifySubscribers()
        }
    }

    fun deleteBlock(index: Int) {
        if (index in 0..(contentBlocks.size - 1)) {
            contentBlocks.removeAt(index)
            notifySubscribers()
        }
    }

    fun saveTextBlock(index: Int, text: String) {
        if (index in 0..(contentBlocks.size - 1)) {
            if (contentBlocks[index] is TextBlock) {
                (contentBlocks[index] as TextBlock).text = text
                notifySubscribers()
            }
        }
    }

}
