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

package ch.protonmail.android.mailattachments.presentation.viewmodel

import android.net.Uri
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailattachments.domain.model.AttachmentId
import ch.protonmail.android.mailattachments.domain.usecase.GetDecryptedAttachmentUri
import ch.protonmail.android.mailattachments.presentation.ExternalAttachmentErrorResult
import ch.protonmail.android.mailattachments.presentation.ExternalAttachmentsHandler
import ch.protonmail.android.mailattachments.presentation.model.FileContent
import ch.protonmail.android.mailattachments.presentation.model.FileSaveState
import ch.protonmail.android.mailcommon.domain.model.DataError
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class FileSaverViewModelTest {

    private lateinit var viewModel: FileSaverViewModel
    private val attachmentsHandler = mockk<ExternalAttachmentsHandler>()
    private val getDecryptedAttachmentUri = mockk<GetDecryptedAttachmentUri>()

    @BeforeTest
    fun setup() {
        viewModel = FileSaverViewModel(attachmentsHandler, getDecryptedAttachmentUri)
    }

    @AfterTest
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `initial state should be Idle`() = runTest {
        // When + Then
        viewModel.saveState.test {
            assertEquals(FileSaveState.Idle, awaitItem())
        }
    }

    @Test
    fun `requestSave should emit RequestingSave state with content`() = runTest {
        // When + Then
        viewModel.saveState.test {
            assertEquals(FileSaveState.Idle, awaitItem())

            viewModel.requestSave(SourceFileContent)

            val state = awaitItem()
            assertEquals(SourceFileContent, (state as FileSaveState.RequestingSave).content)
        }
    }

    @Test
    fun `markLaunchAsConsumed should transition RequestingSave to WaitingForUser`() = runTest {
        // When + Then
        viewModel.saveState.test {
            assertEquals(FileSaveState.Idle, awaitItem())

            viewModel.requestSave(SourceFileContent)
            val requestingState = awaitItem()
            assertTrue(requestingState is FileSaveState.RequestingSave)

            viewModel.markLaunchAsConsumed()

            val waitingState = awaitItem()
            assertEquals(SourceFileContent, (waitingState as FileSaveState.WaitingForUser).content)
        }
    }

    @Test
    fun `markLaunchAsConsumed should not change state if not RequestingSave`() = runTest {
        // When + Then
        viewModel.saveState.test {
            assertEquals(FileSaveState.Idle, awaitItem())

            viewModel.markLaunchAsConsumed()

            // State should remain Idle, no new emission
            expectNoEvents()
        }
    }

    @Test
    fun `performSave should resolve the source at save time and emit Saved state`() = runTest {
        // Given
        val destinationUri = mockk<Uri>()
        coEvery { getDecryptedAttachmentUri(PrimaryUserId, SourceAttachmentId) } returns SourceUri.right()
        coEvery { attachmentsHandler.copyUriToDestination(SourceUri, destinationUri) } returns Unit.right()

        // When + Then
        viewModel.saveState.test {
            assertEquals(FileSaveState.Idle, awaitItem())

            viewModel.moveToWaitingForUser()
            skipItems(2)

            viewModel.performSave(destinationUri)

            assertEquals(FileSaveState.Saving, awaitItem())
            assertEquals(FileSaveState.Saved.UserPicked, awaitItem())
        }

        coVerify(exactly = 1) { getDecryptedAttachmentUri(PrimaryUserId, SourceAttachmentId) }
        coVerify(exactly = 1) { attachmentsHandler.copyUriToDestination(SourceUri, destinationUri) }
        confirmVerified(attachmentsHandler)
    }

    @Test
    fun `performSave should emit Error and skip the copy when the source can no longer be resolved`() = runTest {
        // Given
        val destinationUri = mockk<Uri>()
        coEvery {
            getDecryptedAttachmentUri(PrimaryUserId, SourceAttachmentId)
        } returns DataError.Local.FailedToReadFile.left()

        // When + Then
        viewModel.saveState.test {
            assertEquals(FileSaveState.Idle, awaitItem())

            viewModel.moveToWaitingForUser()
            skipItems(2)

            viewModel.performSave(destinationUri)

            assertEquals(FileSaveState.Saving, awaitItem())
            assertEquals(FileSaveState.Error(ExternalAttachmentErrorResult.SourceUnavailable), awaitItem())
        }

        coVerify(exactly = 1) { getDecryptedAttachmentUri(PrimaryUserId, SourceAttachmentId) }
        confirmVerified(attachmentsHandler)
    }

    @Test
    fun `performSave should reset to Idle when no save is pending`() = runTest {
        // When + Then
        viewModel.saveState.test {
            assertEquals(FileSaveState.Idle, awaitItem())

            viewModel.performSave(mockk<Uri>())

            expectNoEvents()
        }

        confirmVerified(attachmentsHandler, getDecryptedAttachmentUri)
    }

    @Test
    fun `performSave should emit Error state when unable to create uri`() = runTest {
        // Given
        val destinationUri = mockk<Uri>()
        coEvery { getDecryptedAttachmentUri(PrimaryUserId, SourceAttachmentId) } returns SourceUri.right()
        coEvery {
            attachmentsHandler.copyUriToDestination(SourceUri, destinationUri)
        } returns ExternalAttachmentErrorResult.UnableToCreateUri.left()

        // When + Then
        viewModel.saveState.test {
            assertEquals(FileSaveState.Idle, awaitItem())

            viewModel.moveToWaitingForUser()
            skipItems(2)

            viewModel.performSave(destinationUri)
            assertEquals(FileSaveState.Saving, awaitItem())

            assertEquals(FileSaveState.Error(ExternalAttachmentErrorResult.UnableToCreateUri), awaitItem())
        }
    }

    @Test
    fun `performSave should emit Error state when an error occurs during copy`() = runTest {
        // Given
        val destinationUri = mockk<Uri>()
        coEvery { getDecryptedAttachmentUri(PrimaryUserId, SourceAttachmentId) } returns SourceUri.right()
        coEvery {
            attachmentsHandler.copyUriToDestination(SourceUri, destinationUri)
        } returns ExternalAttachmentErrorResult.UnableToCopy.left()

        // When + Then
        viewModel.saveState.test {
            assertEquals(FileSaveState.Idle, awaitItem())

            viewModel.moveToWaitingForUser()
            skipItems(2)

            viewModel.performSave(destinationUri)

            assertEquals(FileSaveState.Saving, awaitItem())

            val errorState = awaitItem()
            assertTrue(errorState is FileSaveState.Error)
            assertEquals(errorState, FileSaveState.Error(ExternalAttachmentErrorResult.UnableToCopy))
        }
    }

    @Test
    fun `performSaveToDownloadFolder should resolve the source and emit Saved state`() = runTest {
        // Given
        coEvery { getDecryptedAttachmentUri(PrimaryUserId, SourceAttachmentId) } returns SourceUri.right()
        coEvery {
            attachmentsHandler.saveFileToDownloadsFolder(SourceUri, SourceFileContent.name, SourceFileContent.mimeType)
        } returns Unit.right()

        // When + Then
        viewModel.saveState.test {
            assertEquals(FileSaveState.Idle, awaitItem())

            viewModel.performSaveToDownloadFolder(SourceFileContent)

            assertEquals(FileSaveState.Saving, awaitItem())
            assertEquals(FileSaveState.Saved.FallbackLocation, awaitItem())
        }

        coVerify(exactly = 1) {
            attachmentsHandler.saveFileToDownloadsFolder(SourceUri, SourceFileContent.name, SourceFileContent.mimeType)
        }
        confirmVerified(attachmentsHandler)
    }

    @Test
    fun `performSaveToDownloadFolder should emit Error when the source can no longer be resolved`() = runTest {
        // Given
        coEvery {
            getDecryptedAttachmentUri(PrimaryUserId, SourceAttachmentId)
        } returns DataError.Local.FailedToReadFile.left()

        // When + Then
        viewModel.saveState.test {
            assertEquals(FileSaveState.Idle, awaitItem())

            viewModel.performSaveToDownloadFolder(SourceFileContent)

            assertEquals(FileSaveState.Saving, awaitItem())
            assertEquals(FileSaveState.Error(ExternalAttachmentErrorResult.SourceUnavailable), awaitItem())
        }

        confirmVerified(attachmentsHandler)
    }

    @Test
    fun `resetState should return state to Idle`() = runTest {
        // When + Then
        viewModel.saveState.test {
            assertEquals(FileSaveState.Idle, awaitItem())

            viewModel.requestSave(SourceFileContent)
            assertTrue(awaitItem() is FileSaveState.RequestingSave)

            viewModel.resetState()
            assertEquals(FileSaveState.Idle, awaitItem())
        }
    }

    private fun FileSaverViewModel.moveToWaitingForUser() {
        requestSave(SourceFileContent)
        markLaunchAsConsumed()
    }

    private companion object {

        val PrimaryUserId = UserId("user-id")
        val SourceAttachmentId = AttachmentId("attachment-1")
        val SourceUri = mockk<Uri>()
        val SourceFileContent = FileContent(
            name = "fileName.pdf",
            mimeType = "application/pdf",
            userId = PrimaryUserId,
            attachmentId = SourceAttachmentId
        )
    }
}
