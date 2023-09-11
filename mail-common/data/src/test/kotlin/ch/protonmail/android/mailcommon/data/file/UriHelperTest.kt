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

package ch.protonmail.android.mailcommon.data.file

import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import android.database.Cursor
import android.net.Uri
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UriHelperTest {

    private val uri = mockk<Uri>()
    private val cursor = mockk<Cursor>(relaxUnitFun = true) {
        every { getColumnIndex(any()) } returns 0
        every { moveToFirst() } returns true
        every { getString(any()) } returns TestData.FileName
        every { getLong(any()) } returns TestData.FileSize
    }
    private val byteArrayInputStream = ByteArrayInputStream(TestData.FileContent)
    private val contentResolverHelper = mockk<ContentResolverHelper> {
        coEvery { openInputStream(uri) } returns byteArrayInputStream
        coEvery { getType(uri) } returns TestData.FileMimeType
        coEvery { query(uri) } returns cursor
    }

    private val testDispatcherProvider = TestDispatcherProvider()
    private val uriHelper = UriHelper(testDispatcherProvider, contentResolverHelper)

    @Test
    fun `should read file content from uri`() = runTest(testDispatcherProvider.Main) {
        // When
        val actual = uriHelper.readFromUri(uri)

        // Then
        assertEquals(byteArrayInputStream, actual)
    }

    @Test
    fun `should return null if reading file content from uri fails`() = runTest(testDispatcherProvider.Main) {
        // Given
        coEvery { contentResolverHelper.openInputStream(uri) } throws FileNotFoundException()

        // When
        val actual = uriHelper.readFromUri(uri)

        // Then
        assertNull(actual)
    }

    @Test
    fun `should get file information from uri`() = runTest(testDispatcherProvider.Main) {
        // Given
        val expected = FileInformation(TestData.FileName, TestData.FileSize, TestData.FileMimeType)

        // When
        val actual = uriHelper.getFileInformationFromUri(uri)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return null if one of the file information is null`() = runTest(testDispatcherProvider.Main) {
        // Given
        every { cursor.getString(any()) } returns null

        // When
        val actual = uriHelper.getFileInformationFromUri(uri)

        // Then
        assertNull(actual)
    }

    private object TestData {

        val FileContent = "I am a file content".toByteArray()
        const val FileName = "image.jpg"
        const val FileSize = 123L
        const val FileMimeType = "image/jpeg"
    }
}
