package shared.persistence

import androidx.compose.ui.graphics.Path
import article.entities.*
import boards.entities.Board
import com.mongodb.MongoException
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import individual_board.entities.Note
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import login.entities.User
import login.model.LoginModel
import org.bson.BsonInt64
import org.bson.Document
import org.bson.types.ObjectId
import shared.ConnectionManager
import shared.ConnectionStatus
import java.time.Instant
import kotlin.jvm.internal.Ref.ObjectRef
import org.mindrot.jbcrypt.BCrypt
import shared.loginModel
import kotlin.math.log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach

class DBStorage() :IPersistence {
    // Call connect() before using DB

    private val dotenv = dotenv()

    private val connectionString = dotenv["CONNECTION_STRING"]

    private val databaseName = "cs346-users-db"

    private val uri = connectionString

    private lateinit var mongoClient: MongoClient
    private lateinit var database: MongoDatabase

    private lateinit var boardsCollection: MongoCollection<Board>
    private lateinit var notesCollection: MongoCollection<Note>

    // We need both collections since reading to polymorphic types requires de-serializing
    //   however adding can be done as is done for the other types
    private lateinit var contentBlocksDocumentCollection: MongoCollection<Document>
    private lateinit var contentBlockCollection: MongoCollection<ContentBlock>

    private lateinit var usersCollection: MongoCollection<User>


    // To make everything run in a coroutine!
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // This ensures mutual exclusion
    //     i.e. a thread will finish running before starting a new one (BUT IN THE BACKGROUND)
    //     this is different from thread.join() which will force the foreground to wait for completion
    private val channel = Channel<Job>(capacity = Channel.UNLIMITED).apply {
        coroutineScope.launch {
            consumeEach { it.join() }
        }
    }



    override fun connect(): Boolean {
        try {
            if (!ConnectionManager.isConnected){
                ConnectionManager.updateConnection(ConnectionStatus.CONNECTING)
            }
            mongoClient = MongoClient.create(uri)
            database = mongoClient.getDatabase(databaseName)

            boardsCollection = database.getCollection<Board>("boards")
            notesCollection = database.getCollection<Note>("notes")
            contentBlocksDocumentCollection = database.getCollection<Document>("contentblocks")
            contentBlockCollection = database.getCollection<ContentBlock>("contentblocks")
            usersCollection = database.getCollection<User>("users")

            ConnectionManager.updateConnection(ConnectionStatus.CONNECTED)

            return true
        } catch (e: MongoException) {
            // Dummy data to avoid "late-inits not initialized" error
            mongoClient = MongoClient.create("mongodb://localhost:27017")
            database = mongoClient.getDatabase("dummy-db")

            boardsCollection = database.getCollection("dummy-boards")
            notesCollection = database.getCollection("dummy-notes")
            contentBlocksDocumentCollection = database.getCollection("dummy-contentblocks")
            contentBlockCollection = database.getCollection<ContentBlock>("dummy-contentblocks")
            usersCollection = database.getCollection("dummy-users")

            ConnectionManager.updateConnection(ConnectionStatus.DISCONNECTED)

            return false
        }

    }

    override suspend fun pingDB(): Boolean {
        return try {
            // ping logic
            if (!ConnectionManager.isConnected){
                ConnectionManager.updateConnection(ConnectionStatus.CONNECTING)
            }


            val pingCMD = Document("ping", BsonInt64(1))
            withTimeout(5000) {
                database.runCommand(pingCMD)
            }

            ConnectionManager.updateConnection(ConnectionStatus.CONNECTED)
            true
        }

        catch (e: Exception) {
            println("Error in pinging DB: ${e.javaClass.name} - ${e.message}")
            ConnectionManager.updateConnection(ConnectionStatus.DISCONNECTED)
            false
        }
    }


    override fun updatePassword(oldPassword: String, newPassword: String): Boolean {
        val userData: User?
        runBlocking {
            userData = usersCollection.find(Filters.eq(User::userName.name, loginModel.currentUser)).firstOrNull()
        }
        if (!BCrypt.checkpw(oldPassword, userData?.passwordHash)){
            // Old password does not match!
            return false
        }
        runBlocking {
            usersCollection.updateOne(
                Filters.eq(User::userName.name, loginModel.currentUser),
                Updates.combine(
                    Updates.set("passwordHash", BCrypt.hashpw(newPassword, BCrypt.gensalt()))
                )
            )
        }
        return true

    }

    override fun addUser(user: User): Boolean {
        val userData: User?
        runBlocking {
            userData = usersCollection.find(Filters.eq(User::userName.name, user.userName)).firstOrNull()
        }
        if (userData != null || user.userName == "dummy-user") {
            // I won't let the user take dummy-user because that's our placeholder name :D
            return false
        }
        runBlocking {
            usersCollection.insertOne(user)
        }
        return true
    }

