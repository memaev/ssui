package com.ssui.mobile.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Deserializes `children` element by element. A child whose JSON is malformed (wrong field type,
 * missing required padding side, ...) is dropped and reported through [DtoParseIssueReporter]
 * instead of failing the whole screen (spec 6.4: "An element whose JSON fails to parse is dropped
 * and logged; the rest of the screen still renders").
 *
 * Only the Json format supports this; other formats fall back to the strict list serializer.
 */
object LenientUiElementListSerializer : KSerializer<List<UiElementDto>> {

    // Lazy on purpose: UiElementDto's generated serializer references this object, so eager
    // initialization would create a static-init cycle.
    private val delegate: KSerializer<List<UiElementDto>> by lazy { ListSerializer(UiElementDto.serializer()) }

    override val descriptor: SerialDescriptor get() = delegate.descriptor

    override fun serialize(encoder: Encoder, value: List<UiElementDto>) = delegate.serialize(encoder, value)

    override fun deserialize(decoder: Decoder): List<UiElementDto> {
        val jsonDecoder = decoder as? JsonDecoder ?: return delegate.deserialize(decoder)
        val array = jsonDecoder.decodeJsonElement() as? JsonArray
            ?: throw SerializationException("'children' must be a JSON array")
        return array.mapNotNull { child ->
            try {
                jsonDecoder.json.decodeFromJsonElement(UiElementDto.serializer(), child)
            } catch (e: SerializationException) {
                DtoParseIssueReporter.report("Dropping element ${child.describe()}: ${e.message}")
                null
            } catch (e: IllegalArgumentException) {
                DtoParseIssueReporter.report("Dropping element ${child.describe()}: ${e.message}")
                null
            }
        }
    }

    private fun kotlinx.serialization.json.JsonElement.describe(): String {
        val obj = this as? JsonObject ?: return "<non-object>"
        val id = (obj["id"] as? JsonPrimitive)?.content ?: "?"
        val type = (obj["type"] as? JsonPrimitive)?.content ?: "?"
        return "id=$id type=$type"
    }
}

/**
 * Sink for parse-time issues. Serializers cannot receive injected dependencies, so the
 * application wires a real logger here at startup; tests may install a recording sink.
 */
object DtoParseIssueReporter {
    @Volatile
    var sink: ((String) -> Unit)? = null

    fun report(message: String) {
        sink?.invoke(message)
    }
}
