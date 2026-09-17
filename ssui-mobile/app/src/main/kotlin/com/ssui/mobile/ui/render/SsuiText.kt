package com.ssui.mobile.ui.render

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ssui.mobile.domain.UiElement

@Composable
fun SsuiText(
    element: UiElement,
    modifier: Modifier = Modifier,
) {
    Text(
        text = element.textContent.orEmpty(),
        // Unspecified -> Material's LocalContentColor, i.e. the theme colour (spec 6.4).
        color = element.contentColor.toColorOrNull() ?: Color.Unspecified,
        modifier = modifier.ssuiContainer(element),
    )
}
