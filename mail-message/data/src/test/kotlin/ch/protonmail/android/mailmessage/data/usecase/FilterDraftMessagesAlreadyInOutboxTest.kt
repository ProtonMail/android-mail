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
import ch.protonmail.android.mailmessage.data.getMessageResource
import ch.protonmail.android.mailmessage.domain.repository.OutboxRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class FilterDraftMessagesAlreadyInOutboxTest {

    private val userId = UserIdSample.Primary
    private val allMailLabel = SystemLabelId.AllMail.labelId.id
    private val draftLabel = SystemLabelId.AllDrafts.labelId.id
    private val sentLabel = SystemLabelId.AllSent.labelId.id
    private val entities = listOf(
        getMessageResource(
            id = MessageIdSample.Invoice.id, labelIds = listOf(allMailLabel, sentLabel)
        ),
        getMessageResource(
            id = MessageIdSample.NewDraftWithSubjectAndBody.id, labelIds = listOf(allMailLabel, draftLabel)
        ),
        getMessageResource(
            id = MessageIdSample.SepWeatherForecast.id, labelIds = listOf(allMailLabel, sentLabel)
        )
    )
    private val allMailEntityCount = 3
    private val sentEntityCount = 2

    private val outboxRepository = mockk<OutboxRepository>()

    private val filterDraftMessagesAlreadyInOutbox = FilterDraftMessagesAlreadyInOutbox(outboxRepository)

    @Test
    fun `should return the same message list when there is no outbox messages`() = runTest {
        // Given
        coEvery { outboxRepository.observeAll(userId) } returns flowOf(emptyList())

        // When
        val result = filterDraftMessagesAlreadyInOutbox.invoke(userId, entities)

        // Then
        assertEquals(allMailEntityCount, result.size)
        coVerify(exactly = 1) { outboxRepository.observeAll(userId) }
    }


    @Test
    fun `should filter out draft message when it is in outbox messages`() = runTest {
        // Given
        coEvery { outboxRepository.observeAll(userId) } returns
            flowOf(listOf(MessageIdSample.NewDraftWithSubjectAndBody))

        // When
        val result = filterDraftMessagesAlreadyInOutbox.invoke(userId, entities)

        // Then
        assertEquals(sentEntityCount, result.size)
        coVerify(exactly = 1) { outboxRepository.observeAll(userId) }
    }

    @Test
    fun `should not filter out sent messages when they are also in outbox messages`() = runTest {
        // Given
        coEvery { outboxRepository.observeAll(userId) } returns
            flowOf(listOf(MessageIdSample.Invoice, MessageIdSample.SepWeatherForecast))

        // When
        val result = filterDraftMessagesAlreadyInOutbox.invoke(userId, entities)

        // Then
        assertEquals(allMailEntityCount, result.size)
        coVerify(exactly = 1) { outboxRepository.observeAll(userId) }
    }
}
