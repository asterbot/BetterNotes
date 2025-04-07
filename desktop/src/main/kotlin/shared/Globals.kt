package shared


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import article.model.ArticleModel
import article.view.ArticleViewModel
import boards.model.BoardModel
import boards.view.BoardViewModel
import boards.view.BoardViewScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import shared.persistence.DBQueue
import shared.persistence.DBStorage
import login.model.LoginModel
import shared.persistence.Operation
import individual_board.model.IndvBoardModel as IndividualBoardModel
import individual_board.view.IndvBoardViewModel as IndividualBoardViewModel
import fdg_layout.FdgLayoutModel
import fdg_layout.FdgLayoutViewModel
import individual_board.entities.Note

val dbStorage: DBStorage = DBStorage()
val dbQueue: DBQueue = DBQueue()

val boardModel = BoardModel(dbStorage)
val boardViewModel = BoardViewModel(boardModel)

val individualBoardModel = IndividualBoardModel(dbStorage)
lateinit var individualBoardViewModel: IndividualBoardViewModel

var articleModel = ArticleModel(dbStorage)
lateinit var articleViewModel: ArticleViewModel

val loginModel = LoginModel(dbStorage)

//val graphModel = GraphModel()
//val graphViewModel = GraphViewModel(graphModel)

val fdgLayoutModel = FdgLayoutModel<Note>()
val fdgLayoutViewModel = FdgLayoutViewModel(fdgLayoutModel)

fun initializeModels(){
    boardModel.initialize()
    individualBoardModel.initialize()
    articleModel.initialize()
}

object LoginManager{
    var loggedIn by mutableStateOf(false)

    fun logIn(){
        loggedIn = true
    }

    fun logOut(){
        loggedIn = false
        loginModel.currentUser = "dummy-user"
        ScreenManager.reset()
    }
}

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
            loginModel?.initialize()

            Operation.contentBlocksInserted.clear()

        }
    }
}


object ScreenManager {
    var visitedScreens = mutableStateListOf<Screen>()
    var currScreenIndex by mutableStateOf(0)

    init {
        visitedScreens.add(BoardViewScreen())
        currScreenIndex = visitedScreens.size - 1
    }

    fun reset(){
        visitedScreens.clear()
        visitedScreens.add(BoardViewScreen())
        currScreenIndex = visitedScreens.size - 1
    }

    fun push(navigator: Navigator, screen: Screen) {
        visitedScreens.subList(currScreenIndex + 1, visitedScreens.size).clear()
        visitedScreens.add(screen)
        navigator.push(screen)
        currScreenIndex = visitedScreens.size - 1
    }

    fun moveBack(navigator: Navigator) {
        currScreenIndex -= 1
        val prevScreen = visitedScreens[currScreenIndex]
        navigator.push(prevScreen)
    }

    fun moveForward(navigator: Navigator) {
        currScreenIndex += 1
        val nextScreen = visitedScreens[currScreenIndex]
        navigator.push(nextScreen)
    }
}