package com.ssui.mobile.ui.render

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ssui.mobile.domain.UiElement

@Composable
fun SsuiColumn(
    element: UiElement,
    onElementClick: OnElementClick,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.ssuiContainer(element),
        verticalArrangement = element.verticalArrangement.toColumnArrangement(element.spacing),
        horizontalAlignment = element.horizontalAlignment.toColumnAlignment(),
    ) {
        element.children.forEach { child ->
            RenderElement(element = child, onElementClick = onElementClick)
        }
    }
}
