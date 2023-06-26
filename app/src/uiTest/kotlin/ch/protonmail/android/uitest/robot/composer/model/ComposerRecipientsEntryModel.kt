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
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performTextInput
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerTestTags
import ch.protonmail.android.uicomponents.chips.ChipsTestTags
import ch.protonmail.android.uitest.util.ComposeTestRuleHolder
import ch.protonmail.android.uitest.util.assertions.assertEmptyText

internal sealed class ComposerRecipientsEntryModel(
    matcher: SemanticsMatcher,
    composeTestRule: ComposeTestRule = ComposeTestRuleHolder.rule
) : ComposerParticipantsEntryModel(matcher, composeTestRule) {

    private val textField = composeTestRule.onNode(
        matcher = hasTestTag(ChipsTestTags.BasicTextField) and hasAnyAncestor(matcher)
    )

    override fun typeValue(value: String) = withParentFocused {
        textField.performTextInput(value)
    }

    override fun isFocused() = apply {
        textField.assertIsFocused()
    }

    override fun hasEmptyValue() = withParentFocused {
        textField.assertEmptyText()
    }

    override fun hasValue(value: String) = withParentFocused {
        textField.assertTextEquals(value)
    }
}

internal object ToRecipientEntryModel : ComposerRecipientsEntryModel(hasTestTag(ComposerTestTags.ToRecipient))
internal object CcRecipientEntryModel : ComposerRecipientsEntryModel(hasTestTag(ComposerTestTags.CcRecipient))
internal object BccRecipientEntryModel : ComposerRecipientsEntryModel(hasTestTag(ComposerTestTags.BccRecipient))
