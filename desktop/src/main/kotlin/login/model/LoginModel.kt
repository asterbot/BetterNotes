package login.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import login.entities.User
import org.bson.types.ObjectId
import org.mindrot.jbcrypt.BCrypt
import shared.ConnectionManager
import shared.persistence.IPersistence

class LoginModel(val persistence: IPersistence) {

    var currentUser by mutableStateOf("dummy-user")

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

    fun addUser(username: String, password: String){
        if (ConnectionManager.isConnected) {
            // Hash pwd and send to DB
            persistence.addUser(User(ObjectId(), username, BCrypt.hashpw(password, BCrypt.gensalt())))
        }
    }

}