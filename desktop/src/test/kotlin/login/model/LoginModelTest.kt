package login.model
import androidx.compose.ui.graphics.Path
import article.entities.ContentBlock
import kotlin.test.*
import boards.entities.*
import individual_board.entities.Note
import kotlinx.coroutines.runBlocking
import login.entities.User
import org.bson.types.ObjectId
import org.mindrot.jbcrypt.BCrypt
import shared.persistence.DBStorage
import shared.persistence.IPersistence

class LoginModelTest() {
    lateinit var mockDB: IPersistence
    lateinit var loginModel: LoginModel
    lateinit var user: User
    lateinit var badUser: User

    @BeforeTest
    fun setup() {
        mockDB = DBStorage("cs346-test-db")

        loginModel = LoginModel(mockDB)

        // Clear the DB before starting
        runBlocking {
            mockDB.clearDB()
        }

        // Add dummy-user in DB if not there already
        mockDB.addUser(User(ObjectId(), "dummy-user", BCrypt.hashpw("dummy-password", BCrypt.gensalt())))

        // New board to test with
        user = User(ObjectId(), "new-user", BCrypt.hashpw("new-password", BCrypt.gensalt()))
        badUser = User(ObjectId(), "dummy-user", BCrypt.hashpw("username-taken-ohno", BCrypt.gensalt()))
    }


    @Test
    fun authenticate(){
        // Check if authentication works
        assertEquals(true, mockDB.authenticate("dummy-user", "dummy-password"))
    }

    @Test
    fun addUser() {
        // Check if adding user works
        val oldCount = mockDB.readUsers().size
        mockDB.addUser(user)
        assertEquals(oldCount + 1, mockDB.readUsers().size)
    }

    @Test
    fun addBadUser(){
        // Check if adding user with existing username doesn't work
        val oldCount = mockDB.readUsers().size
        mockDB.addUser(badUser)
        // The count should remain the same!
        assertEquals(oldCount, mockDB.readUsers().size)
    }

    @Test
    fun deleteUser(){
        // Deleting user correctly
        mockDB.addUser(user)
        val oldCount = mockDB.readUsers().size
        mockDB.deleteUser("new-password", "new-user")
        assertEquals(oldCount - 1, mockDB.readUsers().size)
    }

    @Test
    fun badDeleteUser(){
        // Should not delete - user's password does not match
        mockDB.addUser(user)
        val oldCount = mockDB.readUsers().size
        mockDB.deleteUser("obvious-wrong-password", "new-user")
        assertEquals(oldCount, mockDB.readUsers().size)
    }

    @Test
    fun updateUserPassword(){
        // Updating password correctly
        mockDB.addUser(user)
        mockDB.updatePassword("new-password", "new-new-password", "new-user")
        assertEquals(true, mockDB.authenticate("new-user", "new-new-password"))
        mockDB.updatePassword("new-new-password", "new-password", "new-user")
    }

    @Test
    fun badUpdatePassword(){
        // Should not update - User's old password does not match
        mockDB.addUser(user)
        mockDB.updatePassword("obviously-wrong-password", "new-new-password", "new-user")
        // Should not authenticate
        assertEquals(false, mockDB.authenticate("new-user", "new-new-password"))
    }

    @AfterTest
    fun tearDown() {
        // Delete the new-user no matter what
        try{
            mockDB.deleteUser("new-password", "new-user")
        }
        catch(e: Exception){
            return
        }
    }
}
