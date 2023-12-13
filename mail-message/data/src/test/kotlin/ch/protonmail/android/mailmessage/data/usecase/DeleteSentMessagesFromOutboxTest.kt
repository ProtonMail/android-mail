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
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.usecase.DeleteDraftState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteSentMessagesFromOutboxTest {

    private val userId = UserIdSample.Primary
    private val deleteDraftState = mockk<DeleteDraftState>()
    private val deleteSentMessagesFromOutbox = DeleteSentMessagesFromOutbox(
        deleteDraftState
    )

    @Test
    fun `should delete sent messages from outbox`() = runTest {
        // Given
        coEvery { deleteDraftState(userId, any()) } returns Unit
        val sentDraftItems = listOf(
            DraftState(
                userId = userId,
                apiMessageId = MessageId("outboxItem01"),
                messageId = MessageIdSample.Invoice,
                state = DraftSyncState.Sent,
                action = DraftAction.Compose,
                sendingError = null,
                sendingStatusConfirmed = false
            ),
            DraftState(
                userId = userId,
                apiMessageId = MessageId("outboxItem02"),
                messageId = MessageIdSample.SepWeatherForecast,
                state = DraftSyncState.Sent,
                action = DraftAction.Compose,
                sendingError = null,
                sendingStatusConfirmed = false
            )
        )

        // When
        deleteSentMessagesFromOutbox.invoke(userId, sentDraftItems)

        // Then
        coVerify { deleteDraftState(userId, any()) }
        coVerify { deleteDraftState(userId, any()) }
    }
}
