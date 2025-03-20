package shared


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import article.model.ArticleModel
import article.view.ArticleViewModel
import individual_board.view.IndvBoardViewModel as IndividualBoardViewModel
import boards.view.BoardViewModel
import individual_board.model.IndvBoardModel as IndividualBoardModel
import boards.model.BoardModel
import graph_ui.*
import shared.persistence.DBStorage

val dbStorage: DBStorage = DBStorage()

val boardModel = BoardModel(dbStorage)
val boardViewModel = BoardViewModel(boardModel)

val individualBoardModel = IndividualBoardModel(dbStorage)
lateinit var individualBoardViewModel: IndividualBoardViewModel

val articleModel = ArticleModel(dbStorage)
lateinit var articleViewModel: ArticleViewModel

val graphModel = GraphModel()
val graphViewModel = GraphViewModel(graphModel)


object ConnectionManager{
    // Maintains whether there is a DB connection or not
    var isConnected by mutableStateOf(false)
    fun updateConnection(status: Boolean) {
        isConnected = status
    }
}

