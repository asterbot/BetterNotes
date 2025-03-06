package article.view
import androidx.compose.runtime.mutableStateListOf
import article.entities.ContentBlock
import article.model.ArticleModel
import shared.ISubscriber

class ArticleViewModel(private val model: ArticleModel): ISubscriber {
    val contentBlocksList = mutableStateListOf<ContentBlock>()

    init{
        model.subscribe(this)
        update()
    }

    override fun update() {
        contentBlocksList.clear()
        for (block in model.contentBlocks) {
            contentBlocksList.add(block)
        }
    }
}