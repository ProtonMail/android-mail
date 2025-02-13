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

import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.usecase.ClearMessageSendingError
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveMessageSendingError
import ch.protonmail.android.mailcomposer.domain.usecase.SendMessage
import ch.protonmail.android.mailcomposer.presentation.usecase.FormatMessageSendingError
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageId
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class MessageSendingFacadeTest {

    private val sendMessage = mockk<SendMessage>(relaxed = true)
    private val observeSendingErrors = mockk<ObserveMessageSendingError>(relaxed = true)
    private val formatMessageSendingError = mockk<FormatMessageSendingError>(relaxed = true)
    private val clearMessageSendingError = mockk<ClearMessageSendingError>(relaxed = true)

    private lateinit var messageSendingFacade: MessageSendingFacade

    @BeforeTest
    fun setup() {
        messageSendingFacade = MessageSendingFacade(
            sendMessage,
            observeSendingErrors,
            formatMessageSendingError,
            clearMessageSendingError
        )
    }

    @Test
    fun `should proxy sendMessage accordingly`() = runTest {
        // Given
        val userId = UserId("user-id")
        val messageId = MessageId("message-id")
        val fields = mockk<DraftFields>()
        val action = DraftAction.Compose

        // When
        messageSendingFacade.sendMessage(userId, messageId, fields, action)

        // Then
        coVerify(exactly = 1) { sendMessage(userId, messageId, fields, action) }
    }

    @Test
    fun `should proxy observeAndFormatSendingErrors accordingly`() = runTest {
        // Given
        val userId = UserId("user-id")
        val messageId = MessageId("message-id")

        // When
        messageSendingFacade.observeAndFormatSendingErrors(userId, messageId)

        // Then
        coVerify(exactly = 1) { observeSendingErrors(userId, messageId) }
    }

    @Test
    fun `should proxy clearMessageSendingError accordingly`() = runTest {
        // Given
        val userId = UserId("user-id")
        val messageId = MessageId("message-id")

        // When
        messageSendingFacade.clearMessageSendingError(userId, messageId)

        // Then
        coVerify(exactly = 1) { clearMessageSendingError(userId, messageId) }
    }
}
