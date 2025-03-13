package shared.persistence

import boards.entities.Board
import com.mongodb.MongoException
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import individual_board.entities.Note
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import shared.ConnectionManager

class DBStorage() :IPersistence {
    // Call connect() before using DB

    private val dotenv = dotenv()

    private val connectionString = dotenv["CONNECTION_STRING"]

    private val databaseName = "cs346-notes-db"

    private val uri = connectionString

    private lateinit var mongoClient: MongoClient
    private lateinit var database: MongoDatabase

    private lateinit var boardsCollection: MongoCollection<Board>
    private lateinit var notesCollection: MongoCollection<Note>

    override fun connect(): Boolean {
        try {
            mongoClient = MongoClient.create(uri)
            database = mongoClient.getDatabase(databaseName)

            boardsCollection = database.getCollection<Board>("boards")
            notesCollection = database.getCollection<Note>("notes")

            ConnectionManager.updateConnection(true)

            return true
        } catch (e: MongoException) {
            // Dummy data to avoid "lateinits not initialized" error
            mongoClient = MongoClient.create("mongodb://localhost:27017")
            database = mongoClient.getDatabase("dummy-db")

            boardsCollection = database.getCollection("dummy-boards")
            notesCollection = database.getCollection("dummy-notes")

            return false
        }

    }


    override fun readBoards(): List<Board> {
        val boards = mutableListOf<Board>()

        runBlocking {
            boardsCollection.find().collect {
                boards.add(it)
                println(it)
            }
        }

        return boards
    }

    override fun addBoard(board: Board) {
        runBlocking {
            boardsCollection.insertOne(board)
        }

    }

    override fun deleteBoard(boardId: ObjectId, noteIds: List<ObjectId>) {
        runBlocking {
            // Delete all the notes associated with the board
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
                    Updates.set("notes", notes)
                )
            )
        }
    }


    override fun readNotes(): MutableMap<ObjectId, MutableList<Note>> {
        val toRet: MutableMap<ObjectId, MutableList<Note>> = mutableMapOf()

        runBlocking {
            boardsCollection.find().collect { board ->
                val notesInBoard = board.notes
                println("All notes: $notesInBoard")
                val noteList = mutableListOf<Note>()
                notesInBoard.forEach { noteId ->
                    notesCollection.find(Filters.eq(noteId)).firstOrNull()?.let { note ->
                        noteList.add(note)
                    }
                }
                toRet[board.id] = noteList
            }
        }

        println("toRet: $toRet")
        return toRet
    }

    override fun addNote(board: Board, note: Note) {
        runBlocking {
            notesCollection.insertOne(note)

            // Add note to board as well
            boardsCollection.updateOne(
                Filters.eq(board.id),
                Updates.addToSet("notes", note.id)
            )

        }
    }

    override fun deleteNote(noteId: ObjectId, boardId: ObjectId) {
        runBlocking {
            notesCollection.deleteOne(Filters.eq(noteId))

            // Remove the note's ObjectId from the board's notes array
            boardsCollection.updateOne(
                Filters.eq(boardId),
                Updates.pull("notes", noteId)
            )
        }
    }

    override fun updateNote(noteId: ObjectId, title: String, desc: String) {
        runBlocking {
            notesCollection.updateOne(
                Filters.eq(noteId),
                Updates.combine(
                    Updates.set("title", title),
                    Updates.set("desc", desc)
                    )
            )
        }
    }

}
