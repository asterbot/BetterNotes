package individual_board.entities

import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.codecs.kotlinx.ObjectIdSerializer
import org.bson.types.ObjectId
import java.time.Instant
import java.util.*

/* Notes */

@Serializable
data class Note @OptIn(ExperimentalSerializationApi::class) constructor(
    @SerialName("_id")
    @Contextual var id: ObjectId,
    var title: String,
    var desc: String = "",
    var type: String? = "section", // options: "article", "section"
    @Contextual var contentBlocks: List<@Serializable(with = ObjectIdSerializer::class) ObjectId> = mutableListOf(),
    @Contextual var relatedNotes: List<@Serializable(with = ObjectIdSerializer::class) ObjectId> = mutableListOf(),
    var datetimeCreated: String = Instant.now().toString(),
    var datetimeUpdated: String = Instant.now().toString(),
    var datetimeAccessed: String = Instant.now().toString(),
    var userId: String = "dummy-user"
)


fun MutableList<Note>.addNote(element: Note): Boolean {
    this.add(element)
    return true
}

fun MutableList<Note>.removeNote(element: Note): Boolean {
    this.remove(element)
    return true
}
