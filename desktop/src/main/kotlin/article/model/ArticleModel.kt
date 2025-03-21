package article.model
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Path
import article.entities.*
import boards.entities.Board
import individual_board.entities.Note
import org.bson.types.ObjectId
import shared.ConnectionManager
import shared.IPublisher
import shared.dbQueue
import shared.persistence.Create
import shared.persistence.Delete
import shared.persistence.IPersistence
import shared.persistence.Update

// TODO: NOTE: should pass in board probably
class ArticleModel(val persistence: IPersistence) : IPublisher() {
    // maps Article ID to list of content blocks in the Article
    var contentBlockDict = mutableMapOf<ObjectId, MutableList<ContentBlock>>()

    init {
        persistence.connect()
        if (ConnectionManager.isConnected){
            contentBlockDict = persistence.readContentBlocks()
            notifySubscribers()
        }
        println("DEBUG: initialized ArticleModel")
    }

    fun initialize() {
        // Called when there is a reconnection
        if (ConnectionManager.isConnected) {
            contentBlockDict = persistence.readContentBlocks()
            notifySubscribers()
        }
    }


    fun addBlock(index: Int, type: BlockType, article: Note, board: Board) {
        println("DEBUG: inserting empty block at index $index (attempt)")

        contentBlockDict[article.id]?.let { contentBlocks ->
            val blockToAdd = type.createDefaultBlock()
            // index is a valid value, insert as normal
            if (index in 0..(contentBlocks.size - 1)) {
                contentBlocks.add(index, blockToAdd)
                println("DEBUG: inserted block at index $index into model")

                if (ConnectionManager.isConnected) {
                    persistence.insertContentBlock(article, blockToAdd, index, board.id)
                }
                else{
                    dbQueue.addToQueue(Create(persistence, blockToAdd,
                        boardDependency = board, noteDependency = article, indexDependency = index))
                }

                notifySubscribers()
            }
            // special case where we insert downwards (index out of range of existing block array, so append instead)
            else if (index == contentBlocks.size) {
                contentBlocks.add(blockToAdd)
                println("DEBUG: inserted block at index $index (from the end) into model")

                if (ConnectionManager.isConnected) {
                    persistence.addContentBlock(article, blockToAdd, board.id)
                }
                else{
                    dbQueue.addToQueue(Create(persistence, blockToAdd,
                        boardDependency = board, noteDependency = article))
                }

                notifySubscribers()
            }
        }
    }

    fun duplicateBlock(index: Int, article: Note, board: Board) {
        println("DEBUG: duplicating block at index $index (attempt)")
        contentBlockDict[article.id]?.let { contentBlocks ->
            if (index in 0..(contentBlocks.size - 1)) {
                val dupBlock: ContentBlock = contentBlocks[index].copyBlock()
                contentBlocks.add(index + 1, dupBlock)
                println("DEBUG: duplicated block at index $index into model")

                if (ConnectionManager.isConnected) {
                    persistence.insertContentBlock(article, dupBlock, index+1, board.id)
                }

                notifySubscribers()
            }
        }
    }

    fun moveBlockUp(index: Int, article: Note, board: Board) {
        println("DEBUG: moving up block at index $index (attempt)")
        contentBlockDict[article.id]?.let { contentBlocks ->
            if (index in 1..(contentBlocks.size - 1)) {
                val temp = contentBlocks[index]
                contentBlocks[index] = contentBlocks[index - 1]
                contentBlocks[index - 1] = temp
                println("DEBUG: swapped blocks with indices $index and ${index - 1} in model")

                if (ConnectionManager.isConnected) {
                    persistence.swapContentBlocks(article.id, index, index-1, board.id)
                }

                notifySubscribers()
            }
        }
    }

    fun moveBlockDown(index: Int, article: Note, board: Board) {
        println("DEBUG: moving down block at index $index (attempt)")
        contentBlockDict[article.id]?.let { contentBlocks ->
            if (index in 0..(contentBlocks.size - 2)) {
                val temp = contentBlocks[index]
                contentBlocks[index] = contentBlocks[index + 1]
                contentBlocks[index + 1] = temp
                println("DEBUG: swapped blocks with indices $index and ${index + 1} in model")

                if (ConnectionManager.isConnected) {
                    persistence.swapContentBlocks(article.id, index, index+1, board.id)
                }

                notifySubscribers()
            }
        }
    }

    fun deleteBlock(index: Int, article: Note, board: Board) {
        println("DEBUG: deleting block at index $index (attempt)")
        contentBlockDict[article.id]?.let { contentBlocks ->
            if (index in 0..(contentBlocks.size - 1)) {
                val toRemove: ContentBlock = contentBlocks[index]
                contentBlocks.removeAt(index)
                println("DEBUG: deleted block at index $index in model")

                if (ConnectionManager.isConnected) {
                    persistence.deleteContentBlock(article.id, toRemove.id, board.id)
                }
                else{
                    dbQueue.addToQueue(Delete(persistence, toRemove, boardDependency = board, noteDependency = article))
                }

                notifySubscribers()
            }
        }
    }

    // TODO: later (expand to other ContentBlock types)
    fun saveBlock(index: Int, stringContent: String = "", pathsContent: MutableList<Path> = mutableListOf(), heightContent: MutableState<Float> = mutableStateOf(0f), bListContent: MutableList<Byte> = mutableListOf(),
                  language: String = "kotlin", article: Note, board: Board) {
        contentBlockDict[article.id]?.let { contentBlocks ->
            if (index in 0..(contentBlocks.size - 1)) {
                var block = contentBlocks[index]
                if (block is TextBlock) {
                    (block as TextBlock).text = stringContent
                } else if (block is MarkdownBlock) {
                    (block as MarkdownBlock).text = stringContent
                } else if (block is CodeBlock) {
                    (block as CodeBlock).text = stringContent
                } else if (block is CanvasBlock) {
                    (block as CanvasBlock).paths = pathsContent
                    (block as CanvasBlock).height = canvasHeight

                } else if (block is MathBlock) {
                    (block as MathBlock).text = stringContent
                } else if (block is MediaBlock) {
                    (block as MediaBlock).bList = bListContent
                }
                // TODO: might need to fix for canvas? idk if it can handle it yet
                if (ConnectionManager.isConnected) {
                    persistence.updateContentBlock(block, stringContent, pathsContent, language, article, board.id)
                }
                else{
                    dbQueue.addToQueue(
                        Update(persistence, block, mutableMapOf(
                        "text" to stringContent,
                        "pathsContent" to pathsContent,
                        "bListContent" to bListContent,
                        "language" to language,
                        "article" to article,
                        "boardId" to board.id
                    ))
                    )
                }
            }
        }
        notifySubscribers()
    }
}