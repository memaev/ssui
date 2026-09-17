package com.ssui.mobile.ui.render

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ssui.mobile.domain.UiElement

@Composable
fun SsuiRow(
    element: UiElement,
    onElementClick: OnElementClick,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.ssuiContainer(element),
        horizontalArrangement = element.horizontalAlignment.toRowArrangement(element.spacing),
        verticalAlignment = element.verticalArrangement.toRowAlignment(),
    ) {
        element.children.forEach { child ->
            RenderElement(element = child, onElementClick = onElementClick)
        }
    }
}
