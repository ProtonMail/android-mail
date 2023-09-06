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

import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.model.DraftState
import ch.protonmail.android.mailcomposer.domain.repository.DraftStateRepository
import ch.protonmail.android.mailcomposer.domain.sample.DraftStateSample
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveSendingDraftStatesTest {

    private val draftStateRepository = mockk<DraftStateRepository>()

    private val observeSendingMessageState = ObserveSendingDraftStates(draftStateRepository)

    @Test
    fun `returns filtered sent and error sending draft states from repository`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        expectedDraftStates(userId) {
            listOf(
                DraftStateSample.RemoteDraftInSentState,
                DraftStateSample.RemoteDraftInErrorSendingState,
                DraftStateSample.RemoteDraftState
            )
        }


        // When
        val actual = observeSendingMessageState(userId).first()

        // Then
        val expected = listOf(DraftStateSample.RemoteDraftInSentState, DraftStateSample.RemoteDraftInErrorSendingState)
        assertEquals(expected, actual)
    }

    private fun expectedDraftStates(userId: UserId, states: () -> List<DraftState>): List<DraftState> = states().also {
        coEvery { draftStateRepository.observeAll(userId) } returns flowOf(it)
    }
}
