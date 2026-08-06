/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailcontentsearch.data.worker

import android.content.Context
import ch.protonmail.android.mailcontentsearch.data.R
import ch.protonmail.android.mailcontentsearch.data.worker.ContentIndexingNotification.IndexingProgress
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ContentIndexingNotificationTest {

    // Stubbed per string so the assertions pin down which one was chosen, not merely that some
    // resource was read.
    private val context = mockk<Context> {
        every {
            getString(R.string.content_search_notification_message_progress, *anyVararg())
        } returns "messages only"
        every {
            getString(R.string.content_search_notification_message_progress_multi_account, *anyVararg())
        } returns "messages and accounts"
    }

    @Test
    fun `a single account is reported as a percentage and a message count`() {
        // Given - the account counts would be noise: there is only ever one of them.
        val progress = progress(totalAccounts = 1)

        // When
        val text = progress.contentText(context)

        // Then
        assertEquals("messages only", text)
        verify { context.getString(R.string.content_search_notification_message_progress, 42, 416L, 1000L) }
    }

    @Test
    fun `several accounts get the account counts alongside the messages`() {
        // Given - the message counts are summed across accounts, so on their own they look like
        // one account's backfill stalling and then leaping forward.
        val progress = progress(completedAccounts = 1, totalAccounts = 3)

        // When
        val text = progress.contentText(context)

        // Then
        assertEquals("messages and accounts", text)
        verify {
            context.getString(
                R.string.content_search_notification_message_progress_multi_account,
                42,
                416L,
                1000L,
                1,
                3
            )
        }
    }

    @Test
    fun `the percentage is clamped to what a progress bar can take`() {
        // Given - Rust revises its totals as it goes, so the percentage can overshoot.
        assertEquals(100, progress().copy(percentage = 104.2).roundedPercentage)
        assertEquals(0, progress().copy(percentage = -1.0).roundedPercentage)
    }

    private fun progress(completedAccounts: Int = 0, totalAccounts: Int = 1) = IndexingProgress(
        percentage = 41.6,
        processedMessages = 416,
        totalMessages = 1000,
        completedAccounts = completedAccounts,
        totalAccounts = totalAccounts
    )
}
