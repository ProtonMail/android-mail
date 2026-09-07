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

import java.io.File
import android.content.Context
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailbugreport.domain.LogsFileHandler
import ch.protonmail.android.mailbugreport.domain.annotations.AppLogsFileHandler
import ch.protonmail.android.mailbugreport.domain.annotations.RustLogsFileHandler
import ch.protonmail.android.mailbugreport.domain.provider.LogcatProvider
import ch.protonmail.android.mailbugreport.presentation.model.ApplicationLogsFileUiModel
import ch.protonmail.android.mailbugreport.presentation.model.ApplicationLogsPeekViewOperation
import ch.protonmail.android.mailbugreport.presentation.model.ApplicationLogsPeekViewState
import ch.protonmail.android.mailbugreport.presentation.model.ApplicationLogsViewItemMode
import ch.protonmail.android.mailbugreport.presentation.ui.ApplicationLogsPeekView
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.deserialize
import javax.inject.Inject

@HiltViewModel
class ApplicationLogsPeekViewViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @AppLogsFileHandler private val logsFileHandler: LogsFileHandler,
    @RustLogsFileHandler private val rustLogsFileHandler: LogsFileHandler,
    private val logcatProvider: LogcatProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val openMode = savedStateHandle
        .get<String>(ApplicationLogsPeekView.ApplicationLogsViewMode)
        ?.deserialize<ApplicationLogsViewItemMode>()

    private val mutableState = MutableStateFlow<ApplicationLogsPeekViewState>(ApplicationLogsPeekViewState.Loading)
    val state = mutableState.asStateFlow()

    fun submit(action: ApplicationLogsPeekViewOperation.ViewAction) {
        viewModelScope.launch {
            when (action) {
                ApplicationLogsPeekViewOperation.ViewAction.DisplayFileContent -> handleItemDisplay(openMode)
            }
        }
    }

    private suspend fun handleItemDisplay(openMode: ApplicationLogsViewItemMode?) {
        emitNewStateFromEvent(ApplicationLogsPeekViewOperation.ViewEvent.Loading)
        openMode ?: return emitNewStateFromEvent(ApplicationLogsPeekViewOperation.ViewEvent.InvalidOpenMode)

        val file = withContext(Dispatchers.IO) {
            when (openMode) {
                ApplicationLogsViewItemMode.AppEvents -> logsFileHandler.getLastLogFile()
                ApplicationLogsViewItemMode.RustEvents -> rustLogsFileHandler.getLastLogFile()
                ApplicationLogsViewItemMode.Logcat -> logcatProvider.getLogcatFile().getOrNull()
            }?.takeIf { it.exists() }
        }
            ?: return emitNewStateFromEvent(ApplicationLogsPeekViewOperation.ViewEvent.FileContentLoadError)

        emitNewStateFromEvent(ApplicationLogsPeekViewOperation.ViewEvent.FileContentLoaded(file))
    }

    private suspend fun emitNewStateFromEvent(event: ApplicationLogsPeekViewOperation.ViewEvent) {
        when (event) {
            is ApplicationLogsPeekViewOperation.ViewEvent.FileContentLoaded -> mutableState.update {
                ApplicationLogsPeekViewState.Loaded(event.file.toUiModel())
            }

            ApplicationLogsPeekViewOperation.ViewEvent.FileContentLoadError,
            ApplicationLogsPeekViewOperation.ViewEvent.InvalidOpenMode -> {
                mutableState.update { ApplicationLogsPeekViewState.Error }
            }

            ApplicationLogsPeekViewOperation.ViewEvent.Loading -> mutableState.update {
                ApplicationLogsPeekViewState.Loading
            }
        }
    }

    private suspend fun File.toUiModel() = withContext(Dispatchers.IO) {
        val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", this@toUiModel)
        val lines = readLines()
        val chunkedContents = lines.chunked(ChunkLines).map { it.joinToString(separator = "\n") }
        // A single line can be pathologically long, and Compose cannot lay out an unbounded width.
        val longestLineChars = lines.maxOfOrNull { it.length }?.coerceAtMost(MaxLineChars) ?: 0
        ApplicationLogsFileUiModel(fileUri, name, chunkedContents, longestLineChars)
    }

    private companion object {

        const val ChunkLines = 200
        const val MaxLineChars = 2_000
    }
}
