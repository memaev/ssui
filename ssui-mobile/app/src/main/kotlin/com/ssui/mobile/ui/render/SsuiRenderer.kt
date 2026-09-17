package com.ssui.mobile.ui.render

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ssui.mobile.domain.Action
import com.ssui.mobile.domain.ElementType
import com.ssui.mobile.domain.UiElement

/** Callback for elements with an `onClick` action: (elementId, action). */
typealias OnElementClick = (elementId: String, action: Action) -> Unit

/**
 * Recursive renderer (spec 6.4): switches on [UiElement.type] and delegates to one composable per
 * element type. Unknown types never reach this point - the mapper drops them - so `when` is exhaustive.
 */
@Composable
fun RenderElement(
    element: UiElement,
    onElementClick: OnElementClick,
    modifier: Modifier = Modifier,
) {
    when (element.type) {
        ElementType.COLUMN -> SsuiColumn(element, onElementClick, modifier)
        ElementType.ROW -> SsuiRow(element, onElementClick, modifier)
        ElementType.TEXT -> SsuiText(element, modifier)
        ElementType.BUTTON -> SsuiButton(
            element = element,
            onClick = element.onClick?.let { action -> { onElementClick(element.id, action) } },
            modifier = modifier,
        )

        ElementType.IMAGE -> SsuiImage(
            element = element,
            onClick = element.onClick?.let { action -> { onElementClick(element.id, action) } },
            modifier = modifier,
        )
    }
}
