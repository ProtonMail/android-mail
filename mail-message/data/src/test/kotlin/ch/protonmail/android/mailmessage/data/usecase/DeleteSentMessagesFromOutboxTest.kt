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

package ch.protonmail.android.mailmessage.data.usecase

import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.data.getMessage
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.usecase.DeleteSentDraftMessagesStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteSentMessagesFromOutboxTest {

    private val userId = UserIdSample.Primary
    private val allMailLabel = SystemLabelId.AllMail.labelId.id
    private val draftLabel = SystemLabelId.AllDrafts.labelId.id
    private val sentLabel = SystemLabelId.AllSent.labelId.id
    private val outboxMessages = listOf(
        getMessage(
            id = MessageIdSample.Invoice.id, labelIds = listOf(allMailLabel, sentLabel)
        ),
        getMessage(
            id = MessageIdSample.NewDraftWithSubjectAndBody.id, labelIds = listOf(allMailLabel, draftLabel)
        ),
        getMessage(
            id = MessageIdSample.SepWeatherForecast.id, labelIds = listOf(allMailLabel, sentLabel)
        )
    )

    private val draftOnlyOutboxMessages = listOf(
        getMessage(
            id = MessageIdSample.Invoice.id, labelIds = listOf(allMailLabel, draftLabel)
        ),
        getMessage(
            id = MessageIdSample.NewDraftWithSubjectAndBody.id, labelIds = listOf(allMailLabel, draftLabel)
        ),
        getMessage(
            id = MessageIdSample.SepWeatherForecast.id, labelIds = listOf(allMailLabel, draftLabel)
        )
    )

    private val deleteSentDraftMessagesStatus = mockk<DeleteSentDraftMessagesStatus>()
    private val deleteSentMessagesFromOutbox = DeleteSentMessagesFromOutbox(deleteSentDraftMessagesStatus)

    @Test
    fun `should delete sent messages from outbox`() = runTest {
        // Given
        val sentItemsToClear = listOf(MessageIdSample.Invoice, MessageIdSample.SepWeatherForecast)
        coEvery { deleteSentDraftMessagesStatus(userId, any()) } returns Unit

        // When
        deleteSentMessagesFromOutbox.invoke(userId, outboxMessages)

        // Then
        coVerify(exactly = sentItemsToClear.size) {
            deleteSentDraftMessagesStatus(userId, any())
        }
    }

    @Test
    fun `should not delete draft messages from outbox`() = runTest {
        // Given
        coEvery { deleteSentDraftMessagesStatus(userId, any()) } returns Unit

        // When
        deleteSentMessagesFromOutbox.invoke(userId, draftOnlyOutboxMessages)

        // Then
        coVerify(exactly = 0) {
            deleteSentDraftMessagesStatus(userId, any())
        }
    }
}
