package shared.persistence

import androidx.compose.ui.graphics.Path
import article.entities.ContentBlock
import boards.entities.Board
import individual_board.entities.Note
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import java.util.*

/*
Queue for the operations that happened while offline, and how to run them when back online
*/


sealed class Operation{
//    abstract val id: ObjectId
    abstract val persistence: IPersistence

    companion object {
        // Keeps track of all contentBlocks inserted to avoid race conditions between Create/Update
        // NOTE: kind of a hack, but works!
        val contentBlocksInserted = mutableListOf<ObjectId>()
    }


    abstract fun updateDB()
}

data class Create(override val persistence: IPersistence, val objToAdd:Any,
                  val boardDependency:Board? = null,
                  val noteDependency:Note? = null,
                  val indexDependency: Int? = null
    )
    :Operation(){


    override fun updateDB(){
        when (objToAdd){
            is Board -> {
                persistence.addBoard(objToAdd)

            }
            is Note -> {
                // Must have the board it is a part of
                assert(boardDependency != null)
                persistence.addNote(boardDependency as Board, objToAdd, await = true)

            }
            is ContentBlock -> {
                // Must have the note it is a part of
                assert(noteDependency != null)
                assert(boardDependency != null)

                contentBlocksInserted.add(objToAdd.id)

                if (indexDependency == null) {
                    persistence.addContentBlock(noteDependency as Note, objToAdd, (boardDependency as Board).id, await = true)
                }
                else {
                    persistence.insertContentBlock(
                        noteDependency as Note, objToAdd, indexDependency,
                        (boardDependency as Board).id,
                        await = true
                    )
                }

            }

        }
    }
}

data class Delete(override val persistence: IPersistence, val objToRemove: Any,
        val noteListDependency: List<ObjectId>? = null,
        val boardDependency:Board? = null,
        val noteDependency:Note? = null,
    )
    : Operation(){

    override fun updateDB(){
        println("SYNC DEBUG: Object to remove: $objToRemove")
        when (objToRemove){
            is Board -> {
                assert(noteListDependency != null)
                persistence.deleteBoard(objToRemove.id, (noteListDependency as List<ObjectId>))
            }
            is Note -> {
                assert(boardDependency != null)
                persistence.deleteNote(objToRemove.id, (boardDependency!!.id), await = true)
            }
            is ContentBlock -> {
                assert(noteDependency != null)
                assert(boardDependency != null)
                persistence.deleteContentBlock(noteDependency!!.id, objToRemove.id, boardDependency!!.id, await = true)
            }
        }
    }
}

class Update(override val persistence: IPersistence, val objToUpdate: Any,
    val fields: Map<String, Any> // maps field name to new values!
    )
    : Operation(){

    override fun updateDB(){
        when (objToUpdate){
            is Board -> {
                assert(fields.containsKey("name"))
                assert(fields.containsKey("desc"))
                assert(fields.containsKey("notes"))
                    persistence.updateBoard(
                        objToUpdate.id,
                        (fields["name"] as String),
                        (fields["desc"] as String),
                        (fields["notes"] as MutableList<ObjectId>))
                }

            is Note -> {
                assert(fields.containsKey("title"))
                assert(fields.containsKey("desc"))
                    persistence.updateNote(objToUpdate.id, (fields["title"] as String), (fields["desc"] as String), await = true)
            }

            is ContentBlock -> {
                assert(fields.containsKey("text"))
                assert(fields.containsKey("pathsContent"))
                assert(fields.containsKey("language"))
                assert(fields.containsKey("article"))
                assert(fields.containsKey("boardId"))
                if (objToUpdate.id !in contentBlocksInserted){
                    // If it is newly inserted, do not update here too!
                    persistence.updateContentBlock(objToUpdate,
                        fields["text"] as String,
                        fields["pathsContent"] as MutableList<Path>,
                        fields["language"] as String,
                        fields["article"] as Note,
                        fields["boardId"] as ObjectId,
                        await = true)

                }
                else{
                    println("Not executed!")
                }
            }

        }
        }


    }

class DBQueue {
    private var operationQueue: Queue<Operation> = LinkedList()

    fun addToQueue(operation: Operation){
        operationQueue.add(operation)
        println("SYNC DEBUG: Added to queue, new queue: $operationQueue")
    }

    fun syncDB(){
        println("SYNC DEBUG: Doing sync now")
        while (operationQueue.isNotEmpty()){
            val operation = operationQueue.poll() // removes first item from queue and fetches it
            println("SYNC DEBUG: Operation is $operation")
            operation.updateDB()
        }
    }

}