    override fun authenticate(username: String, password: String): Boolean{
        val userData: User?
        runBlocking {
            userData = usersCollection.find(Filters.eq(User::userName.name, username)).firstOrNull()
        }
        if (userData==null){
            // user not in DB
            return false;
        }

        // Return whether password matches record in DB
        return BCrypt.checkpw(password, userData.passwordHash)
    }


    override fun readBoards(): List<Board> {
        val boards = mutableListOf<Board>()

        runBlocking {
            boardsCollection.find(Filters.eq(Board::userId.name, loginModel.currentUser)).collect {
                boards.add(it)
            }
        }

        return boards
    }

    override fun addBoard(board: Board) {
        board.userId = loginModel.currentUser
        val job = coroutineScope.launch {
            boardsCollection.insertOne(board)
        }
        channel.trySend(job)
    }

    override fun deleteBoard(boardId: ObjectId, noteIds: List<ObjectId>) {
        val job = coroutineScope.launch {
            // Delete all the notes associated with the board
            noteIds.forEach {
                deleteNote(it, boardId)
            }

            boardsCollection.deleteOne(Filters.eq(boardId))
        }
        channel.trySend(job)
    }

    override fun updateBoard(boardId: ObjectId, name: String, desc: String, notes: List<ObjectId>) {
        val job = coroutineScope.launch {
            boardsCollection.updateOne(
                Filters.eq(boardId),
                Updates.combine(
                    Updates.set("name", name),
                    Updates.set("desc", desc),
                    Updates.set("notes", notes),
                    Updates.set("datetimeUpdated", Instant.now().toString())
                )
            )
        }
        channel.trySend(job)
    }

    // This is specifically for updating the datetimeAccessed field
    override fun updateBoardAccessed(boardId: ObjectId,) {
        val job = coroutineScope.launch {
            boardsCollection.updateOne(
                Filters.eq(boardId),
                Updates.set("datetimeAccessed", Instant.now().toString())
            )
        }
        channel.trySend(job)
    }


    override fun readNotes(): MutableMap<ObjectId, MutableList<Note>> {
        val toRet: MutableMap<ObjectId, MutableList<Note>> = mutableMapOf()

        runBlocking {
            boardsCollection.find(Filters.eq(Board::userId.name, loginModel.currentUser)).collect { board ->
                val notesInBoard = board.notes
                println("DBSTORAGE DEBUG: All notes: $notesInBoard")
                val noteList = mutableListOf<Note>()
                notesInBoard.forEach { noteId ->
                    notesCollection.find(Filters.eq(noteId)).firstOrNull()?.let { note ->
                        noteList.add(note)
                    }
                }
                toRet[board.id] = noteList
            }
        }

        println("DBSTORAGE DEBUG: toRet: $toRet")
        return toRet
    }

    override fun addNote(board: Board, note: Note, await: Boolean) {
        note.userId = loginModel.currentUser
        val job = coroutineScope.launch {
            notesCollection.insertOne(note)

            // Add note to board as well
            boardsCollection.updateOne(
                Filters.eq(board.id),
                Updates.combine(
                    Updates.addToSet("notes", note.id),
                    Updates.set("datetimeUpdated", Instant.now().toString()),
                    Updates.set("datetimeAccessed", Instant.now().toString())
                )
            )

        }
        channel.trySend(job)
        if (await) runBlocking { job.join() }
    }

    override fun deleteNote(noteId: ObjectId, boardId: ObjectId, await: Boolean) {
        val job = coroutineScope.launch {
            // delete any content blocks from note if it is an article
            notesCollection.find(Filters.eq(noteId)).firstOrNull()?.let { noteDocument ->
                if (noteDocument.type == "article") {
                    noteDocument.contentBlocks.forEach {
                        deleteContentBlock(noteDocument.id, it, boardId)
                    }
                }
            }

            notesCollection.deleteOne(Filters.eq(noteId))

            // Remove the note's ObjectId from the board's notes array
            boardsCollection.updateOne(
                Filters.eq(boardId),
                Updates.combine(
                    Updates.pull("notes", noteId),
                    Updates.set("datetimeUpdated", Instant.now().toString()),
                    Updates.set("datetimeAccessed", Instant.now().toString())
                )
            )
        }
        channel.trySend(job)
        if (await) runBlocking { job.join() }
    }

