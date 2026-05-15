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

package ch.protonmail.android.mailmailbox.domain.usecase

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maillabel.domain.model.CategoryLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelIdWithCategory
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmailbox.domain.repository.UnreadCountersRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class ObserveCategoryAwareUnreadCountTest {

    private val repository = mockk<UnreadCountersRepository>()

    private val observeCategoryAwareUnreadCount = ObserveCategoryAwareUnreadCount(repository)

    @Test
    fun `returns unread count from repository for selected label and category`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val selectedLabelWithCategory = MailLabelIdWithCategory(
            mailLabelId = MailLabelId.System(SystemLabelId.Inbox.labelId),
            categoryLabelId = CategoryLabelId("24")
        )
        every {
            repository.observeUnreadCount(userId, selectedLabelWithCategory)
        } returns flowOf(12)

        // When
        observeCategoryAwareUnreadCount(userId, selectedLabelWithCategory).test {
            // Then
            assertEquals(12, awaitItem())
            awaitComplete()
        }
    }
}

