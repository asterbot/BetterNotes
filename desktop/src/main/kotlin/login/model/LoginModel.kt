package login.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import login.entities.User
import org.bson.types.ObjectId
import org.mindrot.jbcrypt.BCrypt
import shared.ConnectionManager
import shared.persistence.IPersistence
import java.io.File

class LoginModel(val persistence: IPersistence) {

    var currentUser by mutableStateOf("dummy-user")

    // For password verification
    val uwRegex = """.*U.*W""".toRegex()
    val passwordCriteria = listOf(
        Pair("At least 8 characters",
            { pwd: String -> pwd.length >= 8 }
        ),
        Pair("At least 1 digit",
            { pwd: String -> pwd.count(Char::isDigit) > 0 }
        ),
        Pair("At least 1 lowercase and 1 uppercase character",
            { pwd: String -> pwd.any(kotlin.Char::isLowerCase) && pwd.any(kotlin.Char::isUpperCase) }
        ),
        Pair("At least 1 special character out of: # ! @ ^",
            { pwd: String -> pwd.any { it in "#!@^" } }
        ),
        Pair("Must contain characters U and W in-order",
            { pwd: String -> uwRegex.containsMatchIn(pwd) }
        )

    )

    // For the directory to store credentials
    val homeDir = System.getProperty("user.home")
    val credentialsFilePath = "$homeDir/.note_taking_credentials"
    val credentialsFile = File(credentialsFilePath)

    init {
        persistence.connect()
    }

    fun initialize() {
        // Called when there is a reconnection
        persistence.connect()
    }

    fun changeCurrentUser(newUser: String) {
        currentUser = newUser
    }

    fun addUser(username: String, password: String): Boolean{
        if (ConnectionManager.isConnected) {
            // Hash pwd and send to DB
            return persistence.addUser(User(ObjectId(), username, BCrypt.hashpw(password, BCrypt.gensalt())))
        }
        else{
            return false
        }
    }

    fun saveUser(username:String, password: String){
        val content = "$username\n$password"
        try{
            credentialsFile.writeText(content)
        }
        catch (e: Exception){
            println("Error writing to file: $e")
        }
    }

    fun getUser(): Pair<String,String>?{
        if (!credentialsFile.exists()){
            return null
        }
        else{
            val content = credentialsFile.readText().split("\n")
            println(content)
            return Pair(content[0],content[1])
        }
    }

    fun passwordCriteriaMet(password: String): List<Boolean> {
        val toRet = mutableListOf<Boolean>()

        for (i in 0..<passwordCriteria.size){
            toRet.add(passwordCriteria[i].second(password))
        }

        return toRet
    }

}