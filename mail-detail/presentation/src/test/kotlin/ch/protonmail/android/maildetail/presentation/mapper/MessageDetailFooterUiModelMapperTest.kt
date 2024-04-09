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

import ch.protonmail.android.maildetail.presentation.model.MessageDetailFooterUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithLabelsSample
import ch.protonmail.android.mailmessage.domain.sample.RecipientSample
import kotlin.test.Test
import kotlin.test.assertEquals

internal class MessageDetailFooterUiModelMapperTest {

    private val mapper = MessageDetailFooterUiModelMapper()

    @Test
    fun `should show reply all if recipients size is greater than one`() {
        // Given
        val multipleRecipientsMessage = MessageWithLabelsSample.build(
            MessageSample.build(
                toList = listOf(RecipientSample.John, RecipientSample.Doe)
            )
        )

        val expected = MessageDetailFooterUiModel(
            messageId = MessageIdUiModel(multipleRecipientsMessage.message.messageId.id),
            shouldShowReplyAll = true
        )

        // When
        val actual = mapper.toUiModel(multipleRecipientsMessage)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should not show reply all if recipients size is not greater than one`() {
        // Given
        val singleRecipientMessage = MessageWithLabelsSample.build(
            MessageSample.build(toList = listOf(RecipientSample.John))
        )

        val expected = MessageDetailFooterUiModel(
            messageId = MessageIdUiModel(singleRecipientMessage.message.messageId.id),
            shouldShowReplyAll = false
        )

        // When
        val actual = mapper.toUiModel(singleRecipientMessage)

        // Then
        assertEquals(expected, actual)
    }
}
