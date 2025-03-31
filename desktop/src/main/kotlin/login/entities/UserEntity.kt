package login.entities

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class User(
    @SerialName("_id")
    @Contextual var id: ObjectId,
    val userName: String,
    val passwordHash: String
)
