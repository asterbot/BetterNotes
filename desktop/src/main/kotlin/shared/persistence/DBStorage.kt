package shared.persistence

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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.firstOrNull
import login.entities.User
import org.bson.BsonDocument
import org.bson.BsonInt64
import org.bson.Document
import org.bson.types.ObjectId
import org.mindrot.jbcrypt.BCrypt
import shared.ConnectionManager
import shared.ConnectionStatus
import shared.loginModel
import java.time.Instant

class DBStorage(private var databaseName: String = "cs346-users-db") :IPersistence {
    // call connect() before using DB

    private val dotenv = dotenv()

    private val connectionString = dotenv["CONNECTION_STRING"]

    private val uri = connectionString

    private lateinit var mongoClient: MongoClient
    private lateinit var database: MongoDatabase

    private lateinit var boardsCollection: MongoCollection<Board>
    private lateinit var notesCollection: MongoCollection<Note>

    // we need both collections since reading to polymorphic types requires de-serializing
    //   however adding can be done as is done for the other types
    private lateinit var contentBlocksDocumentCollection: MongoCollection<Document>
    private lateinit var contentBlockCollection: MongoCollection<ContentBlock>

    private lateinit var usersCollection: MongoCollection<User>


    // to make everything run in a coroutine!
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // this ensures mutual exclusion
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

