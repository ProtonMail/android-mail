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

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailconversation.domain.ConversationLabelPropagationOptions
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class MoveRemoteMessageAndLocalConversationTest {

    private val moveMessage = mockk<MoveMessage>()
    private val conversationRepository = mockk<ConversationRepository>()
    private val moveRemoteMessageAndLocalConversation =
        MoveRemoteMessageAndLocalConversation(moveMessage, conversationRepository)

    @Test
    fun `should return an error when message move fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.PlainTextMessage
        val conversationId = ConversationIdSample.WeatherForecast

        val conversationLabelingOptions = MoveRemoteMessageAndLocalConversation.ConversationLabelingOptions(
            removeCurrentLabel = false,
            fromLabel = null,
            toLabel = SystemLabelId.Archive.labelId
        )

        coEvery { moveMessage(userId, messageId, SystemLabelId.Archive.labelId) } returns DataError.Local.Unknown.left()

        // When
        val result = moveRemoteMessageAndLocalConversation(
            userId,
            messageId,
            conversationId,
            conversationLabelingOptions
        )

        // Then
        assert(result.isLeft())
        coVerify(exactly = 1) { moveMessage(userId, messageId, SystemLabelId.Archive.labelId) }
        coVerify { conversationRepository wasNot called }
        confirmVerified(moveMessage, conversationRepository)
    }

    @Test
    fun `should return an error when removing the label from the conversation fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.PlainTextMessage
        val conversationId = ConversationIdSample.WeatherForecast

        val conversationLabelingOptions = MoveRemoteMessageAndLocalConversation.ConversationLabelingOptions(
            removeCurrentLabel = true,
            fromLabel = SystemLabelId.Archive.labelId,
            toLabel = SystemLabelId.Spam.labelId
        )

        coEvery { moveMessage(userId, messageId, SystemLabelId.Spam.labelId) } returns Unit.right()
        coEvery {
            conversationRepository.removeLabels(
                userId,
                listOf(conversationId),
                listOf(SystemLabelId.Archive.labelId),
                moveLabelOption
            )
        } returns DataError.Local.Unknown.left()

        // When
        val result = moveRemoteMessageAndLocalConversation(
            userId,
            messageId,
            conversationId,
            conversationLabelingOptions
        )

        // Then
        assert(result.isLeft())
        coVerify(exactly = 1) { moveMessage(userId, messageId, SystemLabelId.Spam.labelId) }
        coVerify(exactly = 1) {
            conversationRepository.removeLabels(
                userId,
                listOf(conversationId),
                listOf(SystemLabelId.Archive.labelId),
                moveLabelOption
            )
        }
        confirmVerified(moveMessage, conversationRepository)
    }

    @Test
    fun `should return an error when adding the label to the conversation fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.PlainTextMessage
        val conversationId = ConversationIdSample.WeatherForecast

        val conversationLabelingOptions = MoveRemoteMessageAndLocalConversation.ConversationLabelingOptions(
            removeCurrentLabel = true,
            fromLabel = SystemLabelId.Archive.labelId,
            toLabel = SystemLabelId.Spam.labelId
        )

        coEvery { moveMessage(userId, messageId, SystemLabelId.Spam.labelId) } returns Unit.right()
        coEvery {
            conversationRepository.removeLabels(
                userId,
                listOf(conversationId),
                listOf(SystemLabelId.Archive.labelId),
                moveLabelOption
            )
        } returns listOf(ConversationSample.WeatherForecast).right()

        coEvery {
            conversationRepository.addLabels(
                userId,
                listOf(conversationId),
                listOf(SystemLabelId.Spam.labelId),
                moveLabelOption
            )
        } returns DataError.Local.Unknown.left()

        // When
        val result = moveRemoteMessageAndLocalConversation(
            userId,
            messageId,
            conversationId,
            conversationLabelingOptions
        )

        // Then
        assert(result.isLeft())
        coVerify(exactly = 1) { moveMessage(userId, messageId, SystemLabelId.Spam.labelId) }
        coVerify(exactly = 1) {
            conversationRepository.removeLabels(
                userId,
                listOf(conversationId),
                listOf(SystemLabelId.Archive.labelId),
                moveLabelOption
            )

            conversationRepository.addLabels(
                userId,
                listOf(conversationId),
                listOf(SystemLabelId.Spam.labelId),
                moveLabelOption
            )
        }
        confirmVerified(moveMessage, conversationRepository)
    }

    @Test
    fun `should not remove the label when the options say so`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.PlainTextMessage
        val conversationId = ConversationIdSample.WeatherForecast

        val conversationLabelingOptions = MoveRemoteMessageAndLocalConversation.ConversationLabelingOptions(
            removeCurrentLabel = false,
            fromLabel = SystemLabelId.Archive.labelId,
            toLabel = SystemLabelId.Spam.labelId
        )

        coEvery { moveMessage(userId, messageId, SystemLabelId.Spam.labelId) } returns Unit.right()

        coEvery {
            conversationRepository.addLabels(
                userId,
                listOf(conversationId),
                listOf(SystemLabelId.Spam.labelId),
                moveLabelOption
            )
        } returns DataError.Local.Unknown.left()

        // When
        val result = moveRemoteMessageAndLocalConversation(
            userId,
            messageId,
            conversationId,
            conversationLabelingOptions
        )

        // Then
        assert(result.isLeft())
        coVerify(exactly = 1) { moveMessage(userId, messageId, SystemLabelId.Spam.labelId) }
        coVerify(exactly = 1) {
            conversationRepository.addLabels(
                userId,
                listOf(conversationId),
                listOf(SystemLabelId.Spam.labelId),
                moveLabelOption
            )
        }
        confirmVerified(moveMessage, conversationRepository)
    }

    @Test
    fun `should return unit if all operations succeed`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.PlainTextMessage
        val conversationId = ConversationIdSample.WeatherForecast

        val conversationLabelingOptions = MoveRemoteMessageAndLocalConversation.ConversationLabelingOptions(
            removeCurrentLabel = false,
            fromLabel = SystemLabelId.Archive.labelId,
            toLabel = SystemLabelId.Spam.labelId
        )

        coEvery { moveMessage(userId, messageId, SystemLabelId.Spam.labelId) } returns Unit.right()

        coEvery {
            conversationRepository.addLabels(
                userId,
                listOf(conversationId),
                listOf(SystemLabelId.Spam.labelId),
                moveLabelOption
            )
        } returns listOf(ConversationSample.WeatherForecast).right()

        // When
        val result = moveRemoteMessageAndLocalConversation(
            userId,
            messageId,
            conversationId,
            conversationLabelingOptions
        )

        // Then
        assert(result.isRight())
        coVerify(exactly = 1) { moveMessage(userId, messageId, SystemLabelId.Spam.labelId) }
        coVerify(exactly = 1) {
            conversationRepository.addLabels(
                userId,
                listOf(conversationId),
                listOf(SystemLabelId.Spam.labelId),
                moveLabelOption
            )
        }
        confirmVerified(moveMessage, conversationRepository)
    }

    private companion object {

        val moveLabelOption = ConversationLabelPropagationOptions(
            propagateToMessages = false,
            propagateRemotely = false
        )
    }
}
