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

package ch.protonmail.android.mailmailbox.presentation.mailbox.mapper

import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.AvatarUiModel
import ch.protonmail.android.testdata.mailbox.MailboxTestData.buildMailboxItem
import kotlin.test.Test
import kotlin.test.assertEquals

class AvatarUiModelMapperTest {

    private val participantsResolvedNames = listOf("Test")

    private val avatarUiModelMapper = AvatarUiModelMapper()

    @Test
    fun `avatar should show draft icon for all drafts in message mode`() {
        // Given
        val mailboxItem = buildMailboxItem(
            type = MailboxItemType.Message,
            labelIds = listOf(SystemLabelId.AllDrafts.labelId)
        )
        val expectedResult = AvatarUiModel.DraftIcon

        // When
        val result = avatarUiModelMapper(mailboxItem, participantsResolvedNames)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `avatar should show first letter of first participant for all sent messages in message mode`() {
        // Given
        val mailboxItem = buildMailboxItem(
            type = MailboxItemType.Message,
            labelIds = listOf(SystemLabelId.AllSent.labelId)
        )
        val expectedResult = AvatarUiModel.ParticipantInitial(value = "T")

        // When
        val result = avatarUiModelMapper(mailboxItem, participantsResolvedNames)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `avatar should show first letter of first participant for mailbox items in conversation mode`() {
        // Given
        val mailboxItem = buildMailboxItem(
            type = MailboxItemType.Conversation,
            labelIds = listOf(SystemLabelId.Inbox.labelId)
        )
        val expectedResult = AvatarUiModel.ParticipantInitial(value = "T")

        // When
        val result = avatarUiModelMapper(mailboxItem, participantsResolvedNames)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `avatar should show emoji if the first letter of the first participant is an emoji`() {
        // Given
        val mailboxItem = buildMailboxItem()
        val participantsResolvedNames = listOf("\uD83D\uDC7D Test")
        val expectedResult = AvatarUiModel.ParticipantInitial(value = "\uD83D\uDC7D")

        // When
        val result = avatarUiModelMapper(mailboxItem, participantsResolvedNames)

        // Then
        assertEquals(expectedResult, result)
    }
}
