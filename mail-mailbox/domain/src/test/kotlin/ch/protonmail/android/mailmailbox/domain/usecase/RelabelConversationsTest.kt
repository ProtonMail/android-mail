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
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.mailmessage.domain.model.LabelSelectionList
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import kotlin.test.assertEquals

class RelabelConversationsTest {

    private val userId = UserIdSample.Primary
    private val conversationIds = listOf(ConversationIdSample.Invoices)
    private val conversationRepository: ConversationRepository = mockk()
    private val relabelConversation = RelabelConversations(conversationRepository)

    @Test
    fun `when repository fails then error is returned`() = runTest {
        // Given
        val error = DataError.Local.NoDataCached.left()
        coEvery { conversationRepository.relabel(userId, conversationIds, emptyList(), emptyList()) } returns error

        // When
        val result = relabelConversation(
            userId = userId,
            conversationIds = conversationIds,
            currentSelections = LabelSelectionList(
                selectedLabels = listOf(LabelId("labelId")),
                partiallySelectionLabels = emptyList()
            ),
            updatedSelections = LabelSelectionList(
                selectedLabels = listOf(LabelId("labelId")),
                partiallySelectionLabels = emptyList()
            )
        )

        // Then
        assertEquals(error, result)
    }

    @Test
    fun `use case passing correct add and remove label lists to repository`() = runTest {
        // Given
        val oldLabelIds = listOf(LabelId("1"), LabelId("2"), LabelId("3"))
        val newLabelIds = listOf(LabelId("1"), LabelId("2"), LabelId("4"))
        val removedLabels = listOf(LabelId("3"))
        val addedLabels = listOf(LabelId("4"))
        val expected = listOf(ConversationSample.AlphaAppFeedback).right()
        coEvery {
            conversationRepository.relabel(
                userId = UserIdSample.Primary,
                conversationIds = listOf(ConversationIdSample.Invoices),
                labelsToBeRemoved = removedLabels,
                labelsToBeAdded = addedLabels
            )
        } returns expected

        // When
        val actual = relabelConversation(
            UserIdSample.Primary,
            listOf(ConversationIdSample.Invoices),
            currentSelections = LabelSelectionList(
                selectedLabels = oldLabelIds,
                partiallySelectionLabels = emptyList()
            ),
            updatedSelections = LabelSelectionList(
                selectedLabels = newLabelIds,
                partiallySelectionLabels = emptyList()
            )
        )

        // Then
        assertEquals(expected, actual)
        coVerify {
            conversationRepository.relabel(
                userId = UserIdSample.Primary,
                conversationIds = listOf(ConversationIdSample.Invoices),
                labelsToBeRemoved = removedLabels,
                labelsToBeAdded = addedLabels
            )
        }
    }

    @Test
    fun `use case passing correct add and remove label lists when partial selection is changed to remove`() = runTest {
        // Given
        val oldLabelIds = listOf(LabelId("1"), LabelId("2"), LabelId("3"))
        val oldPartialSelectedLabels = listOf(LabelId("5"))
        val newLabelIds = listOf(LabelId("1"), LabelId("2"), LabelId("4"))
        val removedLabels = listOf(LabelId("3"), LabelId("5"))
        val addedLabels = listOf(LabelId("4"))
        val expected = listOf(ConversationSample.AlphaAppFeedback).right()
        coEvery {
            conversationRepository.relabel(
                userId = UserIdSample.Primary,
                conversationIds = listOf(ConversationIdSample.Invoices),
                labelsToBeRemoved = removedLabels,
                labelsToBeAdded = addedLabels
            )
        } returns expected

        // When
        val actual = relabelConversation(
            UserIdSample.Primary,
            listOf(ConversationIdSample.Invoices),
            currentSelections = LabelSelectionList(
                selectedLabels = oldLabelIds,
                partiallySelectionLabels = oldPartialSelectedLabels
            ),
            updatedSelections = LabelSelectionList(
                selectedLabels = newLabelIds,
                partiallySelectionLabels = emptyList()
            )
        )

        // Then
        assertEquals(expected, actual)
        coVerify {
            conversationRepository.relabel(
                userId = UserIdSample.Primary,
                conversationIds = listOf(ConversationIdSample.Invoices),
                labelsToBeRemoved = removedLabels,
                labelsToBeAdded = addedLabels
            )
        }
    }

    @Test
    fun `use case passing correct add and remove label lists when partial selection is changed to add`() = runTest {
        // Given
        val oldLabelIds = listOf(LabelId("1"), LabelId("2"), LabelId("3"))
        val oldPartialSelectedLabels = listOf(LabelId("5"))
        val newLabelIds = listOf(LabelId("1"), LabelId("2"), LabelId("4"), LabelId("5"))
        val removedLabels = listOf(LabelId("3"))
        val addedLabels = listOf(LabelId("4"), LabelId("5"))
        val expected = listOf(ConversationSample.AlphaAppFeedback).right()
        coEvery {
            conversationRepository.relabel(
                userId = UserIdSample.Primary,
                conversationIds = listOf(ConversationIdSample.Invoices),
                labelsToBeRemoved = removedLabels,
                labelsToBeAdded = addedLabels
            )
        } returns expected

        // When
        val actual = relabelConversation(
            UserIdSample.Primary,
            listOf(ConversationIdSample.Invoices),
            currentSelections = LabelSelectionList(
                selectedLabels = oldLabelIds,
                partiallySelectionLabels = oldPartialSelectedLabels
            ),
            updatedSelections = LabelSelectionList(
                selectedLabels = newLabelIds,
                partiallySelectionLabels = emptyList()
            )
        )

        // Then
        assertEquals(expected, actual)
        coVerify {
            conversationRepository.relabel(
                userId = UserIdSample.Primary,
                conversationIds = listOf(ConversationIdSample.Invoices),
                labelsToBeRemoved = removedLabels,
                labelsToBeAdded = addedLabels
            )
        }
    }

    @Test
    fun `use case passing correct add and remove label lists when partial selection is unchanged`() = runTest {
        // Given
        val oldLabelIds = listOf(LabelId("1"), LabelId("2"), LabelId("3"))
        val partialSelectionLabels = listOf(LabelId("5"))
        val newLabelIds = listOf(LabelId("1"), LabelId("2"), LabelId("4"))
        val removedLabels = listOf(LabelId("3"))
        val addedLabels = listOf(LabelId("4"))
        val expected = listOf(ConversationSample.AlphaAppFeedback).right()
        coEvery {
            conversationRepository.relabel(
                userId = UserIdSample.Primary,
                conversationIds = listOf(ConversationIdSample.Invoices),
                labelsToBeRemoved = removedLabels,
                labelsToBeAdded = addedLabels
            )
        } returns expected

        // When
        val actual = relabelConversation(
            UserIdSample.Primary,
            listOf(ConversationIdSample.Invoices),
            currentSelections = LabelSelectionList(
                selectedLabels = oldLabelIds,
                partiallySelectionLabels = partialSelectionLabels
            ),
            updatedSelections = LabelSelectionList(
                selectedLabels = newLabelIds,
                partiallySelectionLabels = partialSelectionLabels
            )
        )

        // Then
        assertEquals(expected, actual)
        coVerify {
            conversationRepository.relabel(
                userId = UserIdSample.Primary,
                conversationIds = listOf(ConversationIdSample.Invoices),
                labelsToBeRemoved = removedLabels,
                labelsToBeAdded = addedLabels
            )
        }
    }
}
