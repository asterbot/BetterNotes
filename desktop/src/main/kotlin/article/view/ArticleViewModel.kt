package article.view
import androidx.compose.runtime.mutableStateListOf
import article.entities.ContentBlock
import article.model.ArticleModel
import shared.ISubscriber
import individual_board.model.IndvBoardModel;
import individual_board.entities.Note

class ArticleViewModel(private val model: ArticleModel): ISubscriber {
    val contentBlocksList = mutableStateListOf<ContentBlock>()

    init{
        model.subscribe(this)
        update()
    }

    override fun update() {
        println("DEBUG: ArticleViewModel update")
        contentBlocksList.clear()
        for (block in model.contentBlocks) {
            contentBlocksList.add(block)
        }
    }
}