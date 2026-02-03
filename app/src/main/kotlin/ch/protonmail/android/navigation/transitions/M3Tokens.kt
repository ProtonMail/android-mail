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

/*
 * Material 3 – Suggested easing & duration pairs
 *
 * Easing                     Duration   Transition type
 * ------------------------------------------------------------
 * Emphasized                 500ms      Begin and end on screen
 * Emphasized decelerate      400ms      Enter the screen
 * Emphasized accelerate      200ms      Exit the screen
 *
 * Standard                   300ms      Begin and end on screen
 * Standard decelerate        250ms      Enter the screen
 * Standard accelerate        200ms      Exit the screen
 *
 * Notes:
 * - Emphasized set is recommended for most expressive, full-screen transitions.
 * - Standard set is suitable for small, utility-focused or top-level transitions.
 * - Enter transitions are longer than exit transitions to guide user attention.
 */

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Material 3 motion easing tokens.
 *
 * InOut = Begin and end on screen
 * Enter: Enter the screen
 * Exit: Exit the screen
 */
object M3Easing {

    object Standard {
        // md.sys.motion.easing.standard
        val InOut: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

        // md.sys.motion.easing.standard.decelerate
        val Enter: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)

        // md.sys.motion.easing.standard.accelerate
        val Exit: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)
    }

    object Emphasized {
        // Fallback for md.sys.motion.easing.emphasized
        val InOut: Easing = Standard.InOut

        // md.sys.motion.easing.emphasized.decelerate
        val Enter: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

        // md.sys.motion.easing.emphasized.accelerate
        val Exit: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    }
}

/**
 * Material 3 Suggested durations
 */
object M3Duration {

    object Emphasized {
        const val InOut = 500
        const val Enter = 400
        const val Exit = 200

        const val EnterDelay = 80
    }

    object Standard {
        const val InOut = 300
        const val Enter = 250
        const val Exit = 200

        const val EnterDelay = 60
    }
}
