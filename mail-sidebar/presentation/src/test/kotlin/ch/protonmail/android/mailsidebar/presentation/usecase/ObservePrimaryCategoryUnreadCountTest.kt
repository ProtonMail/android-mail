/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailsidebar.presentation.usecase

import app.cash.turbine.test
import ch.protonmail.android.maillabel.domain.model.CategorySystemLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.usecase.ResolveLocalCategoryLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveCategoryAwareUnreadCount
import ch.protonmail.android.testdata.maillabel.MailLabelTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class ObservePrimaryCategoryUnreadCountTest {

    private val userId = UserIdTestData.Primary

    private val inboxLabels = MailLabels(
        system = listOf(MailLabelTestData.inboxSystemLabel),
        folders = emptyList(),
        labels = emptyList()
    )

    private val observeMailLabels = mockk<ObserveMailLabels> {
        every { this@mockk(userId) } returns flowOf(inboxLabels)
    }
    private val resolveLocalCategoryLabelId = mockk<ResolveLocalCategoryLabelId>()
    private val observeCategoryAwareUnreadCount = mockk<ObserveCategoryAwareUnreadCount>()

    private val observePrimaryCategoryUnreadCount = ObservePrimaryCategoryUnreadCount(
        observeMailLabels = observeMailLabels,
        resolveLocalCategoryLabelId = resolveLocalCategoryLabelId,
        observeCategoryAwareUnreadCount = observeCategoryAwareUnreadCount
    )

    @Test
    fun `given the primary category resolves, then emits its unread count`() = runTest {
        // Given
        coEvery {
            resolveLocalCategoryLabelId(userId, CategorySystemLabelId.Primary)
        } returns MailLabelTestData.primaryCategoryLabelId
        every {
            observeCategoryAwareUnreadCount(userId, MailLabelTestData.inboxPrimarySystemLabelWithCategory)
        } returns flowOf(7)

        // When + Then
        observePrimaryCategoryUnreadCount(userId).test {
            assertEquals(7, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `given the primary category cannot be resolved, then emits null`() = runTest {
        // Given
        coEvery { resolveLocalCategoryLabelId(userId, CategorySystemLabelId.Primary) } returns null

        // When + Then
        observePrimaryCategoryUnreadCount(userId).test {
            assertNull(awaitItem())
            awaitComplete()
        }
        verify(exactly = 0) { observeCategoryAwareUnreadCount(any(), any()) }
    }

    @Test
    fun `given no inbox label, then emits null without resolving the category`() = runTest {
        // Given
        every { observeMailLabels(userId) } returns flowOf(
            MailLabels(system = emptyList(), folders = emptyList(), labels = emptyList())
        )

        // When + Then
        observePrimaryCategoryUnreadCount(userId).test {
            assertNull(awaitItem())
            awaitComplete()
        }
        verify(exactly = 0) { observeCategoryAwareUnreadCount(any(), any()) }
    }
}
