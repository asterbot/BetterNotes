package article.model
import article.entities.*
import boards.entities.Board
import individual_board.entities.Note
import org.bson.types.ObjectId
import shared.ConnectionManager
import shared.IPublisher
import shared.dbQueue
import shared.persistence.*

// TODO: NOTE: should pass in board probably
class ArticleModel(val persistence: IPersistence) : IPublisher() {

    // maps Article ID to list of content blocks in the Article
    var contentBlockDict = mutableMapOf<ObjectId, MutableList<ContentBlock>>()

    init {
        persistence.connect()
//        if (ConnectionManager.isConnected){
//            contentBlockDict = persistence.readContentBlocks()
//            notifySubscribers()
//        }
//        println("DEBUG: initialized ArticleModel")
    }

    fun initialize() {
        // Called when there is a reconnection
        if (ConnectionManager.isConnected) {
            contentBlockDict = persistence.readContentBlocks()
            notifySubscribers()
        }
    }

    // helper function for gluing blocks together
    fun glueBlocks(upperBlock: ContentBlock, lowerBlock: ContentBlock) {
        upperBlock.gluedBelow = true
        lowerBlock.gluedAbove = true
    }

    // helper function for toggling glue states
    fun toggleGlueBlocks(upperBlock: ContentBlock, lowerBlock: ContentBlock, value: Boolean) {
        upperBlock.gluedBelow = value
        lowerBlock.gluedAbove = value
    }

    fun toggleGlueUpwards(index: Int, article: Note, board: Board) {
        contentBlockDict[article.id]?.let { contentBlocks ->
            if (index in 1..(contentBlocks.size - 1)) {
                toggleGlueBlocks(contentBlocks[index-1], contentBlocks[index], !contentBlocks[index].gluedAbove)
            }
            if (ConnectionManager.isConnected) {
                var updateBlock = contentBlocks[index-1]
                persistence.updateGlueStatus(updateBlock.id, updateBlock.gluedAbove, updateBlock.gluedBelow, article.id, board.id)
                updateBlock = contentBlocks[index]
                persistence.updateGlueStatus(updateBlock.id, updateBlock.gluedAbove, updateBlock.gluedBelow, article.id, board.id)
            } else {
                dbQueue.addToQueue(UpdateGlue(persistence, contentBlocks[index-1], board, article))
                dbQueue.addToQueue(UpdateGlue(persistence, contentBlocks[index], board, article))
            }
            notifySubscribers()
        }
    }

    fun toggleGlueDownwards(index: Int, article: Note, board: Board) {
        contentBlockDict[article.id]?.let { contentBlocks ->
            if (index in 0..(contentBlocks.size - 2)) {
                toggleGlueBlocks(contentBlocks[index], contentBlocks[index+1], !contentBlocks[index].gluedBelow)
            }
            if (ConnectionManager.isConnected) {
                var updateBlock = contentBlocks[index]
                persistence.updateGlueStatus(updateBlock.id, updateBlock.gluedAbove, updateBlock.gluedBelow, article.id, board.id)
                updateBlock = contentBlocks[index+1]
                persistence.updateGlueStatus(updateBlock.id, updateBlock.gluedAbove, updateBlock.gluedBelow, article.id, board.id)
            } else {
                dbQueue.addToQueue(UpdateGlue(persistence, contentBlocks[index], board, article))
                dbQueue.addToQueue(UpdateGlue(persistence, contentBlocks[index+1], board, article))
            }
            notifySubscribers()
        }
    }

