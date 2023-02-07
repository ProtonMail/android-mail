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
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import kotlin.test.assertEquals

class RelabelConversationTest {

    private val conversationRepository: ConversationRepository = mockk()
    private val relabelConversation = RelabelConversation(conversationRepository)

    @Test
    fun `when repository fails then error is returned`() = runTest {
        // Given
        val error = DataError.Local.NoDataCached.left()
        coEvery { conversationRepository.relabel(any(), any(), any(), any()) } returns error

        // When
        val result = relabelConversation(
            userId = UserIdSample.Primary,
            conversationId = ConversationIdSample.Invoices,
            currentLabelIds = listOf(LabelId("labelId")),
            updatedLabelIds = listOf(LabelId("labelId2"))
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
        coEvery {
            conversationRepository.relabel(
                userId = UserIdSample.Primary,
                conversationId = ConversationIdSample.Invoices,
                labelsToBeRemoved = removedLabels,
                labelsToBeAdded = addedLabels
            )
        } returns mockk()

        // When
        relabelConversation(UserIdSample.Primary, ConversationIdSample.Invoices, oldLabelIds, newLabelIds)

        // Then
        coVerify {
            conversationRepository.relabel(
                userId = UserIdSample.Primary,
                conversationId = ConversationIdSample.Invoices,
                labelsToBeRemoved = removedLabels,
                labelsToBeAdded = addedLabels
            )
        }
    }
}
