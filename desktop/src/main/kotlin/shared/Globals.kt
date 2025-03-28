package shared


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import article.model.ArticleModel
import article.view.ArticleViewModel
import individual_board.view.IndvBoardViewModel as IndividualBoardViewModel
import boards.view.BoardViewModel
import shared.persistence.DBQueue
import shared.persistence.DBStorage
import individual_board.model.IndvBoardModel as IndividualBoardModel
import boards.model.BoardModel
import graph_ui.*
import login.model.LoginModel
import shared.persistence.Operation

val dbStorage: DBStorage = DBStorage()
val dbQueue: DBQueue = DBQueue()

val boardModel = BoardModel(dbStorage)
val boardViewModel = BoardViewModel(boardModel)

val individualBoardModel = IndividualBoardModel(dbStorage)
lateinit var individualBoardViewModel: IndividualBoardViewModel

val articleModel = ArticleModel(dbStorage)
lateinit var articleViewModel: ArticleViewModel

val loginModel = LoginModel(dbStorage)

val graphModel = GraphModel()
val graphViewModel = GraphViewModel(graphModel)


// Managing connection status

enum class ConnectionStatus{
    CONNECTED,          // The connection to the DB is successful
    DISCONNECTED,       // The connection to the DB is unsuccessful
    CONNECTING,         // The DB connection process has started, but not completed
}

object ConnectionManager{
    // Maintains whether there is a DB connection or not
    var connection by mutableStateOf(ConnectionStatus.DISCONNECTED)
    var isConnected by mutableStateOf(false)

    fun updateConnection(status: ConnectionStatus) {
        val previousStatus = connection
        connection = status
        isConnected = (connection == ConnectionStatus.CONNECTED)

        if (previousStatus == ConnectionStatus.CONNECTING && status == ConnectionStatus.CONNECTED){
            // If you are going from disconnected -> connected, initialize all the models connections and sync DB

            dbQueue.syncDB()

            // These null-checks are needed
            boardModel?.initialize()
            individualBoardModel?.initialize()
            articleModel?.initialize()

            Operation.contentBlocksInserted.clear()


        }

    }

}
