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

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.mailattachments.presentation.R
import ch.protonmail.android.mailattachments.presentation.model.FileContent
import ch.protonmail.android.mailattachments.presentation.model.FileSaveState
import ch.protonmail.android.mailattachments.presentation.viewmodel.FileSaverViewModel
import timber.log.Timber

/**
 * A Composable that abstracts the save of file contents to an external location.
 */
@Composable
@Suppress("UseComposableActions")
fun fileSaver(
    onFileSaving: (() -> Unit)? = null,
    onFileSaved: ((String) -> Unit),
    onError: ((String) -> Unit)
): (FileContent) -> Unit {
    val viewModel = hiltViewModel<FileSaverViewModel>()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    val fileSaveLauncher = rememberLauncherForActivityResult(
        contract = CreateDocumentWithMimeType()
    ) { destinationUri: Uri? ->
        if (destinationUri != null) {
            // The source is resolved by the ViewModel at this point, not when the save was requested: the file may
            // have been evicted from the cache while the picker was open.
            viewModel.performSave(destinationUri)
        } else {
            viewModel.resetState()
        }
    }

    val fileSavedString = stringResource(R.string.file_saved)
    val fileSavedFallbackString = stringResource(R.string.file_saved_fallback)
    val fileSavedError = stringResource(R.string.error_saving_file)

    LaunchedEffect(saveState) {
        when (val state = saveState) {
            is FileSaveState.RequestingSave -> {
                val attachmentInput = SaveAttachmentInput(
                    state.content.name,
                    state.content.mimeType
                )

                try {
                    fileSaveLauncher.launch(attachmentInput)
                    viewModel.markLaunchAsConsumed()
                } catch (_: ActivityNotFoundException) {
                    Timber.d("Unable to find a suitable target for saving - fallback to Downloads folder")
                    viewModel.performSaveToDownloadFolder(state.content)
                }
            }

            is FileSaveState.Saving -> {
                onFileSaving?.invoke()
            }

            is FileSaveState.Saved.UserPicked -> {
                onFileSaved.invoke(fileSavedString)
                viewModel.resetState()
            }

            is FileSaveState.Saved.FallbackLocation -> {
                onFileSaved.invoke(fileSavedFallbackString)
                viewModel.resetState()
            }

            is FileSaveState.Error -> {
                onError.invoke(fileSavedError)
                viewModel.resetState()
            }

            is FileSaveState.Idle,
            is FileSaveState.WaitingForUser -> Unit
        }
    }

    return { content -> viewModel.requestSave(content) }
}
