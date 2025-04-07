package article.model

import article.entities.BlockType
import article.entities.MarkdownBlock
import article.entities.TextBlock
import boards.entities.Board
import boards.model.BoardModel
import individual_board.entities.Note
import individual_board.model.IndvBoardModel
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.assertEquals
import shared.persistence.DBStorage
import shared.persistence.IPersistence
import kotlin.test.BeforeTest
import kotlin.test.Test

class ArticleModelTest {
    lateinit var mockDB: IPersistence
    lateinit var boardModel: BoardModel
    lateinit var board: Board
    lateinit var indvBoardModel: IndvBoardModel
    lateinit var articleModel: ArticleModel
    lateinit var article: Note

    @BeforeTest
    fun setup() {
        mockDB = DBStorage("cs346-test-db")
        boardModel = BoardModel(mockDB)
        indvBoardModel = IndvBoardModel(mockDB)
        articleModel = ArticleModel(mockDB)

        // Clear the DB before starting
        runBlocking { mockDB.clearDB() }

        // create instance to test on (main the article one)
        board = Board(ObjectId(), name="Board for Article Testing", desc="desc")
        article = Note(ObjectId(), title="Test Article")

        // save to models (local)
        boardModel.boardList = mutableListOf(board)
        indvBoardModel.noteDict = mutableMapOf(
            board.id to mutableListOf(article)
        )
        articleModel.contentBlockDict = mutableMapOf(
            article.id to mutableListOf()
        )

        // save to DB (remote)
        mockDB.addBoard(board)
        mockDB.addNote(board, article)
    }

    @Test
    fun setupConfirm() {
        assertEquals(1, boardModel.boardList.size)
        assertEquals(1, indvBoardModel.noteDict.size)
        assertEquals(1, indvBoardModel.noteDict[board.id]?.size)
        assertEquals(1, articleModel.contentBlockDict.size)
        assertEquals(0, articleModel.contentBlockDict[article.id]?.size)
    }

    /* add content blocks */

    @Test
    fun addFirstCB() {
        val oldCBCount: Int? = articleModel.contentBlockDict[article.id]?.size
        articleModel.addBlock(0, "DOWN", BlockType.PLAINTEXT, article, board, await=true)
        if (oldCBCount != null) {
            assertEquals(oldCBCount + 1, articleModel.contentBlockDict[article.id]?.size) // check local model
            assertEquals(oldCBCount + 1, mockDB.readContentBlocks()[article.id]?.size)
        }
    }

    @Test
    fun addCBFromEnds() {
        val contentBlocks = articleModel.contentBlockDict[article.id]
        val oldCBCount: Int? = contentBlocks?.size
        articleModel.addBlock(0, "DOWN", BlockType.PLAINTEXT, article, board, await=true)
        articleModel.addBlock(0, "DOWN", BlockType.PLAINTEXT, article, board, await=true) // down
        articleModel.addBlock(0, "UP", BlockType.PLAINTEXT, article, board, await=true) // up
        if (oldCBCount != null) {
            assertEquals(oldCBCount + 3, contentBlocks.size) // check local model
            assertEquals(oldCBCount + 3, mockDB.readContentBlocks()[article.id]?.size) // get remote DB
        }
        // test glue properties
        assertEquals(false, (contentBlocks?.get(0)?.gluedAbove))
        assertEquals(true, (contentBlocks?.get(0)?.gluedBelow) == (contentBlocks?.get(1)?.gluedAbove))
        assertEquals(true, (contentBlocks?.get(1)?.gluedBelow) == (contentBlocks?.get(2)?.gluedAbove))
        assertEquals(false, (contentBlocks?.get(2)?.gluedBelow))
    }

