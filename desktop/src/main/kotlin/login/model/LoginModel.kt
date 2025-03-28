package login.model

import login.entities.User
import org.bson.types.ObjectId
import org.mindrot.jbcrypt.BCrypt
import shared.ConnectionManager
import shared.persistence.IPersistence

class LoginModel(val persistence: IPersistence) {

    val currentUser: String = "dummy-user"

    init {
        persistence.connect()
    }

    fun initialize() {
        // Called when there is a reconnection
        persistence.connect()
    }

    fun addUser(username: String, password: String){
        if (ConnectionManager.isConnected) {
            // Hash pwd and send to DB
            persistence.addUser(User(ObjectId(), username, BCrypt.hashpw(password, BCrypt.gensalt())))
        }
    }

}