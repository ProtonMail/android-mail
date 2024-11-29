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

package ch.protonmail.android.mailbugreport.presentation.ui

import java.io.File
import java.io.IOException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.mailbugreport.presentation.R
import ch.protonmail.android.mailbugreport.presentation.model.ApplicationLogsOperation.ApplicationLogsAction.Export
import ch.protonmail.android.mailbugreport.presentation.model.ApplicationLogsOperation.ApplicationLogsAction.View
import ch.protonmail.android.mailbugreport.presentation.model.ApplicationLogsViewItemMode
import ch.protonmail.android.mailbugreport.presentation.utils.ApplicationLogsUtils.shareLogs
import ch.protonmail.android.mailbugreport.presentation.viewmodel.ApplicationLogsViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import me.proton.core.compose.component.ProtonSettingsTopBar
import me.proton.core.presentation.utils.showToast
import timber.log.Timber

@Composable
fun ApplicationLogsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onViewItemClick: (ApplicationLogsViewItemMode) -> Unit,
    viewModel: ApplicationLogsViewModel = hiltViewModel()
) {
    val scaffoldState = rememberScaffoldState()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var file by remember { mutableStateOf(File("")) }

    val fileSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    file.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } catch (e: IOException) {
                Timber.e("FileSave", "Error copying file", e)
            }
        }
    }

    val actions = ApplicationLogsScreenList.Actions(
        onExport = { viewModel.submit(Export.ExportLogs) },
        onShare = { viewModel.submit(Export.ShareLogs) },
        onShowLogcat = { viewModel.submit(View.ViewLogcat) },
        onShowEvents = { viewModel.submit(View.ViewEvents) }
    )

    ConsumableLaunchedEffect(state.showApplicationLogs) {
        onViewItemClick(ApplicationLogsViewItemMode.Events)
    }

    ConsumableLaunchedEffect(state.showLogcat) {
        onViewItemClick(ApplicationLogsViewItemMode.Logcat)
    }

    ConsumableLaunchedEffect(state.share) {
        context.shareLogs(it)
    }

    ConsumableLaunchedEffect(state.export) {
        file = it
        fileSaveLauncher.launch(it.name)
    }

    ConsumableTextEffect(state.error) { message ->
        context.showToast(message)
    }

    Scaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        topBar = {
            ProtonSettingsTopBar(
                title = stringResource(R.string.application_events_title),
                onBackClick = onBackClick
            )
        },
        content = { paddingValues ->
            ApplicationLogsScreenList(Modifier.padding(paddingValues), actions)
        }
    )
}
