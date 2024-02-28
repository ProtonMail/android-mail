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
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.DraftState
import ch.protonmail.android.mailmessage.domain.model.DraftSyncState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class GetMessageIdsInDraftStateTest {

    private val userId = UserIdSample.Primary
    private val draftStateList = listOf(
        DraftState(
            userId = userId,
            messageId = MessageId("outboxItem01"),
            apiMessageId = null,
            state = DraftSyncState.Local,
            action = DraftAction.Compose,
            sendingError = null,
            sendingStatusConfirmed = false
        ),
        DraftState(
            userId = userId,
            messageId = MessageId("outboxItem02"),
            apiMessageId = MessageIdSample.NewDraftWithSubjectAndBody,
            state = DraftSyncState.Synchronized,
            action = DraftAction.Compose,
            sendingError = null,
            sendingStatusConfirmed = false
        ),
        DraftState(
            userId = userId,
            messageId = MessageId("outboxItem03"),
            apiMessageId = MessageIdSample.SepWeatherForecast,
            state = DraftSyncState.Sent,
            action = DraftAction.Compose,
            sendingError = null,
            sendingStatusConfirmed = false
        )
    )

    private val draftStateRepository = mockk<DraftStateRepository>()

    private val getMessageIdsInDraftState = GetMessageIdsInDraftState(draftStateRepository)

    @Test
    fun `should return empty list when there are no draft states`() = runTest {
        // Given
        coEvery { draftStateRepository.observeAll(userId) } returns flowOf(emptyList())

        // When
        val result = getMessageIdsInDraftState(userId)

        // Then
        assertEquals(emptyList(), result)
        coVerify(exactly = 1) { draftStateRepository.observeAll(userId) }
    }

    @Test
    fun `should return list of draft message ids`() = runTest {
        // Given
        val expectedIdList = draftStateList.map { it.apiMessageId ?: it.messageId }
        coEvery { draftStateRepository.observeAll(userId) } returns flowOf(draftStateList)

        // When
        val result = getMessageIdsInDraftState(userId)

        // Then
        assertEquals(expectedIdList, result)
        coVerify(exactly = 1) { draftStateRepository.observeAll(userId) }
    }

    @Test
    fun `should return message ids when api message ids are not available`() = runTest {
        // Given
        val draftStatesWithoutApiMessageId = draftStateList.map { it.copy(apiMessageId = null) }
        val expectedIdList = draftStatesWithoutApiMessageId.map { it.messageId }
        coEvery { draftStateRepository.observeAll(userId) } returns flowOf(draftStatesWithoutApiMessageId)

        // When
        val result = getMessageIdsInDraftState(userId)

        // Then
        assertEquals(expectedIdList, result)
        coVerify(exactly = 1) { draftStateRepository.observeAll(userId) }
    }
}
