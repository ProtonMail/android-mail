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

package ch.protonmail.android.mailbugreport.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailbugreport.domain.usecase.GetAggregatedEventsZipFile
import ch.protonmail.android.mailbugreport.presentation.R
import ch.protonmail.android.mailbugreport.presentation.model.ApplicationLogsOperation
import ch.protonmail.android.mailbugreport.presentation.model.ApplicationLogsOperation.ApplicationLogsEvent.Export.SendByEmailReady
import ch.protonmail.android.mailbugreport.presentation.model.ApplicationLogsOperation.ApplicationLogsEvent.Export.ShareReady
import ch.protonmail.android.mailbugreport.presentation.model.ApplicationLogsState
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ApplicationLogsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getAggregatedEventsZipFile: GetAggregatedEventsZipFile
) : ViewModel() {

    private val mutableState = MutableStateFlow(
        ApplicationLogsState(
            error = Effect.empty(),
            showApplicationLogs = Effect.empty(),
            showRustLogs = Effect.empty(),
            showLogcat = Effect.empty(),
            share = Effect.empty(),
            sendByEmail = Effect.empty(),
            export = Effect.empty()
        )
    )

    val state: StateFlow<ApplicationLogsState> = mutableState.asStateFlow()

    fun submit(action: ApplicationLogsOperation.ApplicationLogsAction) {
        viewModelScope.launch {
            when (action) {
                is ApplicationLogsOperation.ApplicationLogsAction.Export.ExportLogs -> exportToUri(action.uri)
                is ApplicationLogsOperation.ApplicationLogsAction.Export.ShareLogs -> handleZipRequest(::ShareReady)
                is ApplicationLogsOperation.ApplicationLogsAction.Export.SendLogsByEmail ->
                    handleZipRequest(::SendByEmailReady)
                is ApplicationLogsOperation.ApplicationLogsAction.View -> handleViewAction(action)
            }
        }
    }

    private suspend fun handleZipRequest(onReady: (Uri) -> ApplicationLogsOperation.ApplicationLogsEvent.Export) {
        val zipFileResult = withContext(Dispatchers.IO) {
            getAggregatedEventsZipFile()
                .mapCatching { zipFile ->
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", zipFile)
                }
        }

        zipFileResult.fold(
            onSuccess = { uri -> emitNewStateFromEvent(onReady(uri)) },
            onFailure = {
                emitNewStateFromEvent(ApplicationLogsOperation.ApplicationLogsEvent.Export.ExportError)
            }
        )
    }

    private suspend fun exportToUri(uri: Uri) {
        val success = withContext(Dispatchers.IO) {
            runCatching {
                val zipFile = getAggregatedEventsZipFile().getOrThrow()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    zipFile.inputStream().use { inputStream -> inputStream.copyTo(outputStream) }
                }
            }.isSuccess
        }

        if (!success) emitNewStateFromEvent(ApplicationLogsOperation.ApplicationLogsEvent.Export.ExportError)
    }

    private fun handleViewAction(action: ApplicationLogsOperation.ApplicationLogsAction.View) {
        when (action) {
            ApplicationLogsOperation.ApplicationLogsAction.View.ViewAppEvents ->
                emitNewStateFromEvent(ApplicationLogsOperation.ApplicationLogsEvent.View.AppEventsReady)

            ApplicationLogsOperation.ApplicationLogsAction.View.ViewRustEvents ->
                emitNewStateFromEvent(ApplicationLogsOperation.ApplicationLogsEvent.View.RustEventsReady)

            ApplicationLogsOperation.ApplicationLogsAction.View.ViewLogcat ->
                emitNewStateFromEvent(ApplicationLogsOperation.ApplicationLogsEvent.View.LogcatReady)
        }
    }

    private fun emitNewStateFromEvent(event: ApplicationLogsOperation.ApplicationLogsEvent) {
        when (event) {
            is ApplicationLogsOperation.ApplicationLogsEvent.Export.ShareReady -> {
                mutableState.update { mutableState.value.copy(share = Effect.of(event.uri)) }
            }

            is ApplicationLogsOperation.ApplicationLogsEvent.Export.SendByEmailReady -> {
                mutableState.update { mutableState.value.copy(sendByEmail = Effect.of(event.uri)) }
            }

            ApplicationLogsOperation.ApplicationLogsEvent.View.AppEventsReady -> {
                mutableState.update { mutableState.value.copy(showApplicationLogs = Effect.of(Unit)) }
            }

            ApplicationLogsOperation.ApplicationLogsEvent.View.RustEventsReady -> {
                mutableState.update { mutableState.value.copy(showRustLogs = Effect.of(Unit)) }
            }

            ApplicationLogsOperation.ApplicationLogsEvent.View.LogcatReady -> {
                mutableState.update { mutableState.value.copy(showLogcat = Effect.of(Unit)) }
            }

            ApplicationLogsOperation.ApplicationLogsEvent.Export.ExportError -> {
                mutableState.update {
                    mutableState.value.copy(
                        error = Effect.of(TextUiModel.TextRes(R.string.application_events_export_error))
                    )
                }
            }
        }
    }

}