    fun addBlock(index: Int, direction: String?, type: BlockType, article: Note, board: Board) {
        // direction can be either "UP" or "DOWN"
        contentBlockDict[article.id]?.let { contentBlocks ->
            val blockToAdd = type.createDefaultBlock()
            // index is a valid value, insert as expected
            if (index in 0..contentBlocks.size) {
                contentBlocks.add(index, blockToAdd)
            }

            /*
            next, reapply glue to surrounding blocks
            when adding a block, we consider what the initial state of the blocks were before insertion (5 cases):
                1) there are blocks above and below, but they were not glued
                    then, we attach the new block to whatever direction we called this function from
                2) there are blocks above and below, and they were glued together before
                    then, the added block should be glued to both to maintain glued-block formation
                3) only a block is above the added block (i.e. must have inserted downwards)
                    then, we glue the added block to the one above it
                4) only a block is below the added block (i.e. must have inserted upwards)
                    then, we glue the added block to the one below it
                5) no blocks are above or below
                    then, this is the first inserted block (don't do anything)
            */

            // case 5) (skip it)
            if (contentBlocks.size > 1) {
                // case 4)
                if (index == 0) {
                    glueBlocks(contentBlocks[index], contentBlocks[index + 1])
                }
                // case 3)
                else if (index == contentBlocks.size - 1) {
                    glueBlocks(contentBlocks[index-1], contentBlocks[index])
                }
                else {
                    // case 2)
                    if (contentBlocks[index-1].gluedBelow && contentBlocks[index+1].gluedAbove) {
                        glueBlocks(contentBlocks[index-1], contentBlocks[index])
                        glueBlocks(contentBlocks[index], contentBlocks[index+1])
                    }
                    // case 1)
                    else if (!contentBlocks[index-1].gluedBelow && !contentBlocks[index+1].gluedAbove) {
                        // "UP" <=> we added the new block above an old one
                        if (direction == "UP") {
                            glueBlocks(contentBlocks[index], contentBlocks[index+1])
                        } else {
                            glueBlocks(contentBlocks[index-1], contentBlocks[index])
                        }
                    }
                }
            }

            if (ConnectionManager.isConnected) {
                // update surrounding blocks' glue status (is possible)
                if (index - 1 >= 0) {
                    val updateBlock = contentBlocks[index - 1]
                    persistence.updateGlueStatus(updateBlock.id, updateBlock.gluedAbove, updateBlock.gluedBelow, article.id, board.id)
                }
                if (index + 1 <= contentBlocks.size-1) {
                    val updateBlock = contentBlocks[index + 1]
                    persistence.updateGlueStatus(updateBlock.id, updateBlock.gluedAbove, updateBlock.gluedBelow, article.id, board.id)
                }
                persistence.insertContentBlock(article, blockToAdd, index, board.id)
            }
            else{
                if (index - 1 >= 0) {
                    dbQueue.addToQueue(UpdateGlue(persistence, contentBlocks[index-1], board, article))
                }
                if (index + 1 <= contentBlocks.size-1) {
                    dbQueue.addToQueue(UpdateGlue(persistence, contentBlocks[index+1], board, article))
                }
                dbQueue.addToQueue(Create(persistence, blockToAdd,
                    boardDependency = board, noteDependency = article, indexDependency = index))
            }
            notifySubscribers()
        }
    }

    fun duplicateBlock(index: Int, article: Note, board: Board) {
        contentBlockDict[article.id]?.let { contentBlocks ->
            if (index in 0..(contentBlocks.size - 1)) {
                val dupBlock: ContentBlock = contentBlocks[index].copyBlock()
                contentBlocks.add(index + 1, dupBlock)

                /*
                when duplicating, there are two cases:
                    1) the block we are duplicating from is NOT glued at the bottom
                        then, we simply attach the duplicated block to the original
                    2) the block we are duplicating is glued at the bottom
                        then, there existed a block below the original that it was glued to
                        so, the duplicated block should be glued on both sides
                */
                // case 2)
                if (contentBlocks[index].gluedBelow) {
                    glueBlocks(contentBlocks[index+1], contentBlocks[index+2])
                }
                // cases 1) and 2)
                glueBlocks(contentBlocks[index], contentBlocks[index+1])

                if (ConnectionManager.isConnected) {
                    // update glue for surrounding blocks
                    var updateBlock = contentBlocks[index]
                    persistence.updateGlueStatus(updateBlock.id, updateBlock.gluedAbove, updateBlock.gluedBelow, article.id, board.id)
                    if (index+2 <= contentBlocks.size-1) {
                        updateBlock = contentBlocks[index+2]
                        persistence.updateGlueStatus(updateBlock.id, updateBlock.gluedAbove, updateBlock.gluedBelow, article.id, board.id)
                    }
                    persistence.insertContentBlock(article, dupBlock, index+1, board.id)
                } else {
                    dbQueue.addToQueue(UpdateGlue(persistence, contentBlocks[index], board, article))
                    if (index+2 <= contentBlocks.size-1) {
                        dbQueue.addToQueue(UpdateGlue(persistence, contentBlocks[index+2], board, article))
                    }
                    dbQueue.addToQueue(Create(persistence, contentBlocks[index+1],
                        boardDependency = board, noteDependency = article, indexDependency = index+1))
                }
                notifySubscribers()
            }
        }
    }

