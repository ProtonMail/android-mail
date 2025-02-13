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

package ch.protonmail.android.mailcomposer.presentation.facade

import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcomposer.domain.model.RecipientsBcc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsCc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsTo
import ch.protonmail.android.mailcomposer.domain.usecase.GetExternalRecipients
import ch.protonmail.android.mailcomposer.presentation.mapper.ComposerParticipantMapper
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class MessageParticipantsFacadeTest {

    private val observePrimaryUserId = mockk<ObservePrimaryUserId>(relaxed = true)
    private val participantMapper = mockk<ComposerParticipantMapper>(relaxed = true)
    private val getExternalRecipients = mockk<GetExternalRecipients>(relaxed = true)

    private lateinit var messageParticipantsFacade: MessageParticipantsFacade

    @BeforeTest
    fun setup() {
        messageParticipantsFacade = MessageParticipantsFacade(
            observePrimaryUserId,
            participantMapper,
            getExternalRecipients
        )
    }

    @Test
    fun `should proxy observePrimaryUserId accordingly`() {
        // When
        messageParticipantsFacade.observePrimaryUserId()

        // Then
        verify(exactly = 1) { observePrimaryUserId.invoke() }
    }

    @Test
    fun `should proxy mapToParticipant accordingly`() = runTest {
        // Given
        val recipient = RecipientUiModel.Valid("test@example.com")

        // When
        messageParticipantsFacade.mapToParticipant(recipient)

        // Then
        coVerify(exactly = 1) { participantMapper.recipientUiModelToParticipant(recipient) }
    }

    @Test
    fun `should proxy getExternalRecipients accordingly`() = runTest {
        // Given
        val userId = UserId("user-id")
        val recipientsTo = RecipientsTo(mockk())
        val recipientsCc = RecipientsCc(mockk())
        val recipientsBcc = RecipientsBcc(mockk())

        // When
        messageParticipantsFacade.getExternalRecipients(userId, recipientsTo, recipientsCc, recipientsBcc)

        // Then
        coVerify(exactly = 1) { getExternalRecipients(userId, recipientsTo, recipientsCc, recipientsBcc) }
    }
}
