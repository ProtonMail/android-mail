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

package ch.protonmail.android.maildetail.domain.usecase

import java.util.UUID
import ch.protonmail.android.maildetail.domain.model.DecryptedMessageBody
import ch.protonmail.android.maildetail.domain.repository.InMemoryConversationStateRepository
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.MimeType
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SetMessageViewStateTest {

    private val repo = mockk<InMemoryConversationStateRepository>(relaxUnitFun = true)

    @Test
    fun `Should expand message on expand`() = runTest {
        // Given
        val useCase = buildUseCase()
        val messageId = MessageId(UUID.randomUUID().toString())
        val decryptedMessageBody = DecryptedMessageBody(
            UUID.randomUUID().toString(),
            MimeType.Html
        )

        // When
        useCase.expanded(messageId, decryptedMessageBody)

        // Then
        coVerify { repo.expandMessage(messageId, decryptedMessageBody) }
    }

    @Test
    fun `Should collapse message on collapse`() = runTest {
        // Given
        val useCase = buildUseCase()
        val messageId = MessageId(UUID.randomUUID().toString())

        // When
        useCase.collapsed(messageId)

        // Then
        coVerify { repo.collapseMessage(messageId) }
    }

    @Test
    fun `Should call expanding on expanding message`() = runTest {
        // Given
        val useCase = buildUseCase()
        val messageId = MessageId(UUID.randomUUID().toString())

        // When
        useCase.expanding(messageId)

        // Then
        coVerify { repo.expandingMessage(messageId) }
    }

    private fun buildUseCase() = SetMessageViewState(repo)
}