    override fun updateNote(noteId: ObjectId, title: String, desc: String, relatedNotes: List<ObjectId>, await: Boolean) {
        val job = coroutineScope.launch {
            notesCollection.updateOne(
                Filters.eq(noteId),
                Updates.combine(
                    Updates.set("title", title),
                    Updates.set("desc", desc),
                    Updates.set("relatedNotes", relatedNotes),
                    Updates.set("datetimeUpdated", Instant.now().toString()),
                    Updates.set("datetimeAccessed", Instant.now().toString())
                )
            )

            boardsCollection.find(Filters.eq("notes", noteId)).firstOrNull()?.let { board ->
                boardsCollection.updateOne(
                    Filters.eq(board.id),
                    Updates.combine(
                        Updates.set("datetimeUpdated", Instant.now().toString()),
                        Updates.set("datetimeAccessed", Instant.now().toString())
                    )
                )
            }
        }
        channel.trySend(job)
        if (await) runBlocking { job.join() }
    }

    override fun updateNoteAccessed(noteId: ObjectId, boardId: ObjectId, await: Boolean) {
        val job = coroutineScope.launch {
            notesCollection.updateOne(
                Filters.eq(noteId),
                Updates.set("datetimeAccessed", Instant.now().toString())
            )

            boardsCollection.updateOne(
                Filters.eq(boardId),
                Updates.combine(
                    Updates.set("datetimeUpdated", Instant.now().toString()),
                    Updates.set("datetimeAccessed", Instant.now().toString())
                )
            )
        }
        channel.trySend(job)
        if (await) runBlocking { job.join() }
    }

    override fun readContentBlocks(): MutableMap<ObjectId, MutableList<ContentBlock>> {
        val toRet: MutableMap<ObjectId, MutableList<ContentBlock>> = mutableMapOf()
        runBlocking {
            notesCollection.find(Filters.eq(Board::userId.name, loginModel.currentUser)).collect { note ->
                // NOTE: i think it's ok to not check if the note is an article or not
                // just let the code go through each note regardless
                val blocksInArticle = note.contentBlocks
                println("DBSTORAGE DEBUG: All Content Blocks: $blocksInArticle")
                val contentBlockList: MutableList<ContentBlock> = mutableListOf()
                blocksInArticle.forEach { blockId ->
                    // convert each contentBlock of an article to the correct data subclass
                    contentBlocksDocumentCollection.find(Filters.eq(blockId)).firstOrNull()?.let { block ->
                        val typeStr = block.getString("blockType")
                        println("DBSTORAGE DEBUG: Content Block Type: $typeStr")
                        val blockCasted = when (typeStr) {
                            "PLAINTEXT" -> TextBlock(
                                id = block.getObjectId("_id"),
                                text = block.getString("text"),
                            )
                            "MARKDOWN" -> MarkdownBlock(
                                id = block.getObjectId("_id"),
                                text = block.getString("text"),
                            )
                            "CODE" -> CodeBlock(
                                id = block.getObjectId("_id"),
                                text = block.getString("text"),
                                language = block.getString("language"),
                            )
                            "CANVAS" -> CanvasBlock(
                                id = block.getObjectId("_id"),
                                // TODO: paths = block.getList("paths"),
                            )
                            "MATH" -> MathBlock(
                                id = block.getObjectId("_id"),
                                text = block.getString("text"),
                            )
                            else -> TextBlock(
                                id = ObjectId(),
                                text = "BAD!!!!!! THIS SHOULD NOT HAPPEN!!!!!!!",
                            )
                        }
                        contentBlockList.add(blockCasted)
                    }


                }
                toRet[note.id] = contentBlockList
            }
        }

        println("DBSTORAGE DEBUG: [ARTICLEID, CONTENTBLOCKS] MAP: $toRet")
        return toRet
    }


    override fun insertContentBlock(article: Note, contentBlock: ContentBlock, index: Int, boardId: ObjectId,
                                    await: Boolean) {
        contentBlock.userId = loginModel.currentUser
        val job = coroutineScope.launch {
            contentBlockCollection.insertOne(contentBlock)

            // get existing list of contentBlock ids in the note
            notesCollection.find(Filters.eq(article.id)).firstOrNull()?.let {articleDocument ->
                val blockIds = articleDocument.contentBlocks.toMutableList()
                // update the block with new id
                blockIds.add(index, contentBlock.id)
                // then, add back to the document (i.e. update)
                notesCollection.updateOne(
                    Filters.eq(article.id),
                    Updates.combine(
                        Updates.set("contentBlocks", blockIds),
                        Updates.set("datetimeUpdated", Instant.now().toString()),
                        Updates.set("datetimeAccessed", Instant.now().toString())
                    )
                )
            }

            boardsCollection.updateOne(
                Filters.eq(boardId),
                Updates.combine(
                    Updates.set("datetimeUpdated", Instant.now().toString()),
                    Updates.set("datetimeAccessed", Instant.now().toString())
                )
            )
            println("Insert complete")
        }
        channel.trySend(job)
        if (await) runBlocking {job.join()}
    }