    // helper function for moveBlockUp/Down
    fun swapPosAndGlue(contentBlocks: MutableList<ContentBlock>, index1: Int, index2: Int) {
        val temp = contentBlocks[index1]
        contentBlocks[index1] = contentBlocks[index2]
        contentBlocks[index2] = temp
        val tempGluedAbove = contentBlocks[index1].gluedAbove
        contentBlocks[index1].gluedAbove = contentBlocks[index2].gluedAbove
        contentBlocks[index2].gluedAbove = tempGluedAbove
        val tempGluedBelow = contentBlocks[index1].gluedBelow
        contentBlocks[index1].gluedBelow = contentBlocks[index2].gluedBelow
        contentBlocks[index2].gluedBelow = tempGluedBelow
    }

    fun getBlockBounds(contentBlocks: MutableList<ContentBlock>, upperBlockEnd: Int, lowerBlockStart: Int): Pair<Int, Int> {
        // traverse the gluedAbove/gluedBelow states of the content blocks until we find the bounds of each block to swap
        var upperBlockStart = upperBlockEnd
        var lowerBlockEnd = lowerBlockStart
        while (upperBlockStart >= 0 && contentBlocks[upperBlockStart].gluedAbove) {
            upperBlockStart--
        }
        while (lowerBlockEnd <= contentBlocks.size && contentBlocks[lowerBlockEnd].gluedBelow) {
            lowerBlockEnd++
        }
        return Pair(upperBlockStart, lowerBlockEnd)
    }

    // helper function for moving blocks that are glued together
    fun moveGluedBlocks(contentBlocks: MutableList<ContentBlock>, upperBlockStart: Int, upperBlockEnd: Int,
                        lowerBlockStart: Int, lowerBlockEnd: Int) {
        // one by one, insert the contents from the lower block in front of the upper block
        for (offset in 0..lowerBlockEnd-lowerBlockStart) {
            val toMoveIndex = lowerBlockStart + offset
            val toInsertIndex = upperBlockStart + offset
            val blockToMove = contentBlocks[toMoveIndex]
            contentBlocks.removeAt(toMoveIndex)
            contentBlocks.add(toInsertIndex, blockToMove)
        }
    }

