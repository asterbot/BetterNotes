

// Boards page imports

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import boards.view.BoardViewScreen
import cafe.adriel.voyager.navigator.Navigator
import com.mongodb.kotlin.client.coroutine.MongoClient


//import individual_board.view.ViewModel as IndividualBoardViewModel
//import boards.view.ViewModel as BoardViewModel
//import individual_board.model.Model as IndividualBoardModel
//import boards.model.Model as BoardModel
//
//val boardModel = BoardModel()
//val boardViewModel = BoardViewModel(boardModel)
//
//val individualBoardModel = IndividualBoardModel()
//val individualBoardViewModel = IndividualBoardViewModel(individualBoardModel)

data class Movie(val title: String, val year: Int, val cast: List<String>)

fun main() {
    val pwd = System.getenv("PWD")
    println("PASSWORD: $pwd")
    val connectionString = "mongodb+srv://goatedbetternotes:${pwd}@cs346-notetaking.w6ldm.mongodb.net/?retryWrites=true&w=majority&appName=cs346-notetaking"
    val databaseName = "cs-346-notetaking"

//    var conn = setupConnection(connectionString, databaseName)
//    println(conn)

    // Replace the placeholder with your MongoDB deployment's connection string
    val uri = connectionString
    val mongoClient = MongoClient.create(uri)
    val database = mongoClient.getDatabase(databaseName)
    // Get a collection of documents of type Movie
    println("got here")
    val collection = database.getCollection<Movie>("movies")
    println(":(")
    println(collection)
//
//
//    runBlocking {
//        val doc = collection.find(eq("title", "Back to the Future")).firstOrNull()
//        if (doc != null) {
//            println(doc)
//        } else {
//            println("No matching documents found.")
//        }
//    }

    mongoClient.close()


    // composable stuff
    application {
        Window(onCloseRequest = ::exitApplication) {
            // Starting screen
            Navigator(BoardViewScreen())
        }
    }
}