    override fun addContentBlock(article: Note, contentBlock: ContentBlock, boardId: ObjectId, await: Boolean) {
        contentBlock.userId = loginModel.currentUser
        val job = coroutineScope.launch {
            // add contentBlock to collection
            contentBlockCollection.insertOne(contentBlock)

            // add contentBlock to end of article's list as well
            notesCollection.updateOne(
                Filters.eq(article.id),
                Updates.combine(
                    Updates.addToSet("contentBlocks", contentBlock.id),
                    Updates.set("datetimeUpdated", Instant.now().toString()),
                    Updates.set("datetimeAccessed", Instant.now().toString())
                )
            )

            boardsCollection.updateOne(
                Filters.eq(boardId),
                Updates.combine(
                    Updates.set("datetimeUpdated", Instant.now().toString()),
                    Updates.set("datetimeAccessed", Instant.now().toString())
                )
            )

            println("Add complete!")
        }
        channel.trySend(job)
        if (await) runBlocking{ job.join() }
    }

    override fun swapContentBlocks(articleId: ObjectId, index1: Int, index2: Int, boardId: ObjectId, await: Boolean) {
        val job = coroutineScope.launch {
            notesCollection.find(Filters.eq(articleId)).firstOrNull()?.let {articleDocument ->
                var blockIds = articleDocument.contentBlocks.toMutableList()
                // swap indices of content blocks in article field
                val temp: ObjectId = blockIds[index1]
                blockIds[index1] = blockIds[index2]
                blockIds[index2] = temp
                // then, add back to the document (i.e. update)
                notesCollection.updateOne(
                    Filters.eq(articleId),
                    Updates.combine(
                        Updates.set("contentBlocks", blockIds),
                        Updates.set("datetimeUpdated", Instant.now().toString()),
                        Updates.set("datetimeAccessed", Instant.now().toString())
                    )
                )
            }

            boardsCollection.updateOne(
                Filters.eq(boardId),
                Updates.combine(
                    Updates.set("datetimeUpdated", Instant.now().toString()),
                    Updates.set("datetimeAccessed", Instant.now().toString())
                )
            )
        }
        channel.trySend(job)
        if (await) runBlocking{ job.join() }
    }

    override fun deleteContentBlock(articleId: ObjectId, contentBlockId: ObjectId, boardId: ObjectId, await: Boolean) {
        val job = coroutineScope.launch {
            // delete content block from content block collection
            contentBlocksDocumentCollection.deleteOne(Filters.eq(contentBlockId))
            // also update the content block id array for the article
            notesCollection.updateOne(
                Filters.eq(articleId),
                Updates.combine(
                    Updates.pull("contentBlocks", contentBlockId),
                    Updates.set("datetimeUpdated", Instant.now().toString()),
                    Updates.set("datetimeAccessed", Instant.now().toString())
                )
            )

            boardsCollection.updateOne(
                Filters.eq(boardId),
                Updates.combine(
                    Updates.set("datetimeUpdated", Instant.now().toString()),
                    Updates.set("datetimeAccessed", Instant.now().toString())
                )
            )
        }
        channel.trySend(job)
        if (await) runBlocking{ job.join() }
    }

    override fun updateContentBlock (
        block: ContentBlock,
        text: String,
        pathsContent: MutableList< Path>,
        language: String,
        article: Note,
        boardId: ObjectId,
        await: Boolean
    ) {
        val now = Instant.now().toString()

        val job = coroutineScope.launch {

            // Update the content block in the content block collection
            contentBlocksDocumentCollection.updateOne(
                Filters.eq(block.id),
                Updates.combine(
                    Updates.set("text", text),
                    Updates.set("language", language),
                    // Updates.set("paths", pathsContent) // Uncomment if needed
                )
            )

            // Update the note's datetime fields
            notesCollection.updateOne(
                Filters.eq(article.id),
                Updates.combine(
                    Updates.set("datetimeUpdated", now),
                    Updates.set("datetimeAccessed", now)
                )
            )

            // Update the board's datetime fields
            boardsCollection.updateOne(
                Filters.eq(boardId),
                Updates.combine(
                    Updates.set("datetimeUpdated", now),
                    Updates.set("datetimeAccessed", now)
                )
            )
        }

        channel.trySend(job)
        if (await) runBlocking{ job.join() }
    }
}