    override suspend fun clearDB(){
        boardsCollection.deleteMany(BsonDocument())
        notesCollection.deleteMany(BsonDocument())
        contentBlockCollection.deleteMany(BsonDocument())
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


    override fun updatePassword(oldPassword: String, newPassword: String, username: String): Boolean {
        val userData: User?
        runBlocking {
            userData = usersCollection.find(Filters.eq(User::userName.name, username)).firstOrNull()
        }
        if (!BCrypt.checkpw(oldPassword, userData?.passwordHash)){
            // old password does not match!
            return false
        }
        runBlocking {
            usersCollection.updateOne(
                Filters.eq(User::userName.name, username),
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
        if (userData != null) {
            // i won't let the user take dummy-user because that's our placeholder name :D
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
        // return whether password matches record in DB
        return BCrypt.checkpw(password, userData.passwordHash)
    }

    override fun deleteUser(password: String, username:String): Boolean {
        val userData: User?
        runBlocking {
            userData = usersCollection.find(Filters.eq(User::userName.name, username)).firstOrNull()
        }
        if (!BCrypt.checkpw(password, userData?.passwordHash)){
            // old password does not match!
            return false
        }

        runBlocking {
            boardsCollection.deleteMany(Filters.eq(Board::userId.name, username))
            notesCollection.deleteMany(Filters.eq(Note::userId.name, username))
            contentBlockCollection.deleteMany(Filters.eq(Note::userId.name, username))
            usersCollection.deleteOne(Filters.eq(User::userName.name, username))
        }
        return true
    }

    override fun readUsers(): List<User> {
        val users = mutableListOf<User>()
        runBlocking {
            usersCollection.find().collect {
                users.add(it)
            }
        }
        return users
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
        runBlocking {
            boardsCollection.insertOne(board)
        }
    }

    override fun deleteBoard(boardId: ObjectId, noteIds: List<ObjectId>) {
        runBlocking {
            // delete all the notes associated with the board
            noteIds.forEach {
                deleteNote(it, boardId)
            }
            boardsCollection.deleteOne(Filters.eq(boardId))
        }
    }

    override fun updateBoard(boardId: ObjectId, name: String, desc: String, notes: List<ObjectId>) {
        runBlocking {
            boardsCollection.updateOne(
                Filters.eq(boardId),
                Updates.combine(
                    Updates.set("name", name),
                    Updates.set("desc", desc),
                    Updates.set("notes", notes),
                    Updates.set("datetimeUpdated", Instant.now().toString()),
                    Updates.set("datetimeAccessed", Instant.now().toString())
                )
            )
        }
    }

    // this is specifically for updating the datetimeAccessed field
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
                val noteList = mutableListOf<Note>()
                notesInBoard.forEach { noteId ->
                    notesCollection.find(Filters.eq(noteId)).firstOrNull()?.let { note ->
                        noteList.add(note)
                    }
                }
                toRet[board.id] = noteList
            }
        }
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
                noteDocument.contentBlocks.forEach {
                    deleteContentBlock(noteDocument.id, it, boardId)
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

    override fun updateNote(noteId: ObjectId, title: String, desc: String, relatedNotes: List<ObjectId>, tagColor:String, await: Boolean) {
        val job = coroutineScope.launch {
            notesCollection.updateOne(
                Filters.eq(noteId),
                Updates.combine(
                    Updates.set("title", title),
                    Updates.set("desc", desc),
                    Updates.set("tag", tagColor),
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
                val contentBlockList: MutableList<ContentBlock> = mutableListOf()
                var prevBlockGlued = false
                blocksInArticle.forEach { blockId ->
                    // convert each contentBlock of an article to the correct data subclass
                    contentBlocksDocumentCollection.find(Filters.eq(blockId)).firstOrNull()?.let { block ->
                        val typeStr = block.getString("blockType")
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
                            "CANVAS" ->{
                                val bList = block["bList"] as? List<*>
                                println(bList!!::class)
                                val byteList = bList.mapNotNull {
                                    (it as? Number)?.toByte()
                                }.toMutableList()
                                CanvasBlock(
                                    id = block.getObjectId("_id"),
                                    bList = byteList,
                                    canvasHeight = block.getInteger("canvasHeight")
                                )
                            }
                            "MATH" -> MathBlock(
                                id = block.getObjectId("_id"),
                                text = block.getString("text"),
                            )
                            "MEDIA" -> {
                                val bList = block["bList"] as? List<*>
                                println(bList!!::class)
                                val byteList = bList.mapNotNull {
                                    (it as? Number)?.toByte()
                                }.toMutableList()
                                MediaBlock(
                                    id = block.getObjectId("_id"),
                                    bList = byteList
                                )
                            }
                            else -> TextBlock(
                                id = ObjectId(),
                                text = "BAD!!!!!! THIS SHOULD NOT HAPPEN!!!!!!!",
                            )
                        }
                        // provide some glue safety (i.e. neighbouring blocks are either both glued or both not glued)
                        blockCasted.gluedAbove = prevBlockGlued
                        blockCasted.gluedBelow = block.getBoolean("gluedBelow")
                        prevBlockGlued = blockCasted.gluedBelow

                        contentBlockList.add(blockCasted)
                    }
                }
                toRet[note.id] = contentBlockList
            }
        }
        return toRet
    }

    override fun updateGlueStatus(
        contentBlockId: ObjectId,
        gluedAbove: Boolean,
        gluedBelow: Boolean,
        articleId: ObjectId,
        boardId: ObjectId,
        await: Boolean
    ) {
        val now = Instant.now().toString()
        val job = coroutineScope.launch {

            // update the content block in the content block collection
            contentBlocksDocumentCollection.updateOne(
                Filters.eq(contentBlockId),
                Updates.combine(
                    Updates.set("gluedAbove", gluedAbove),
                    Updates.set("gluedBelow", gluedBelow)
                )
            )
            // update the note's datetime fields
            notesCollection.updateOne(
                Filters.eq(articleId),
                Updates.combine(
                    Updates.set("datetimeUpdated", now),
                    Updates.set("datetimeAccessed", now)
                )
            )
            // update the board's datetime fields
            boardsCollection.updateOne(
                Filters.eq(boardId),
                Updates.combine(
                    Updates.set("datetimeUpdated", now),
                    Updates.set("datetimeAccessed", now)
                )
            )
        }
        if (await) runBlocking{ job.join() }
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
        }
        channel.trySend(job)
        if (await) runBlocking{ job.join() }
    }

    override fun swapContentBlocks(articleId: ObjectId, upperBlockStart: Int, upperBlockEnd: Int,
                                   lowerBlockStart: Int, lowerBlockEnd: Int, boardId: ObjectId, await: Boolean) {
        val job = coroutineScope.launch {
            notesCollection.find(Filters.eq(articleId)).firstOrNull()?.let {articleDocument ->
                var blockIds = articleDocument.contentBlocks.toMutableList()

                // swap ids between upper and lower blocks
                for (offset in 0..lowerBlockEnd-lowerBlockStart) {
                    val toMoveIndex = lowerBlockStart + offset
                    val toInsertIndex = upperBlockStart + offset
                    val idToMove = blockIds[toMoveIndex]
                    blockIds.removeAt(toMoveIndex)
                    blockIds.add(toInsertIndex, idToMove)
                }
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
        canvasHeight: Int,
        bList: MutableList<Byte>,
        language: String,
        gluedAbove: Boolean,
        gluedBelow: Boolean,
        article: Note,
        boardId: ObjectId,
        await: Boolean
    ) {
        val now = Instant.now().toString()


        val job = coroutineScope.launch {
            // update the content block in the content block collection
            contentBlocksDocumentCollection.updateOne(
                Filters.eq(block.id),
                Updates.combine(
                    Updates.set("text", text),
                    Updates.set("canvasHeight", canvasHeight),
                    Updates.set("language", language),
                    Updates.set("bList", bList),
                    Updates.set("gluedAbove", gluedAbove),
                    Updates.set("gluedBelow", gluedBelow),
                )
            )
            // update the note's datetime fields
            notesCollection.updateOne(
                Filters.eq(article.id),
                Updates.combine(
                    Updates.set("datetimeUpdated", now),
                    Updates.set("datetimeAccessed", now)
                )
            )
            // update the board's datetime fields
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
