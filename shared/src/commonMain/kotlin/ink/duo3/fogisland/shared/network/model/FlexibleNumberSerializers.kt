package ink.duo3.fogisland.shared.network.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull

object FlexibleLongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleLong", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeLong(value)
    }

    override fun deserialize(decoder: Decoder): Long {
        if (decoder is JsonDecoder) {
            val primitive = decoder.decodeJsonElement() as? JsonPrimitive
                ?: throw SerializationException("Expected a JSON primitive")
            primitive.longOrNull?.let { return it }
            primitive.content.toLongOrNull()?.let { return it }
            throw SerializationException("Expected a Long-compatible value")
        }

        return decoder.decodeLong()
    }
}

@OptIn(ExperimentalSerializationApi::class)
object FlexibleNullableLongSerializer : KSerializer<Long?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleNullableLong", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Long?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeLong(value)
        }
    }

    override fun deserialize(decoder: Decoder): Long? {
        if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonNull) {
                return null
            }

            val primitive = element as? JsonPrimitive
                ?: throw SerializationException("Expected a JSON primitive")
            primitive.longOrNull?.let { return it }
            return primitive.content.toLongOrNull()
        }

        return decoder.decodeNullableSerializableValue(FlexibleLongSerializer)
    }
}

object FlexibleIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleInt", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }

    override fun deserialize(decoder: Decoder): Int {
        if (decoder is JsonDecoder) {
            val primitive = decoder.decodeJsonElement() as? JsonPrimitive
                ?: throw SerializationException("Expected a JSON primitive")
            primitive.content.toIntOrNull()?.let { return it }
            primitive.longOrNull
                ?.takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }
                ?.toInt()
                ?.let { return it }
            throw SerializationException("Expected an Int-compatible value")
        }

        return decoder.decodeInt()
    }
}

object FlexibleBooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleBoolean", PrimitiveKind.BOOLEAN)

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeBoolean(value)
    }

    override fun deserialize(decoder: Decoder): Boolean {
        if (decoder is JsonDecoder) {
            val primitive = decoder.decodeJsonElement() as? JsonPrimitive
                ?: throw SerializationException("Expected a JSON primitive")
            primitive.booleanOrNull?.let { return it }

            return when (primitive.content.trim().lowercase()) {
                "1", "true" -> true
                "0", "false" -> false
                else -> throw SerializationException("Expected a Boolean-compatible value")
            }
        }

        return decoder.decodeBoolean()
    }
}

@OptIn(ExperimentalSerializationApi::class)
object FlexibleNullableIntSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleNullableInt", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Int?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeInt(value)
        }
    }

    override fun deserialize(decoder: Decoder): Int? {
        if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonNull) {
                return null
            }

            val primitive = element as? JsonPrimitive
                ?: throw SerializationException("Expected a JSON primitive")
            primitive.content.toIntOrNull()?.let { return it }
            return primitive.longOrNull
                ?.takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }
                ?.toInt()
        }

        return decoder.decodeNullableSerializableValue(FlexibleIntSerializer)
    }
}

@OptIn(ExperimentalSerializationApi::class)
object FlexibleNullableBooleanSerializer : KSerializer<Boolean?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleNullableBoolean", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Boolean?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeBoolean(value)
        }
    }

    override fun deserialize(decoder: Decoder): Boolean? {
        if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonNull) {
                return null
            }

            val primitive = element as? JsonPrimitive
                ?: throw SerializationException("Expected a JSON primitive")
            primitive.booleanOrNull?.let { return it }

            return when (primitive.content.trim().lowercase()) {
                "1", "true" -> true
                "0", "false" -> false
                else -> null
            }
        }

        return decoder.decodeNullableSerializableValue(FlexibleBooleanSerializer)
    }
}
