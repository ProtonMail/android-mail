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

package ch.protonmail.android.maildetail.presentation.mapper

import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.testdata.message.MessageTestData
import kotlin.test.assertEquals
import kotlin.test.Test

class DetailAvatarUiModelMapperTest {

    private val messageId = "messageId"
    private val senderResolvedName = "Sender"

    private val detailAvatarUiModelMapper = DetailAvatarUiModelMapper()

    @Test
    fun `avatar should show draft icon for all drafts`() {
        // Given
        val message = MessageTestData.buildMessage(
            id = messageId,
            labelIds = listOf(SystemLabelId.AllDrafts.labelId.id)
        )
        val expectedResult = AvatarUiModel.DraftIcon

        // When
        val result = detailAvatarUiModelMapper(message, senderResolvedName)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `avatar should show first letter of sender for non-draft message`() {
        // Given
        val message = MessageTestData.buildMessage(
            id = messageId,
            labelIds = listOf(SystemLabelId.Inbox.labelId.id)
        )
        val expectedResult = AvatarUiModel.ParticipantInitial(value = "S")

        // When
        val result = detailAvatarUiModelMapper(message, senderResolvedName)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `avatar should show emoji if the first letter of the sender is an emoji`() {
        // Given
        val message = MessageTestData.buildMessage(id = messageId)
        val senderResolvedName = "\uD83D\uDC7D Test"
        val expectedResult = AvatarUiModel.ParticipantInitial(value = "\uD83D\uDC7D")

        // When
        val result = detailAvatarUiModelMapper(message, senderResolvedName)

        // Then
        assertEquals(expectedResult, result)
    }
}
