package com.ssui.mobile.ui.render

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ssui.mobile.domain.UiElement

@Composable
fun SsuiButton(
    element: UiElement,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick ?: {},
        enabled = onClick != null,
        // The button draws its own container, so padding acts as an outer inset here.
        modifier = modifier
            .ssuiPadding(element.padding)
            .ssuiSize(element.width, element.height),
        colors = ButtonDefaults.buttonColors(
            containerColor = element.containerColor.toColorOrNull() ?: Color.Unspecified,
            contentColor = element.contentColor.toColorOrNull() ?: Color.Unspecified,
        ),
    ) {
        Text(text = element.textContent.orEmpty())
    }
}
