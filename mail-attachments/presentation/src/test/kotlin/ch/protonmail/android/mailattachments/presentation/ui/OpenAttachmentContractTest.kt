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

package ch.protonmail.android.mailattachments.presentation.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class OpenAttachmentContractTest {

    private val context = mockk<Context> {
        every { getString(any()) } returns "Open with"
    }
    private val contract = OpenAttachmentContract()

    @Test
    fun `createIntent should include FLAG_ACTIVITY_NEW_TASK`() {
        // Given
        val input = OpenAttachmentInput(
            uri = Uri.parse("content://test/file.pdf"),
            mimeType = "application/pdf"
        )

        // When
        val chooserIntent = contract.createIntent(context, input)
        val innerIntent = chooserIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)

        // Then
        assertNotNull(innerIntent)
        assertTrue(innerIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `createIntent should include FLAG_GRANT_READ_URI_PERMISSION`() {
        // Given
        val input = OpenAttachmentInput(
            uri = Uri.parse("content://test/file.pdf"),
            mimeType = "application/pdf"
        )

        // When
        val chooserIntent = contract.createIntent(context, input)
        val innerIntent = chooserIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)

        // Then
        assertNotNull(innerIntent)
        assertTrue(innerIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }
}
