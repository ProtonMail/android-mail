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

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Instrumented: the flags are only observable on a real Intent built by Intent.createChooser.
@RunWith(AndroidJUnit4::class)
internal class OpenAttachmentContractTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val contract = OpenAttachmentContract()
    private val input = OpenAttachmentInput(
        uri = Uri.parse("content://test/file.pdf"),
        mimeType = "application/pdf"
    )

    @Test
    fun createIntentShouldIncludeFlagActivityNewTask() {
        // When
        val chooserIntent = contract.createIntent(context, input)
        val innerIntent = chooserIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)

        // Then
        assertNotNull(innerIntent)
        assertTrue(innerIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun createIntentShouldIncludeFlagGrantReadUriPermission() {
        // When
        val chooserIntent = contract.createIntent(context, input)
        val innerIntent = chooserIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)

        // Then
        assertNotNull(innerIntent)
        assertTrue(innerIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }
}
