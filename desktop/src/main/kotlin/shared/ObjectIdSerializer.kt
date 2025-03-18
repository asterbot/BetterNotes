package shared

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.types.ObjectId

@Serializer(forClass = ObjectId::class)
object ObjectIdSerializer : KSerializer<ObjectId> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ObjectId")

    override fun serialize(encoder: Encoder, value: ObjectId) {
        encoder.encodeString(value.toString()) // Serializing ObjectId as a string
    }

    override fun deserialize(decoder: Decoder): ObjectId {
        val idString = decoder.decodeString()
        return ObjectId(idString) // Deserializing the string back to ObjectId
    }
}