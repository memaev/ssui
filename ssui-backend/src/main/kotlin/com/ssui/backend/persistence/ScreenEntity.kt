package com.ssui.backend.persistence

import com.ssui.backend.api.ActionType
import com.ssui.backend.api.ElementType
import com.ssui.backend.api.HorizontalAlignment
import com.ssui.backend.api.SizeMode
import com.ssui.backend.api.VerticalArrangement
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

/**
 * One MongoDB document per screen in the `screens` collection; the whole element tree is embedded.
 * `id` is the UUID string stored as `_id`.
 */
@Document(collection = ScreenEntity.COLLECTION)
data class ScreenEntity(
    @Id
    val id: String,
    @Indexed(unique = true)
    val name: String,
    val root: UiElementEntity,
) {
    companion object {
        const val COLLECTION = "screens"
    }
}

/**
 * Embedded element. `@Field("id")` is required: without it Spring Data treats a property called `id`
 * as the document id and would persist it as `_id`, breaking the spec document shape (4.3/4.5).
 */
data class UiElementEntity(
    @Field("id")
    val id: String,
    val type: ElementType,
    val children: List<UiElementEntity> = emptyList(),
    val textContent: String? = null,
    val imageContent: String? = null,
    val contentDescription: String? = null,
    val containerColor: String? = null,
    val contentColor: String? = null,
    val padding: PaddingEntity? = null,
    val width: SizeEntity? = null,
    val height: SizeEntity? = null,
    val horizontalAlignment: HorizontalAlignment? = null,
    val verticalArrangement: VerticalArrangement? = null,
    val spacing: Int? = null,
    val onClick: ActionEntity? = null,
)

data class PaddingEntity(
    val start: Int,
    val top: Int,
    val end: Int,
    val bottom: Int,
)

data class SizeEntity(
    val mode: SizeMode,
    val dp: Int? = null,
)

data class ActionEntity(
    val type: ActionType,
    val payload: Map<String, String> = emptyMap(),
)
