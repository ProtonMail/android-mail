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

package ch.protonmail.android.uitest.robot.composer.model

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performTextInput
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerTestTags
import ch.protonmail.android.uitest.util.assertions.assertEmptyText

internal sealed class ComposerParticipantEntryModel(
    matcher: SemanticsMatcher,
    index: Int,
    composeTestRule: ComposeTestRule
) {

    private val prefix = composeTestRule.onAllNodesWithTag(
        testTag = ComposerTestTags.FieldPrefix,
        useUnmergedTree = true
    )[index]

    private val field = composeTestRule.onNode(matcher, useUnmergedTree = true)

    // region actions
    fun typeValue(value: String) = apply {
        field.performTextInput(value)
    }
    // endregion

    // region verification
    fun isFocused() = apply {
        field.assertIsFocused()
    }

    fun hasPrefix(value: String) = apply {
        prefix.assertTextEquals(value)
    }

    fun hasEmptyValue() = apply {
        field.assertEmptyText()
    }

    fun hasValue(value: String) = apply {
        field.assertTextEquals(value)
    }
    // endregion
}

internal class SenderParticipantEntryModel(composeTestRule: ComposeTestRule) : ComposerParticipantEntryModel(
    matcher = hasTestTag(ComposerTestTags.FromSender),
    index = 0,
    composeTestRule = composeTestRule
)

internal class ToParticipantEntryModel(composeTestRule: ComposeTestRule) : ComposerParticipantEntryModel(
    matcher = hasTestTag(ComposerTestTags.ToRecipient),
    index = 1,
    composeTestRule = composeTestRule
)
