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
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailconversation.domain.sample.ConversationLabelSample
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.toMailLabelSystem
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveMailLabels
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MoveConversationTest {

    private val userId = UserIdSample.Primary
    private val conversationId = ConversationIdSample.WeatherForecast
    private val exclusiveMailLabels = SystemLabelId.exclusiveList.map { it.toMailLabelSystem() }

    private val conversationRepository: ConversationRepository = mockk {
        every { this@mockk.observeConversation(userId, conversationId, any()) } returns flowOf(
            ConversationSample.WeatherForecast.copy(
                labels = listOf(ConversationLabelSample.WeatherForecast.Inbox)
            ).right()
        )
    }
    private val observeExclusiveMailLabels: ObserveExclusiveMailLabels = mockk {
        every { this@mockk.invoke(userId) } returns flowOf(
            MailLabels(
                systemLabels = exclusiveMailLabels,
                folders = emptyList(),
                labels = emptyList()
            )
        )
    }
    private val move = MoveConversation(
        conversationRepository = conversationRepository,
        observeExclusiveMailLabels = observeExclusiveMailLabels
    )

    @Test
    fun `when conversation repository returns error then return error`() = runTest {
        // Given
        val error = DataError.Local.NoDataCached.left()
        coEvery { conversationRepository.observeConversation(userId, conversationId, any()) } returns flowOf(error)

        // When
        val actual = move(userId, conversationId, SystemLabelId.Trash.labelId)

        // Then
        assertEquals(error, actual)
    }

    @Test
    fun `when move to returns error then return error`() = runTest {
        // Given
        val error = DataError.Local.NoDataCached.left()
        coEvery {
            conversationRepository.move(
                userId,
                conversationId,
                exclusiveMailLabels.map { it.id.labelId },
                SystemLabelId.Trash.labelId
            )
        } returns error

        // When
        val result = move(userId, conversationId, SystemLabelId.Trash.labelId)

        // Then
        assertEquals(error, result)
    }


    @Test
    fun `when moving a conversation to trash then repository is called with the given data`() = runTest {
        // Given
        val toLabel = SystemLabelId.Trash.labelId

        val conversation = ConversationSample.WeatherForecast.copy(
            labels = listOf(ConversationLabelSample.WeatherForecast.Trash)
        ).right()
        coEvery {
            conversationRepository.move(
                userId,
                conversationId,
                exclusiveMailLabels.map { it.id.labelId },
                toLabel
            )
        } returns conversation

        // When
        val result = move(userId, conversationId, toLabel)

        // Then
        coVerify {
            conversationRepository.move(
                userId,
                conversationId,
                exclusiveMailLabels.map { it.id.labelId },
                toLabel
            )
        }
        assertEquals(conversation, result)
    }
}
