package article.view

import article.entities.ContentBlock
import article.entities.TextBlock
import article.model.ArticleModel
import org.bson.types.ObjectId
import shared.persistence.DBStorage
import shared.persistence.IPersistence
import kotlin.test.*


class ArticleViewModelTest() {
    lateinit var mockDB: IPersistence
    lateinit var articleModel: ArticleModel
    lateinit var articleViewModel: ArticleViewModel
    lateinit var blockId: ObjectId

    @BeforeTest
    fun setup() {
        mockDB = DBStorage("cs346-test-db")
        articleModel = ArticleModel(mockDB)
        articleModel.contentBlockDict = mutableMapOf()

        blockId = ObjectId()
        articleModel.contentBlockDict[blockId] = mutableListOf()

        articleViewModel = ArticleViewModel(articleModel, blockId)
    }

    @Test
    fun update() {
        val block1 = TextBlock(text = "Hello")
        val block2 = TextBlock(text = "World")
        articleModel.contentBlockDict[blockId]?.addAll(listOf(block1, block2))

        articleViewModel.update()

        assertEquals(2, articleViewModel.contentBlocksList.size)
        assertEquals(block1, articleViewModel.contentBlocksList[0])
        assertEquals(block2, articleViewModel.contentBlocksList[1])
    }
}