    fun moveBlockUp(index: Int, article: Note, board: Board): Int? {
        contentBlockDict[article.id]?.let { contentBlocks ->
            if (index in 1..(contentBlocks.size - 1)) {
                /*
                for glued blocks, there are two cases to consider when moving a block up:
                    1) the block's top edge is glued
                        then, we are inside a "chunk" of glued blocks, so we interchange positions within
                    2) the block's top edge is NOT glued
                        then, by moving a block "upwards", we are swapping "chunks" of glued blocks,
                        instead of individual contentBlocks
                */
                if (contentBlocks[index].gluedAbove) {
                    // case 1): swap individual content blocks and their glue states
                    swapPosAndGlue(contentBlocks, index, index-1)

                    if (ConnectionManager.isConnected) {
                        var updateBlock = contentBlocks[index]
                        persistence.updateGlueStatus(updateBlock.id, updateBlock.gluedAbove, updateBlock.gluedBelow, article.id, board.id)
                        updateBlock = contentBlocks[index-1]
                        persistence.updateGlueStatus(updateBlock.id, updateBlock.gluedAbove, updateBlock.gluedBelow, article.id, board.id)
                        persistence.swapContentBlocks(article.id, index-1, index-1, index, index, board.id)
                    } else {
                        dbQueue.addToQueue(UpdateGlue(persistence, contentBlocks[index], board, article))
                        dbQueue.addToQueue(UpdateGlue(persistence, contentBlocks[index-1], board, article))
                        dbQueue.addToQueue(SwapBlocks(persistence, article, board, mutableMapOf(
                            "upperBlockStart" to index-1,
                            "upperBlockEnd" to index-1,
                            "lowerBlockStart" to index,
                            "lowerBlockEnd" to index,
                        )))
                    }
                    notifySubscribers()
                    return index-1
                }
                // otherwise, we are considering swapping entire glued blocks
                else {
                    val upperBlockEnd = index-1
                    val lowerBlockStart = index
                    val (upperBlockStart, lowerBlockEnd) = getBlockBounds(contentBlocks, upperBlockEnd, lowerBlockStart)
                    moveGluedBlocks(contentBlocks, upperBlockStart, upperBlockEnd, lowerBlockStart, lowerBlockEnd)

                    if (ConnectionManager.isConnected) {
                        persistence.swapContentBlocks(article.id, upperBlockStart, upperBlockEnd, lowerBlockStart, lowerBlockEnd, board.id)
                    } else {
                        dbQueue.addToQueue(SwapBlocks(persistence, article, board, mutableMapOf(
                            "upperBlockStart" to upperBlockStart,
                            "upperBlockEnd" to upperBlockEnd,
                            "lowerBlockStart" to lowerBlockStart,
                            "lowerBlockEnd" to lowerBlockEnd,
                        )))
                    }
                    notifySubscribers()
                    return upperBlockStart + (lowerBlockEnd - lowerBlockStart)
                }
            }
        }
        return null
    }

    fun moveBlockDown(index: Int, article: Note, board: Board): Int? {
        contentBlockDict[article.id]?.let { contentBlocks ->
            if (index in 0..(contentBlocks.size - 2)) {
                // we consider the same cases for glued blocks as the function above, but in the opposite direction
                if (contentBlocks[index].gluedBelow) {
                    swapPosAndGlue(contentBlocks, index, index+1)

                    if (ConnectionManager.isConnected) {
                        var updateBlock = contentBlocks[index]
                        persistence.updateGlueStatus(updateBlock.id, updateBlock.gluedAbove, updateBlock.gluedBelow, article.id, board.id)
                        updateBlock = contentBlocks[index+1]
                        persistence.updateGlueStatus(updateBlock.id, updateBlock.gluedAbove, updateBlock.gluedBelow, article.id, board.id)
                        persistence.swapContentBlocks(article.id, index, index, index + 1,index + 1, board.id)
                    } else {
                        dbQueue.addToQueue(UpdateGlue(persistence, contentBlocks[index], board, article))
                        dbQueue.addToQueue(UpdateGlue(persistence, contentBlocks[index+1], board, article))
                        dbQueue.addToQueue(SwapBlocks(persistence, article, board, mutableMapOf(
                            "upperBlockStart" to index,
                            "upperBlockEnd" to index,
                            "lowerBlockStart" to index + 1,
                            "lowerBlockEnd" to index + 1,
                        )))
                    }
                    notifySubscribers()
                    return index+1
                } else {
                    val upperBlockEnd = index
                    val lowerBlockStart = index+1
                    val (upperBlockStart, lowerBlockEnd) = getBlockBounds(contentBlocks, upperBlockEnd, lowerBlockStart)
                    moveGluedBlocks(contentBlocks, upperBlockStart, upperBlockEnd, lowerBlockStart, lowerBlockEnd)

                    if (ConnectionManager.isConnected) {
                        persistence.swapContentBlocks(article.id, upperBlockStart, upperBlockEnd, lowerBlockStart, lowerBlockEnd, board.id)
                    } else {
                        dbQueue.addToQueue(SwapBlocks(persistence, article, board, mutableMapOf(
                            "upperBlockStart" to upperBlockStart,
                            "upperBlockEnd" to upperBlockEnd,
                            "lowerBlockStart" to lowerBlockStart,
                            "lowerBlockEnd" to lowerBlockEnd,
                        )))
                    }
                    notifySubscribers()
                    return upperBlockStart + (lowerBlockEnd - lowerBlockStart) + 1
                }
            }
        }
        return null
    }

