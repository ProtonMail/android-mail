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

package ch.protonmail.android.uitest.util.assertions

import android.util.Log
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.printToString

/**
 * Traverses the UI hierarchy starting from the receiving [SemanticsNodeInteraction] to find a
 * matching child with the given [SemanticsMatcher].
 *
 * @param matcher the matcher to match the child element with.
 * @param maxDepthLevel the maximum level of depth.
 *
 * @throws AssertionError if no child is found within the [maxDepthLevel].
 */
fun SemanticsNodeInteraction.hasAnyChildWith(matcher: SemanticsMatcher, maxDepthLevel: Int = 5) {
    val logTag = "SemanticsNodeInteraction#hasAnyChildWith"
    var child = onChild()

    for (attempt in 1.rangeTo(maxDepthLevel)) {
        try {
            child.assert(matcher)
            Log.d(logTag, "Found matching element at attempt $attempt.")
            return
        } catch (e: AssertionError) {
            Log.d(logTag, "Attempt #$attempt - Unable to find child with matcher '$matcher'.")
            Log.d(logTag, "${e.message}")
            child = child.onChild()
        }
    }

    throw AssertionError(
        "Unable to find any child of the given node within $maxDepthLevel level(s) " +
            "of depth with matcher '${matcher.description}'."
    ).also {
        Log.e(logTag, "Dumping first ancestor hierarchy...\n\n${printToString()}")
    }
}
