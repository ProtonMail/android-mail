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

import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel

fun SemanticsNodeInteractionsProvider.onAllNodesWithText(
    text: TextUiModel,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false
): SemanticsNodeInteractionCollection = onAllNodesWithText(getString(text), substring, ignoreCase, useUnmergedTree)

fun SemanticsNodeInteractionsProvider.onAllNodesWithText(
    @StringRes textRes: Int,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false
): SemanticsNodeInteractionCollection = onAllNodesWithText(getString(textRes), substring, ignoreCase, useUnmergedTree)

fun SemanticsNodeInteractionsProvider.onNodeWithContentDescription(
    @StringRes labelRes: Int,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false
): SemanticsNodeInteraction = onNodeWithContentDescription(getString(labelRes), substring, ignoreCase, useUnmergedTree)

fun SemanticsNodeInteractionsProvider.onAllNodesWithContentDescription(
    @StringRes labelRes: Int,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false
): SemanticsNodeInteractionCollection = onAllNodesWithContentDescription(
    label = getString(labelRes),
    substring = substring,
    ignoreCase = ignoreCase,
    useUnmergedTree = useUnmergedTree
)

fun SemanticsNodeInteractionsProvider.onNodeWithText(
    text: TextUiModel,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false
): SemanticsNodeInteraction = onNodeWithText(getString(text), substring, ignoreCase, useUnmergedTree)

fun SemanticsNodeInteractionsProvider.onNodeWithText(
    @StringRes textRes: Int,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false
): SemanticsNodeInteraction = onNodeWithText(getString(textRes), substring, ignoreCase, useUnmergedTree)
