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

package ch.protonmail.android.mailmailbox.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.toMailLabelSystem
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveMailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import kotlin.test.Test
import kotlin.test.assertEquals

class MoveConversationsTest {

    private val userId = UserIdSample.Primary
    private val conversationIds = listOf(ConversationIdSample.Invoices, ConversationIdSample.WeatherForecast)
    private val exclusiveMailLabels = SystemLabelId.exclusiveList.map { it.toMailLabelSystem() }

    private val conversationRepository = mockk<ConversationRepository>()
    private val observeMailLabels = mockk<ObserveMailLabels>()
    private val observeExclusiveMailLabels = mockk<ObserveExclusiveMailLabels>()

    private val moveConversations by lazy {
        MoveConversations(
            conversationRepository = conversationRepository,
            observeExclusiveMailLabels = observeExclusiveMailLabels,
            observeMailLabels = observeMailLabels
        )
    }

    @Test
    fun `when move succeeds then Unit is returned`() = runTest {
        // Given
        val destinationLabel = LabelId("labelId")
        val expectedConversations = listOf(ConversationSample.WeatherForecast, ConversationSample.AlphaAppFeedback)

        expectObserveMailLabelsSucceeds()
        expectObserveExclusiveMailLabelSucceeds()
        expectMoveSucceeds(destinationLabel, expectedConversations)

        // When
        val result = moveConversations(userId, conversationIds, destinationLabel)

        // Then
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `when move fails then DataError is returned`() = runTest {
        // Given
        val destinationLabel = LabelId("labelId")

        expectObserveMailLabelsSucceeds()
        expectObserveExclusiveMailLabelSucceeds()
        expectMoveFails(destinationLabel)

        // When
        val result = moveConversations(userId, conversationIds, destinationLabel)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), result)
    }

    private fun expectMoveSucceeds(destinationLabel: LabelId, expectedList: List<Conversation>) {
        val exclusiveList = exclusiveMailLabels.map { it.id.labelId }
        coEvery {
            conversationRepository.move(userId, conversationIds, exclusiveList, exclusiveList, destinationLabel)
        } returns expectedList.right()
    }

    private fun expectMoveFails(destinationLabel: LabelId) {
        val exclusiveList = exclusiveMailLabels.map { it.id.labelId }
        coEvery {
            conversationRepository.move(userId, conversationIds, exclusiveList, exclusiveList, destinationLabel)
        } returns DataError.Local.NoDataCached.left()
    }

    private fun expectObserveExclusiveMailLabelSucceeds() {
        every { observeExclusiveMailLabels(userId) } returns flowOf(
            MailLabels(
                systemLabels = exclusiveMailLabels,
                folders = emptyList(),
                labels = emptyList()
            )
        )
    }

    private fun expectObserveMailLabelsSucceeds() {
        every { observeMailLabels(userId) } returns flowOf(
            MailLabels(
                systemLabels = exclusiveMailLabels,
                folders = emptyList(),
                labels = emptyList()
            )
        )
    }
}
