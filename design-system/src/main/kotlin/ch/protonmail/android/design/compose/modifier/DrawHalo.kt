/*
 * Copyright (c) 2026 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.design.compose.modifier

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

/**
 * Paints a soft, evenly spreading halo behind the node's outline — the analogue of a CSS
 * `box-shadow: 0 0 <blurRadius> <color>` with no offsets, which glows in every direction
 * instead of downwards the way an elevation shadow does.
 */
fun Modifier.drawHalo(
    color: Color,
    blurRadiusPx: Float,
    shape: Shape
) = drawWithCache {
    // Built once per size/colour/shape change rather than per frame: this draws under content that
    // animates its size and scrolls, so a Paint and a mask filter per draw pass would churn.
    val outline = shape.createOutline(size, layoutDirection, this)
    val paint = Paint().apply {
        this.color = color
        asFrameworkPaint().maskFilter = BlurMaskFilter(blurRadiusPx, BlurMaskFilter.Blur.NORMAL)
    }
    onDrawBehind {
        drawIntoCanvas { canvas: Canvas -> canvas.drawOutline(outline, paint) }
    }
}
