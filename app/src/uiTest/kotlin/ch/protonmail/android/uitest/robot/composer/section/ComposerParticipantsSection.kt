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

package ch.protonmail.android.uitest.robot.composer.section

import androidx.compose.ui.test.junit4.ComposeTestRule
import ch.protonmail.android.uitest.robot.composer.model.SenderParticipantEntryModel
import ch.protonmail.android.uitest.robot.composer.model.ToParticipantEntryModel
import ch.protonmail.android.uitest.util.getTestString
import ch.protonmail.android.test.R as testR

internal class ComposerParticipantsSection(composeTestRule: ComposeTestRule) {

    private val senderField by lazy { SenderParticipantEntryModel(composeTestRule) }
    private val recipientField by lazy { ToParticipantEntryModel(composeTestRule) }

    fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    fun typeSender(sender: String) = apply {
        senderField.typeValue(sender)
    }

    fun typeRecipient(recipient: String) = apply {
        recipientField.typeValue(recipient)
    }

    inner class Verify {

        fun hasEmptySender() = apply {
            senderField.hasPrefix(FromFieldPrefix)
                .hasEmptyValue()
        }

        fun hasSender(value: String) = apply {
            senderField.hasPrefix(FromFieldPrefix)
                .hasValue(value)
        }

        fun hasRecipientFieldFocused() = apply {
            recipientField.isFocused()
        }

        fun hasEmptyRecipient() = apply {
            recipientField.hasPrefix(ToFieldPrefix)
                .hasEmptyValue()
        }

        fun hasRecipient(value: String) = apply {
            recipientField.hasPrefix(ToFieldPrefix)
                .hasValue(value)
        }
    }

    private companion object {

        val FromFieldPrefix = getTestString(testR.string.test_composer_sender_label)
        val ToFieldPrefix = getTestString(testR.string.test_composer_recipient_label)
    }
}
