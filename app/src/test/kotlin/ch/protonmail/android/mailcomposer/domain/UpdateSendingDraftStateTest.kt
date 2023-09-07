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

package ch.protonmail.android.mailcomposer.domain

import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.model.DraftSyncState
import ch.protonmail.android.mailcomposer.domain.repository.DraftStateRepository
import ch.protonmail.android.mailcomposer.domain.sample.DraftStateSample
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UpdateSendingDraftStateTest {

    private val draftStateRepository = mockk<DraftStateRepository>()

    private val updateSendingDraftState = UpdateSendingDraftState(draftStateRepository)

    @Test
    fun `reset error state sets the draft state to Syncronized`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = DraftStateSample.RemoteDraftInErrorSendingState.messageId
        coJustRun { draftStateRepository.updateDraftSyncState(userId, messageId, DraftSyncState.Synchronized) }

        // When
        updateSendingDraftState.resetErrorState(userId, messageId)

        // Then
        coVerify { draftStateRepository.updateDraftSyncState(userId, messageId, DraftSyncState.Synchronized) }
    }

    @Test
    fun `delete draft state delegates to repository`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = DraftStateSample.RemoteDraftInErrorSendingState.messageId
        coJustRun { draftStateRepository.deleteDraftState(userId, messageId) }

        // When
        updateSendingDraftState.deleteDraftState(userId, messageId)

        // Then
        coVerify { draftStateRepository.deleteDraftState(userId, messageId) }
    }
}
