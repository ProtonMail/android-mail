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

package ch.protonmail.android.mailmailbox.data.repository

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.data.mapper.LocalCategoryLabelId
import ch.protonmail.android.mailcommon.data.mapper.LocalLabelId
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maillabel.data.local.LabelDataSource
import ch.protonmail.android.maillabel.domain.model.CategoryLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelIdWithCategory
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.model.UnreadCounter
import ch.protonmail.android.testdata.label.rust.LocalLabelTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import ch.protonmail.android.maillabel.domain.model.LabelId
import org.junit.Test
import kotlin.test.assertEquals

class UnreadCountersRepositoryImplTest {

    private val labelDataSource = mockk<LabelDataSource>()

    private val repository = UnreadCountersRepositoryImpl(labelDataSource)

    @Test
    fun `combines system labels and folders unread counters`() = runTest {
        // Given
        val expectedCounters = listOf(systemUnreadMessageCounter, labelUnreadMessageCounter, folderUnreadMessageCounter)
        coEvery { labelDataSource.observeSystemLabels(userId) } returns flowOf(
            listOf(LocalLabelTestData.localSystemLabelWithCount)
        )
        coEvery { labelDataSource.observeMessageLabels(userId) } returns flowOf(
            listOf(LocalLabelTestData.localMessageLabelWithCount)
        )
        coEvery { labelDataSource.observeMessageFolders(userId) } returns flowOf(
            listOf(LocalLabelTestData.localMessageFolderWithCount)
        )

        // When
        repository.observeUnreadCounters(userId).test {
            // Then
            val actual = awaitItem()
            assertEquals(expectedCounters, actual)
            awaitComplete()
        }

    }

    @Test
    fun `observes unread count for selected label with category`() = runTest {
        // Given
        val selectedLabelWithCategory = MailLabelIdWithCategory(
            mailLabelId = MailLabelId.System(SystemLabelId.Inbox.labelId),
            categoryLabelId = CategoryLabelId("24")
        )
        every {
            labelDataSource.observeUnreadCount(
                userId = userId,
                labelId = LocalLabelId(SystemLabelId.Inbox.labelId.id.toULong()),
                categoryLabelId = LocalCategoryLabelId(24u)
            )
        } returns flowOf(9)

        // When
        repository.observeUnreadCount(userId, selectedLabelWithCategory).test {
            // Then
            assertEquals(9, awaitItem())
            awaitComplete()
        }
    }

    companion object TestData {
        private val userId = UserIdSample.Primary

        val systemUnreadMessageCounter = UnreadCounter(LabelId("1"), 0)
        val labelUnreadMessageCounter = UnreadCounter(LabelId("100"), 0)
        val folderUnreadMessageCounter = UnreadCounter(LabelId("200"), 7)
    }
}
