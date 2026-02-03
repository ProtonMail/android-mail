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

package ch.protonmail.android.navigation.transitions

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Easing
import kotlinx.coroutines.delay

/**
 * RouteTransitions – Material 3 aligned (Android)
 *
 * M3 guidance:
 * - Emphasized set for most screen transitions.
 *
 * Note: M3 Android "forward/back" uses slide + fade (no parallax)
 * Note: Top-level navigation (drawer destinations) typically uses cross-fade
 */
object RouteTransitions {
    private const val SlideFraction = 0.25f

    private fun <T> slideSpec(
        duration: Int,
        easing: Easing,
        delay: Int = 0
    ) = tween<T>(durationMillis = duration, delayMillis = delay, easing = easing)

    private fun fadeSpec(
        duration: Int,
        easing: Easing,
        delay: Int = 0
    ) = tween<Float>(durationMillis = duration, delayMillis = delay, easing = easing)

    // ---------------------- Default Transitions: Top-level screen changes (Drawer destinations) ----------------------
    fun defaultEnterTransition(): EnterTransition = fadeIn(
        animationSpec = fadeSpec(
            duration = M3Duration.Emphasized.InOut,
            easing = M3Easing.Emphasized.InOut
        )
    )

    fun defaultExitTransition(): ExitTransition = fadeOut(
        animationSpec = fadeSpec(
            duration = M3Duration.Emphasized.InOut,
            easing = M3Easing.Emphasized.InOut
        )
    )

    fun defaultPopEnterTransition(): EnterTransition = fadeIn(
        animationSpec = fadeSpec(
            duration = M3Duration.Emphasized.InOut,
            easing = M3Easing.Emphasized.InOut
        )
    )

    fun defaultPopExitTransition(): ExitTransition = fadeOut(
        animationSpec = fadeSpec(
            duration = M3Duration.Emphasized.InOut,
            easing = M3Easing.Emphasized.InOut
        )
    )
    // ---------------------- Screen forward/back (such as Mailbox <-> Conversation) ----------------------
    private fun partial(w: Int) = (w * SlideFraction).toInt()

    fun pushEnterFromRight(): EnterTransition = slideInHorizontally(
        animationSpec = slideSpec(
            duration = M3Duration.Emphasized.Enter,
            easing = M3Easing.Emphasized.Enter,
            delay = M3Duration.Emphasized.EnterDelay
        ),
        initialOffsetX = { width -> partial(width) }
    ) + fadeIn(
        animationSpec = fadeSpec(
            duration = M3Duration.Emphasized.Enter,
            easing = M3Easing.Emphasized.Enter,
            delay = M3Duration.Emphasized.EnterDelay
        )
    )

    fun pushEnterFromLeft(): EnterTransition = slideInHorizontally(
        animationSpec = slideSpec(M3Duration.Emphasized.Enter, M3Easing.Emphasized.Enter),
        initialOffsetX = { width -> -partial(width) }
    ) + fadeIn(animationSpec = fadeSpec(M3Duration.Emphasized.Enter, M3Easing.Emphasized.Enter))

    fun pushExitToLeft(): ExitTransition = slideOutHorizontally(
        animationSpec = slideSpec(M3Duration.Emphasized.Exit, M3Easing.Emphasized.Exit),
        targetOffsetX = { width -> -partial(width) }
    ) + fadeOut(animationSpec = fadeSpec(M3Duration.Emphasized.Exit, M3Easing.Emphasized.Exit))

    fun popEnterFromLeft(): EnterTransition = slideInHorizontally(
        animationSpec = slideSpec(
            duration = M3Duration.Emphasized.Enter,
            easing = M3Easing.Emphasized.Enter,
            delay = M3Duration.Emphasized.EnterDelay
        ),
        initialOffsetX = { width -> -partial(width) }
    ) + fadeIn(
        animationSpec = fadeSpec(
            duration = M3Duration.Emphasized.Enter,
            easing = M3Easing.Emphasized.Enter,
            delay = M3Duration.Emphasized.EnterDelay
        )
    )

    fun popEnterFromRight(): EnterTransition = slideInHorizontally(
        animationSpec = slideSpec(M3Duration.Emphasized.Enter, M3Easing.Emphasized.Enter),
        initialOffsetX = { width -> partial(width) }
    ) + fadeIn(animationSpec = fadeSpec(M3Duration.Emphasized.Enter, M3Easing.Emphasized.Enter))

    fun popExitToRight(): ExitTransition = slideOutHorizontally(
        animationSpec = slideSpec(M3Duration.Emphasized.Exit, M3Easing.Emphasized.Exit),
        targetOffsetX = { width -> partial(width) }
    ) + fadeOut(animationSpec = fadeSpec(M3Duration.Emphasized.Exit, M3Easing.Emphasized.Exit))

    fun popExitToLeft(): ExitTransition = slideOutHorizontally(
        animationSpec = slideSpec(M3Duration.Emphasized.Exit, M3Easing.Emphasized.Exit),
        targetOffsetX = { width -> -partial(width) }
    ) + fadeOut(animationSpec = fadeSpec(M3Duration.Emphasized.Exit, M3Easing.Emphasized.Exit))

    // ---------------------- Screen enter from bottom (such Composer Fab) ----------------------
    fun enterFromBottom(): EnterTransition = slideInVertically(
        animationSpec = slideSpec(M3Duration.Emphasized.Enter, M3Easing.Emphasized.Enter),
        initialOffsetY = { fullHeight -> fullHeight }
    ) + fadeIn(animationSpec = fadeSpec(M3Duration.Emphasized.Enter, M3Easing.Emphasized.Enter))

    fun exitToBottom(): ExitTransition = slideOutVertically(
        animationSpec = slideSpec(M3Duration.Emphasized.Exit, M3Easing.Emphasized.Exit),
        targetOffsetY = { fullHeight -> fullHeight }
    ) + fadeOut(animationSpec = fadeSpec(M3Duration.Emphasized.Exit, M3Easing.Emphasized.Exit))
}
