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

package ch.protonmail.android.maildetail.presentation.usecase

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClearMessageBodyCacheTest {

    private val context = mockk<Context>()
    private val tempRoot = File("build/tmp/test/clear-body-cache").apply {
        deleteRecursively()
        mkdirs()
    }

    @Test
    fun `deletes body cache directory when it exists`() = runTest {
        // Given
        every { context.cacheDir } returns tempRoot
        val bodyCacheDir = File(tempRoot, ClearMessageBodyCache.BODY_CACHE_DIRECTORY).apply {
            mkdirs()
            File(this, "sample.html").writeText("cached body")
        }
        val useCase = ClearMessageBodyCache(context, Dispatchers.Unconfined)

        // When
        useCase()

        // Then
        assertTrue(bodyCacheDir.exists())
        assertTrue(bodyCacheDir.listFiles().isNullOrEmpty())
    }

    @Test
    fun `does nothing when body cache directory does not exist`() = runTest {
        // Given
        every { context.cacheDir } returns tempRoot
        val bodyCacheDir = File(tempRoot, ClearMessageBodyCache.BODY_CACHE_DIRECTORY)

        val useCase = ClearMessageBodyCache(context, Dispatchers.Unconfined)

        // When
        useCase()

        // Then
        assertFalse(bodyCacheDir.exists())
        assertTrue(tempRoot.exists())
    }
}
