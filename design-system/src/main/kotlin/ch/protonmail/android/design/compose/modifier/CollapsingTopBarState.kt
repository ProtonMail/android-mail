/*
 * Copyright (c) 2022 Proton Technologies AG
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

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Scroll-linked collapse state for a top bar.
 *
 * The bar follows the scroll one to one: it gives way as the content scrolls down and comes back as
 * it scrolls up, then settles to the nearest end state when the gesture ends.
 */
@Stable
class CollapsingTopBarState(initialOffsetPx: Float = 0f) {

    /** Current vertical offset of the collapsible content, in `[-collapsibleHeightPx, 0]`. */
    var offsetPx by mutableFloatStateOf(initialOffsetPx)
        private set

    /** Height of the collapsible content. */
    private var collapsibleHeightPx: Float = 0f

    /** Offset applied by the last layout pass, in pixels. Mirrors [offsetPx] outside the snapshot. */
    private var appliedOffsetPx: Int = 0

    /** When false the bar stays put and no scroll delta is consumed. */
    private var enabled: Boolean = true

    /** Guards the collapse direction only. The bar can always come back. */
    private var canCollapse: () -> Boolean = { true }

    /** Serialises the settle and expand animations so they never drive the offset at the same time. */
    private val animationMutex = MutatorMutex()

    val nestedScrollConnection = object : NestedScrollConnection {

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val height = collapsibleHeightPx
            val delta = available.y
            val blocked = !enabled || height <= 0f || delta < 0f && !canCollapse()
            if (blocked) return Offset.Zero

            val current = offsetPx.coerceIn(-height, 0f)
            val target = (current + delta).coerceIn(-height, 0f)
            val consumed = target - current
            if (consumed == 0f) return Offset.Zero

            offsetPx = target
            return Offset(0f, consumed)
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            settle()
            return Velocity.Zero
        }
    }

    /**
     * Updates the inputs the scroll handling depends on. Call it from a [androidx.compose.runtime.SideEffect],
     * so the values match the last composition without making them observable.
     */
    fun configure(enabled: Boolean, canCollapse: () -> Boolean) {
        this.enabled = enabled
        this.canCollapse = canCollapse
    }

    /**
     * Expanded height of a container measured while the bar is partly collapsed. Callers report the
     * height they see, this adds back what the collapse took away.
     */
    fun expandedHeightPx(measuredHeightPx: Int): Int = measuredHeightPx - appliedOffsetPx

    /** Brings a collapsed bar back, for when the user can no longer scroll it back into view. */
    suspend fun animateExpand() {
        val current = offsetPx
        if (current == 0f) return

        animateOffsetTo(target = 0f, durationMillis = ExpandDurationMs)
    }

    /** Called from [collapsingContainer] on every layout pass. Writes plain fields only. */
    internal fun onLayout(collapsibleHeightPx: Float, appliedOffsetPx: Int) {
        this.collapsibleHeightPx = collapsibleHeightPx
        this.appliedOffsetPx = appliedOffsetPx
    }

    /** Animates a half-collapsed bar to the closest end state once the gesture is over. */
    private suspend fun settle() {
        val height = collapsibleHeightPx
        if (!enabled || height <= 0f) return

        val current = offsetPx.coerceIn(-height, 0f)
        if (current == 0f || current == -height) return

        val target = if (abs(current) < height / 2f) 0f else -height
        animateOffsetTo(target = target, durationMillis = SettleDurationMs)
    }

    private suspend fun animateOffsetTo(target: Float, durationMillis: Int) = animationMutex.mutate {
        animate(
            initialValue = offsetPx,
            targetValue = target,
            animationSpec = tween(durationMillis = durationMillis)
        ) { value, _ -> offsetPx = value }
    }

    companion object {

        private const val SettleDurationMs = 150
        private const val ExpandDurationMs = 200

        val Saver: Saver<CollapsingTopBarState, Float> = Saver(
            save = { it.offsetPx },
            restore = { CollapsingTopBarState(it) }
        )
    }
}

@Composable
fun rememberCollapsingTopBarState(): CollapsingTopBarState =
    rememberSaveable(saver = CollapsingTopBarState.Saver) { CollapsingTopBarState() }

/**
 * Shifts the content up by [CollapsingTopBarState.offsetPx] and shrinks the space it takes by the
 * same amount, so whatever sits below it moves in step with the collapse.
 */
fun Modifier.collapsingContainer(state: CollapsingTopBarState) = this
    .clipToBounds()
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val height = placeable.height.toFloat()
        val offset = state.offsetPx.coerceIn(-height, 0f).roundToInt()
        state.onLayout(height, offset)
        layout(placeable.width, placeable.height + offset) {
            placeable.place(0, offset)
        }
    }
