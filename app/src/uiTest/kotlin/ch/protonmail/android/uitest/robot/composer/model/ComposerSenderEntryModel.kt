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

import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performTextInput
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerTestTags
import ch.protonmail.android.uitest.util.assertions.assertEmptyText

// This implementation is temporary, as the final version of the Sender Field won't allow direct input in the TextField.
internal object ComposerSenderEntryModel : ComposerParticipantsEntryModel(
    parentMatcher = hasTestTag(ComposerTestTags.FromSender)
) {

    override fun typeValue(value: String) = withParentFocused {
        parent.performTextInput(value)
    }

    override fun isFocused() = apply {
        parent.assertIsFocused()
    }

    override fun hasEmptyValue() = withParentFocused {
        parent.assertEmptyText()
    }

    override fun hasValue(value: String) = withParentFocused {
        parent.assertTextEquals(value)
    }
}
