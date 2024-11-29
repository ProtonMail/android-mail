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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailbugreport.domain.usecase.GetAggregatedEventsZipFile
import ch.protonmail.android.mailbugreport.presentation.R
import ch.protonmail.android.mailbugreport.presentation.model.ApplicationLogsOperation
import ch.protonmail.android.mailbugreport.presentation.model.ApplicationLogsState
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val getAggregatedEventsZipFile: GetAggregatedEventsZipFile
) : ViewModel() {

    private val mutableState = MutableStateFlow(
        ApplicationLogsState(
            error = Effect.empty(),
            showApplicationLogs = Effect.empty(),
            showLogcat = Effect.empty(),
            share = Effect.empty(),
            export = Effect.empty()
        )
    )

    val state: StateFlow<ApplicationLogsState> = mutableState.asStateFlow()

    fun submit(action: ApplicationLogsOperation.ApplicationLogsAction) {
        viewModelScope.launch {
            when (action) {
                is ApplicationLogsOperation.ApplicationLogsAction.Export -> handleExportAction(action)
                is ApplicationLogsOperation.ApplicationLogsAction.View -> handleViewAction(action)
            }
        }
    }

    private suspend fun handleExportAction(action: ApplicationLogsOperation.ApplicationLogsAction.Export) {
        val zipFile = withContext(Dispatchers.IO) { getAggregatedEventsZipFile() }.getOrElse {
            mutableState.value = mutableState.value.copy(
                error = Effect.of(TextUiModel.TextRes(R.string.application_events_export_error))
            )
            return
        }

        when (action) {
            ApplicationLogsOperation.ApplicationLogsAction.Export.ExportLogs ->
                emitNewStateFromEvent(ApplicationLogsOperation.ApplicationLogsEvent.Export.ExportReady(zipFile))

            ApplicationLogsOperation.ApplicationLogsAction.Export.ShareLogs ->
                emitNewStateFromEvent(ApplicationLogsOperation.ApplicationLogsEvent.Export.ShareReady(zipFile))
        }
    }

    private fun handleViewAction(action: ApplicationLogsOperation.ApplicationLogsAction.View) {
        when (action) {
            ApplicationLogsOperation.ApplicationLogsAction.View.ViewEvents ->
                emitNewStateFromEvent(ApplicationLogsOperation.ApplicationLogsEvent.View.EventsReady)

            ApplicationLogsOperation.ApplicationLogsAction.View.ViewLogcat ->
                emitNewStateFromEvent(ApplicationLogsOperation.ApplicationLogsEvent.View.LogcatReady)
        }
    }

    private fun emitNewStateFromEvent(event: ApplicationLogsOperation.ApplicationLogsEvent) {
        when (event) {
            is ApplicationLogsOperation.ApplicationLogsEvent.Export.ShareReady -> {
                mutableState.update { mutableState.value.copy(share = Effect.of(event.file)) }
            }

            is ApplicationLogsOperation.ApplicationLogsEvent.Export.ExportReady -> {
                mutableState.update { mutableState.value.copy(export = Effect.of(event.file)) }
            }

            ApplicationLogsOperation.ApplicationLogsEvent.View.EventsReady -> {
                mutableState.update { mutableState.value.copy(showApplicationLogs = Effect.of(Unit)) }
            }

            ApplicationLogsOperation.ApplicationLogsEvent.View.LogcatReady -> {
                mutableState.update { mutableState.value.copy(showLogcat = Effect.of(Unit)) }
            }
        }
    }
}