    @Test
    fun addCBInMiddleGlued() {
        val contentBlocks = articleModel.contentBlockDict[article.id]
        val oldCBCount: Int? = contentBlocks?.size
        // add content block in middle
        articleModel.addBlock(0, "DOWN", BlockType.PLAINTEXT, article, board, await=true)
        articleModel.addBlock(0, "DOWN", BlockType.PLAINTEXT, article, board, await=true)
        articleModel.addBlock(0, "DOWN", BlockType.PLAINTEXT, article, board, await=true) // test this block
        if (oldCBCount != null) {
            assertEquals(oldCBCount + 3, contentBlocks.size) // check local model
            assertEquals(oldCBCount + 3, mockDB.readContentBlocks()[article.id]?.size) // get remote DB
            assertEquals(true, contentBlocks[1].gluedAbove)
            assertEquals(true, contentBlocks[1].gluedBelow)
        }
        assertEquals(true, (contentBlocks?.get(0)?.gluedBelow) == (contentBlocks?.get(1)?.gluedAbove))
        assertEquals(true, (contentBlocks?.get(1)?.gluedBelow) == (contentBlocks?.get(2)?.gluedAbove))
    }

    @Test
    fun addCBInMiddleNotGlued() {
        val contentBlocks = articleModel.contentBlockDict[article.id]
        val oldCBCount: Int? = contentBlocks?.size
        // add content block in middle
        articleModel.addBlock(0, "DOWN", BlockType.PLAINTEXT, article, board, await=true)
        articleModel.addBlock(0, "DOWN", BlockType.PLAINTEXT, article, board, await=true)
        articleModel.toggleGlueUpwards(1, article, board, await=true) // split blocks
        articleModel.addBlock(1, "UP", BlockType.PLAINTEXT, article, board, await=true)
        articleModel.addBlock(0, "DOWN", BlockType.PLAINTEXT, article, board, await=true)
        if (oldCBCount != null) {
            assertEquals(oldCBCount + 4, contentBlocks.size) // check local model
            assertEquals(oldCBCount + 4, mockDB.readContentBlocks()[article.id]?.size) // get remote DB
            assertEquals(true, contentBlocks[1].gluedAbove)
            assertEquals(false, contentBlocks[1].gluedBelow)
            assertEquals(false, contentBlocks[2].gluedAbove)
            assertEquals(true, contentBlocks[2].gluedBelow)
        }
    }

    /* duplicate content blocks */

    @Test
    fun dupCBAtBottom() {
        val contentBlocks = articleModel.contentBlockDict[article.id]
        val oldCBCount: Int? = contentBlocks?.size
        articleModel.addBlock(0, "DOWN", BlockType.PLAINTEXT, article, board, await=true)
        (contentBlocks?.get(0) as TextBlock).text = "Hello World!"
        articleModel.duplicateBlock(0, article, board, await=true)
        if (oldCBCount != null) {
            assertEquals(oldCBCount + 2, contentBlocks.size) // check local model
            assertEquals(oldCBCount + 2, mockDB.readContentBlocks()[article.id]?.size) // get remote DB
            assertEquals(true, contentBlocks[1].gluedAbove)
            assertEquals(false, contentBlocks[1].gluedBelow)
            assertEquals(true, contentBlocks[0].blockType == contentBlocks[1].blockType)
            assertEquals(true, (contentBlocks[0] as TextBlock).text == (contentBlocks[1] as TextBlock).text)
        }
    }

    @Test
    fun dupCBInMiddle() {
        val contentBlocks = articleModel.contentBlockDict[article.id]
        val oldCBCount: Int? = contentBlocks?.size
        articleModel.addBlock(0, "DOWN", BlockType.PLAINTEXT, article, board, await=true)
        articleModel.addBlock(0, "UP", BlockType.MARKDOWN, article, board, await=true)
        (contentBlocks?.get(0) as MarkdownBlock).text = "Hello World!"
        articleModel.duplicateBlock(0, article, board, await=true)
        if (oldCBCount != null) {
            assertEquals(oldCBCount + 3, contentBlocks.size) // check local model
            assertEquals(oldCBCount + 3, mockDB.readContentBlocks()[article.id]?.size) // get remote DB
            assertEquals(true, contentBlocks[1].gluedAbove)
            assertEquals(true, contentBlocks[1].gluedBelow)
            assertEquals(BlockType.MARKDOWN, contentBlocks[1].blockType)
            assertEquals(true, contentBlocks[0].blockType == contentBlocks[1].blockType)
            assertEquals(true, (contentBlocks[0] as MarkdownBlock).text == (contentBlocks[1] as MarkdownBlock).text)
        }
    }

}