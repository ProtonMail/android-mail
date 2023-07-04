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

package ch.protonmail.android.uitest.util

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst

/**
 * Returns a child [SemanticsNodeInteraction] from another [SemanticsNodeInteraction]
 * by filtering the parent's children with the given [SemanticsMatcher].
 */
fun SemanticsNodeInteraction.child(matcher: () -> SemanticsMatcher): SemanticsNodeInteraction =
    onChildren().filter(matcher.invoke()).onFirst()

/**
 * Returns a [SemanticsNodeInteractionCollection] from a [SemanticsNodeInteraction]
 * by filtering the parent's children with the given [SemanticsMatcher].
 */
fun SemanticsNodeInteraction.children(matcher: () -> SemanticsMatcher): SemanticsNodeInteractionCollection =
    onChildren().filter(matcher.invoke())
