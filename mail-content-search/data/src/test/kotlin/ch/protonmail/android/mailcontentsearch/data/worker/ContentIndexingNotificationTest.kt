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
        every { getString(R.string.content_search_notification_percentage, *anyVararg()) } returns "42%"
        every { getString(R.string.content_search_notification_preparing) } returns "Preparing…"
        every {
            getString(R.string.content_search_notification_progress, *anyVararg())
        } returns "user@proton.me · 42%"
    }

    @Test
    fun `the account address is shown next to the percentage`() {
        // When
        val text = ContentIndexingNotification.contentText(context, "user@proton.me", progress())

        // Then
        assertEquals("user@proton.me · 42%", text)
        verify { context.getString(R.string.content_search_notification_progress, "user@proton.me", "42%") }
    }

    @Test
    fun `an unsized backfill waits rather than reporting a percentage`() {
        // Given
        val text = ContentIndexingNotification.contentText(context, null, progress(isSized = false))

        // Then
        assertEquals("Preparing…", text)
    }

    @Test
    fun `the percentage stands alone until the address is known`() {
        // Given
        val text = ContentIndexingNotification.contentText(context, accountLabel = "  ", progress = progress())

        // Then
        assertEquals("42%", text)
    }

    @Test
    fun `the percentage is clamped to what a progress bar can take`() {
        // Given - Rust revises its totals as it goes, so the percentage can overshoot.
        assertEquals(100, progress().copy(percentage = 104.2).roundedPercentage)
        assertEquals(0, progress().copy(percentage = -1.0).roundedPercentage)
    }

    private fun progress(isSized: Boolean = true) = IndexingProgress(percentage = 41.6, isSized = isSized)
}
