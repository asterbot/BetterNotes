package shared


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import article.model.ArticleModel
import article.view.ArticleViewModel
import boards.model.BoardModel
import boards.view.BoardViewModel
import shared.persistence.DBStorage
import individual_board.model.IndvBoardModel as IndividualBoardModel
import individual_board.view.IndvBoardViewModel as IndividualBoardViewModel

val dbStorage: DBStorage = DBStorage()

val boardModel = BoardModel(dbStorage)
val boardViewModel = BoardViewModel(boardModel)

val individualBoardModel = IndividualBoardModel(dbStorage)
lateinit var individualBoardViewModel: IndividualBoardViewModel

val articleModel = ArticleModel(dbStorage)
lateinit var articleViewModel: ArticleViewModel

object ConnectionManager{
    // Maintains whether there is a DB connection or not
    var isConnected by mutableStateOf(false)
    fun updateConnection(status: Boolean) {
        isConnected = status
    }
}

