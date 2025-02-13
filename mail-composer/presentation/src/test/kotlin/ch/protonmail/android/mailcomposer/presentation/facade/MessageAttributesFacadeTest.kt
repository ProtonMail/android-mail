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

import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveMessageExpirationTime
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveMessagePassword
import ch.protonmail.android.mailcomposer.domain.usecase.SaveMessageExpirationTime
import ch.protonmail.android.mailmessage.domain.model.MessageId
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.time.Duration.Companion.minutes

internal class MessageAttributesFacadeTest {

    private val observeMessagePassword = mockk<ObserveMessagePassword>(relaxed = true)
    private val observeMessageExpiration = mockk<ObserveMessageExpirationTime>(relaxed = true)
    private val saveMessageExpirationTime = mockk<SaveMessageExpirationTime>(relaxed = true)

    private lateinit var messageAttributesFacade: MessageAttributesFacade

    @BeforeTest
    fun setup() {
        messageAttributesFacade = MessageAttributesFacade(
            observeMessagePassword,
            observeMessageExpiration,
            saveMessageExpirationTime
        )
    }

    @Test
    fun `should proxy observeMessagePassword accordingly`() = runTest {
        // Given
        val userId = UserId("user-id")
        val messageId = MessageId("message-id")

        // When
        messageAttributesFacade.observeMessagePassword(userId, messageId)

        // Then
        coVerify(exactly = 1) { observeMessagePassword(userId, messageId) }
    }

    @Test
    fun `should proxy observeMessageExpiration accordingly`() = runTest {
        // Given
        val userId = UserId("user-id")
        val messageId = MessageId("message-id")

        // When
        messageAttributesFacade.observeMessageExpiration(userId, messageId)

        // Then
        coVerify(exactly = 1) { observeMessageExpiration(userId, messageId) }
    }

    @Test
    fun `should proxy saveMessageExpiration accordingly`() = runTest {
        // Given
        val userId = UserId("user-id")
        val messageId = MessageId("message-id")
        val senderEmail = SenderEmail("sender@email.com")
        val expiration = 10.minutes

        // When
        messageAttributesFacade.saveMessageExpiration(userId, messageId, senderEmail, expiration)

        // Then
        coVerify(exactly = 1) { saveMessageExpirationTime(userId, messageId, senderEmail, expiration) }
    }
}
