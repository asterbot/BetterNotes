package boards.entities

import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.codecs.kotlinx.ObjectIdSerializer
import org.bson.types.ObjectId
import java.time.Instant

// entities holds the main data for the class and provides manipulator functions

@Serializable
data class Board @OptIn(ExperimentalSerializationApi::class) constructor(
    @SerialName("_id")
    @Contextual var id: ObjectId,
    var name: String,
    var desc: String,
    // for some reason we need to serialize each objectId independently when storing in lists?
    @Contextual var notes: List<@Serializable(with = ObjectIdSerializer::class) ObjectId> = mutableListOf(),
    var datetimeCreated: String = Instant.now().toString(),
    var datetimeUpdated: String = Instant.now().toString(),
    var datetimeAccessed: String = Instant.now().toString(),
    var userId: String = "dummy-user"
)

fun MutableList<Board>.addBoard(element: Board): Boolean {
    this.add(element)
    return true
}

fun MutableList<Board>.removeBoard(element: Board): Boolean {
    this.remove(element)
    return true
}

fun MutableList<Board>.updateBoard(element: Board, name: String, desc: String): Boolean {
    val index = this.indexOf(element)
    if (index == -1) return false
    this[index] = element.copy(name = name, desc = desc)
    this[index].datetimeUpdated = Instant.now().toString()
    this[index].datetimeAccessed = Instant.now().toString()
    return true
}
