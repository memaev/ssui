package com.ssui.mobile.ui.render

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.ssui.mobile.domain.HorizontalAlignment
import com.ssui.mobile.domain.VerticalArrangement

/**
 * Spec 4.3 / 6.4 mapping table.
 *
 * COLUMN: horizontalAlignment -> Alignment.Horizontal, verticalArrangement -> Arrangement.Vertical.
 * ROW:    horizontalAlignment -> Arrangement.Horizontal, verticalArrangement (TOP/CENTER/BOTTOM) -> Alignment.Vertical.
 * spacing -> Arrangement.spacedBy, ignored when the arrangement is SPACE_*.
 */

fun HorizontalAlignment.toColumnAlignment(): Alignment.Horizontal = when (this) {
    HorizontalAlignment.START -> Alignment.Start
    HorizontalAlignment.CENTER -> Alignment.CenterHorizontally
    HorizontalAlignment.END -> Alignment.End
}

fun VerticalArrangement.toColumnArrangement(spacingDp: Int): Arrangement.Vertical = when (this) {
    VerticalArrangement.TOP -> Arrangement.spacedBy(spacingDp.dp, Alignment.Top)
    VerticalArrangement.CENTER -> Arrangement.spacedBy(spacingDp.dp, Alignment.CenterVertically)
    VerticalArrangement.BOTTOM -> Arrangement.spacedBy(spacingDp.dp, Alignment.Bottom)
    VerticalArrangement.SPACE_BETWEEN -> Arrangement.SpaceBetween
    VerticalArrangement.SPACE_AROUND -> Arrangement.SpaceAround
    VerticalArrangement.SPACE_EVENLY -> Arrangement.SpaceEvenly
}

fun HorizontalAlignment.toRowArrangement(spacingDp: Int): Arrangement.Horizontal = when (this) {
    HorizontalAlignment.START -> Arrangement.spacedBy(spacingDp.dp, Alignment.Start)
    HorizontalAlignment.CENTER -> Arrangement.spacedBy(spacingDp.dp, Alignment.CenterHorizontally)
    HorizontalAlignment.END -> Arrangement.spacedBy(spacingDp.dp, Alignment.End)
}

/** Only TOP/CENTER/BOTTOM are meaningful for a ROW; SPACE_* fall back to TOP (spec 4.3). */
fun VerticalArrangement.toRowAlignment(): Alignment.Vertical = when (this) {
    VerticalArrangement.CENTER -> Alignment.CenterVertically
    VerticalArrangement.BOTTOM -> Alignment.Bottom
    VerticalArrangement.TOP,
    VerticalArrangement.SPACE_BETWEEN,
    VerticalArrangement.SPACE_AROUND,
    VerticalArrangement.SPACE_EVENLY,
    -> Alignment.Top
}
