package com.ssui.mobile.ui.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssui.mobile.domain.Padding
import com.ssui.mobile.domain.Size
import com.ssui.mobile.domain.UiElement

/** Layout-field mapping from spec 6.4: size -> background -> padding (padding is inside the container). */
fun Modifier.ssuiContainer(element: UiElement): Modifier =
    ssuiSize(element.width, element.height)
        .ssuiBackground(element.containerColor)
        .ssuiPadding(element.padding)

fun Modifier.ssuiSize(width: Size, height: Size): Modifier {
    val withWidth = when (width) {
        Size.Fill -> fillMaxWidth()
        Size.Wrap -> wrapContentWidth()
        is Size.Fixed -> width(width.dp.dp)
    }
    return when (height) {
        Size.Fill -> withWidth.fillMaxHeight()
        Size.Wrap -> withWidth.wrapContentHeight()
        is Size.Fixed -> withWidth.height(height.dp.dp)
    }
}

fun Modifier.ssuiPadding(padding: Padding?): Modifier =
    if (padding == null) this
    else padding(
        start = padding.start.dp,
        top = padding.top.dp,
        end = padding.end.dp,
        bottom = padding.bottom.dp,
    )

/** `null` or an unparseable colour string leaves the theme background untouched. */
fun Modifier.ssuiBackground(hexColor: String?): Modifier {
    val color = hexColor.toColorOrNull() ?: return this
    return background(color)
}
