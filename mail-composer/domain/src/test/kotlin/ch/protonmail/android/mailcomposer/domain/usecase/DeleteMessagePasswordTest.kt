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

package ch.protonmail.android.mailcomposer.domain.usecase

import arrow.core.right
import ch.protonmail.android.mailcomposer.domain.repository.MessagePasswordRepository
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.DraftState
import ch.protonmail.android.mailmessage.domain.model.DraftSyncState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.test.utils.FakeTransactor
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DeleteMessagePasswordTest {

    val userId = UserIdTestData.userId
    val messageId = MessageIdSample.NewDraftWithSubjectAndBody
    private val apiMessageId = MessageId("apiMessageId")

    private val draftStateRepository = mockk<DraftStateRepository>()
    private val messagePasswordRepository = mockk<MessagePasswordRepository>()
    private val transactor = FakeTransactor()

    private val deleteMessagePassword = DeleteMessagePassword(
        draftStateRepository = draftStateRepository,
        messagePasswordRepository = messagePasswordRepository,
        transactor = transactor
    )

    @Test
    fun `should call delete method from repository when deleting message password and api message id exists`() =
        runTest {
            // Given
            expectApiMessageIdExists()
            coEvery { messagePasswordRepository.deleteMessagePassword(userId, apiMessageId) } just runs

            // When
            deleteMessagePassword(userId, messageId)

            // Then
            coVerify { messagePasswordRepository.deleteMessagePassword(userId, apiMessageId) }
        }

    @Test
    fun `should call delete method from repository when deleting message password and api message id does not exist`() =
        runTest {
            // Given
            expectApiMessageIdDoesNotExist()
            coEvery { messagePasswordRepository.deleteMessagePassword(userId, messageId) } just runs

            // When
            deleteMessagePassword(userId, messageId)

            // Then
            coVerify { messagePasswordRepository.deleteMessagePassword(userId, messageId) }
        }

    private fun expectApiMessageIdExists() {
        coEvery {
            draftStateRepository.observe(userId, messageId)
        } returns flowOf(
            DraftState(
                userId = userId,
                messageId = messageId,
                apiMessageId = apiMessageId,
                state = DraftSyncState.Synchronized,
                action = DraftAction.Compose,
                sendingError = null,
                sendingStatusConfirmed = false
            ).right()
        )
    }

    private fun expectApiMessageIdDoesNotExist() {
        coEvery {
            draftStateRepository.observe(userId, messageId)
        } returns flowOf(
            DraftState(
                userId = userId,
                messageId = messageId,
                apiMessageId = null,
                state = DraftSyncState.Synchronized,
                action = DraftAction.Compose,
                sendingError = null,
                sendingStatusConfirmed = false
            ).right()
        )
    }
}
