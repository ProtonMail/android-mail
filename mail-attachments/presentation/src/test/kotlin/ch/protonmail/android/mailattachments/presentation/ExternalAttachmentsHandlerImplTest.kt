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

package ch.protonmail.android.mailattachments.presentation

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailattachments.presentation.usecase.GenerateUniqueFileName
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ExternalAttachmentsHandlerImplTest {

    private val context = mockk<Context>()
    private lateinit var contentResolver: ContentResolver
    private lateinit var attachmentsHandler: ExternalAttachmentsHandlerImpl

    private val generateUniqueFileName = mockk<GenerateUniqueFileName> {
        every { this@mockk.invoke(any()) } returns "fileName.pdf"
    }

    @BeforeTest
    fun setup() {
        contentResolver = mockk(relaxed = true)
        every { context.contentResolver } returns contentResolver
        attachmentsHandler = ExternalAttachmentsHandlerImpl(context, Dispatchers.Unconfined, generateUniqueFileName)
    }

    @AfterTest
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `copyUriToDestination should successfully save file`() = runTest {
        // Given
        val sourceUri = mockk<Uri>()
        val destinationUri = mockk<Uri>()
        val inputStream = ByteArrayInputStream("test content".toByteArray())
        val outputStream = ByteArrayOutputStream()

        every { contentResolver.openInputStream(sourceUri) } returns inputStream
        every { contentResolver.openOutputStream(destinationUri) } returns outputStream

        // When + Then
        val actual = attachmentsHandler.copyUriToDestination(sourceUri, destinationUri)

        assertEquals(Unit.right(), actual)
        verify(exactly = 1) { contentResolver.openInputStream(sourceUri) }
        verify(exactly = 1) { contentResolver.openOutputStream(destinationUri) }
        confirmVerified(contentResolver)
    }

    @Test
    fun `copyUriToDestination should emit an Error when input stream is null`() = runTest {
        // Given
        val sourceUri = mockk<Uri>()
        val destinationUri = mockk<Uri>()
        every { contentResolver.openInputStream(sourceUri) } returns null

        // When
        val actual = attachmentsHandler.copyUriToDestination(sourceUri, destinationUri)

        // Then
        assertEquals(ExternalAttachmentErrorResult.UnableToCopy.left(), actual)
        verify(exactly = 1) { contentResolver.openInputStream(sourceUri) }
        confirmVerified(contentResolver)
    }

    @Test
    fun `copyUriToDestination should emit an Error when output stream is null`() = runTest {
        // Given
        val sourceUri = mockk<Uri>()
        val destinationUri = mockk<Uri>()
        val inputStream = ByteArrayInputStream("test content".toByteArray())

        every { contentResolver.openInputStream(sourceUri) } returns inputStream
        every { contentResolver.openOutputStream(destinationUri) } returns null

        // When
        val actual = attachmentsHandler.copyUriToDestination(sourceUri, destinationUri)

        // Then
        assertEquals(ExternalAttachmentErrorResult.UnableToCopy.left(), actual)
        verify(exactly = 1) { contentResolver.openInputStream(sourceUri) }
        verify(exactly = 1) { contentResolver.openOutputStream(destinationUri) }
        confirmVerified(contentResolver)
    }

    @Test
    fun `saveDataToDestination should successfully save data`() = runTest {
        // Given
        val destinationUri = mockk<Uri>()
        val mimeType = "image/png"
        val data = byteArrayOf(1, 2, 3, 4)
        val outputStream = ByteArrayOutputStream()

        every { contentResolver.openOutputStream(destinationUri) } returns outputStream

        // When
        val actual = attachmentsHandler.saveDataToDestination(destinationUri, mimeType, data)

        // Then
        assertEquals(Unit.right(), actual)
        assertEquals(data.toList(), outputStream.toByteArray().toList())
        verify(exactly = 1) { contentResolver.openOutputStream(destinationUri) }
        confirmVerified(contentResolver)
    }

    @Test
    fun `saveDataToDestination should emit an Error when output stream is null`() = runTest {
        // Given
        val destinationUri = mockk<Uri>()
        val mimeType = "image/png"
        val data = byteArrayOf(1, 2, 3, 4)

        every { contentResolver.openOutputStream(destinationUri) } returns null

        // When
        val actual = attachmentsHandler.saveDataToDestination(destinationUri, mimeType, data)

        // Then
        assertEquals(ExternalAttachmentErrorResult.UnableToCopy.left(), actual)
        verify(exactly = 1) { contentResolver.openOutputStream(destinationUri) }
        confirmVerified(contentResolver)
    }

    @Test
    fun `copyUriToDestination should complete even when coroutine is cancelled`() = runTest {
        // Given
        val handler = ExternalAttachmentsHandlerImpl(
            context, StandardTestDispatcher(testScheduler), generateUniqueFileName
        )
        val sourceUri = mockk<Uri>()
        val destinationUri = mockk<Uri>()
        val content = "test content".toByteArray()
        val outputStream = ByteArrayOutputStream()

        val inputStream = ByteArrayInputStream(content)

        every { contentResolver.openInputStream(sourceUri) } returns inputStream
        every { contentResolver.openOutputStream(destinationUri) } returns outputStream

        // When
        // CoroutineStart.UNDISPATCHED lets the coroutine run until it suspends at withContext(ioDispatcher),
        // then we cancel the parent job before the IO block is dispatched
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            handler.copyUriToDestination(sourceUri, destinationUri)
        }
        job.cancel()
        advanceUntilIdle()

        // Then
        assertEquals(content.toList(), outputStream.toByteArray().toList())
    }
}