    fun deleteBlock(index: Int, article: Note, board: Board) {
        contentBlockDict[article.id]?.let { contentBlocks ->
            if (index in 0..(contentBlocks.size - 1)) {
                val toRemove: ContentBlock = contentBlocks[index]

                /*
                when deleting a block, there are 4 cases:
                    1) the deleted block is glued on neither end
                        then, deleting this block has no effect on anything (no need to change anything)
                    2) the deleted block is glued on both ends
                        then, when deleting, the upper and lower blocks become glued together (no need to change anything)
                    3) the deleted block is only glued above
                        then, it was initially glued to a block above, and its bottom should now become un-glued
                    4) the deleted lock is only glued below
                        then, it was initially glued to a block below, and its top should now become un-glued
                */

                if (toRemove.gluedAbove && !toRemove.gluedBelow) {
                    // case 3)
                    contentBlocks[index-1].gluedBelow = false
                }
                else if (!toRemove.gluedAbove && toRemove.gluedBelow) {
                    // case 4)
                    contentBlocks[index + 1].gluedAbove = false
                }

                // once glue has been updated, delete the block
                contentBlocks.removeAt(index)

                if (ConnectionManager.isConnected) {
                    // update any glue that could have been changed
                    if (index-1 >= 0) {
                        val updateBlock = contentBlocks[index-1]
                        persistence.updateGlueStatus(updateBlock.id, updateBlock.gluedAbove, updateBlock.gluedBelow, article.id, board.id)
                    }
                    if (index+1 <= contentBlocks.size-1) {
                        val updateBlock = contentBlocks[index+1]
                        persistence.updateGlueStatus(updateBlock.id, updateBlock.gluedAbove, updateBlock.gluedBelow, article.id, board.id)
                    }
                    persistence.deleteContentBlock(article.id, toRemove.id, board.id)
                }
                else{
                    if (index-1 >= 0) {
                        dbQueue.addToQueue(UpdateGlue(persistence, contentBlocks[index-1], board, article))
                    }
                    if (index+1 <= contentBlocks.size-1) {
                        dbQueue.addToQueue(UpdateGlue(persistence, contentBlocks[index+1], board, article))
                    }
                    dbQueue.addToQueue(Delete(persistence, toRemove, boardDependency = board, noteDependency = article))
                }
                notifySubscribers()
            }
        }
    }

    // TODO: later (expand to other ContentBlock types)
    fun saveBlock(index: Int, stringContent: String = "", canvasHeight: Int = 0, bList: MutableList<Byte> = mutableListOf(),
                  language: String = "kotlin", gluedAbove: Boolean, gluedBelow: Boolean, article: Note, board: Board) {
        println("I'M IN HERE")
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
                    (block as CanvasBlock).bList = bList
                    (block as CanvasBlock).canvasHeight = canvasHeight

                } else if (block is MathBlock) {
                    (block as MathBlock).text = stringContent
                } else if (block is MediaBlock) {
                    (block as MediaBlock).bList = bList
                }
                // TODO: might need to fix for canvas? idk if it can handle it yet

                block.gluedAbove = gluedAbove
                block.gluedBelow = gluedBelow

                if (ConnectionManager.isConnected) {
                    persistence.updateContentBlock(block, stringContent, canvasHeight, bList, language, gluedAbove, gluedBelow, article, board.id)
                }
                else {
                    dbQueue.addToQueue(
                        Update(persistence, block, mutableMapOf(
                        "text" to stringContent,
                        "language" to language,
                        "canvasHeight" to canvasHeight,
                        "bList" to bList,
                        "gluedAbove" to gluedAbove,
                        "gluedBelow" to gluedBelow,
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