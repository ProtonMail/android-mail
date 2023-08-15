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

import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.mailmessage.domain.model.Participant
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantName
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantNameResult
import ch.protonmail.android.testdata.contact.ContactTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

class ParticipantUiModelMapperTest {

    private val participant = Participant(address = "test@protonmail.com", name = "Test")

    private val resolveParticipantName: ResolveParticipantName = mockk {
        every {
            this@mockk.invoke(participant, ContactTestData.contacts, any())
        } returns ResolveParticipantNameResult("Test", isProton = false)
    }

    private val participantUiModelMapper = ParticipantUiModelMapper(resolveParticipantName)

    @Test
    fun `sender is mapped to a participant ui model`() {
        // Given
        val expectedResult = ParticipantUiModel(
            participantName = "Test",
            participantAddress = "test@protonmail.com",
            participantPadlock = R.drawable.ic_proton_lock,
            shouldShowOfficialBadge = false
        )

        // When
        val result = participantUiModelMapper.senderToUiModel(participant, ContactTestData.contacts)

        // Then
        assertEquals(expectedResult, result)
        verify {
            resolveParticipantName(participant, ContactTestData.contacts, ResolveParticipantName.FallbackType.USERNAME)
        }
    }

    @Test
    fun `recipient is mapped to a participant ui model`() {
        // Given
        val expectedResult = ParticipantUiModel(
            participantName = "Test",
            participantAddress = "test@protonmail.com",
            participantPadlock = R.drawable.ic_proton_lock,
            shouldShowOfficialBadge = false
        )

        // When
        val result = participantUiModelMapper.recipientToUiModel(participant, ContactTestData.contacts)

        // Then
        assertEquals(expectedResult, result)
        verify {
            resolveParticipantName(participant, ContactTestData.contacts, ResolveParticipantName.FallbackType.NONE)
        }
    }
}
