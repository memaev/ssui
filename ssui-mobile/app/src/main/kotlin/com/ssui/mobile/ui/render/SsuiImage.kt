package com.ssui.mobile.ui.render

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.ssui.mobile.domain.UiElement

@Composable
fun SsuiImage(
    element: UiElement,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = element.imageContent,
        contentDescription = element.contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .ssuiSize(element.width, element.height)
            .ssuiBackground(element.containerColor)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .ssuiPadding(element.padding),
    )
}
