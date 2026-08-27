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

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.component.appbar.ProtonMediumTopAppBar
import ch.protonmail.android.mailbugreport.domain.helper.EventsFileNameHelper
import ch.protonmail.android.mailbugreport.presentation.R
import ch.protonmail.android.mailbugreport.presentation.model.ApplicationLogsOperation.ApplicationLogsAction.Export
import ch.protonmail.android.mailbugreport.presentation.model.ApplicationLogsOperation.ApplicationLogsAction.View
import ch.protonmail.android.mailbugreport.presentation.model.ApplicationLogsViewItemMode
import ch.protonmail.android.mailbugreport.presentation.utils.ApplicationLogsUtils.shareLogs
import ch.protonmail.android.mailbugreport.presentation.viewmodel.ApplicationLogsViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import me.proton.core.presentation.utils.showToast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationLogsScreen(
    modifier: Modifier = Modifier,
    actions: ApplicationLogsScreen.Actions,
    viewModel: ApplicationLogsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isStandalone = LocalAppLogsEntryPointIsStandalone.current

    val fileSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.submit(Export.ExportLogs(uri)) }
    }

    val screenListActions = ApplicationLogsScreenList.Actions(
        onExport = { fileSaveLauncher.launch(EventsFileNameHelper.generateTimestampedFilename()) },
        onShare = { viewModel.submit(Export.ShareLogs) },
        onSendByEmail = actions.onComposeWithLogs?.let { { viewModel.submit(Export.SendLogsByEmail) } },
        onShowLogcat = { viewModel.submit(View.ViewLogcat) },
        onShowRustEvents = { viewModel.submit(View.ViewRustEvents) },
        onShowAppEvents = { viewModel.submit(View.ViewAppEvents) },
        onFeatureFlagNavigation = actions.onFeatureFlagsNavigation
    )

    ConsumableLaunchedEffect(state.showApplicationLogs) {
        actions.onViewItemClick(ApplicationLogsViewItemMode.AppEvents)
    }

    ConsumableLaunchedEffect(state.showRustLogs) {
        actions.onViewItemClick(ApplicationLogsViewItemMode.RustEvents)
    }

    ConsumableLaunchedEffect(state.showLogcat) {
        actions.onViewItemClick(ApplicationLogsViewItemMode.Logcat)
    }

    ConsumableLaunchedEffect(state.share) {
        context.shareLogs(it)
    }

    ConsumableLaunchedEffect(state.sendByEmail) {
        actions.onComposeWithLogs?.invoke(it)
    }

    ConsumableTextEffect(state.error) { message ->
        context.showToast(message)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ProtonMediumTopAppBar(
                title = { Text(text = stringResource(R.string.application_events_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.presentation_back)
                        )
                    }
                }
            )
        },
        content = { paddingValues ->
            ApplicationLogsScreenList(
                modifier = Modifier.padding(paddingValues),
                appVersion = state.appVersion,
                actions = screenListActions,
                isStandalone = isStandalone
            )
        }
    )
}

object ApplicationLogsScreen {
    data class Actions(
        val onBackClick: () -> Unit,
        val onViewItemClick: (ApplicationLogsViewItemMode) -> Unit,
        val onFeatureFlagsNavigation: () -> Unit,
        /**
         * Attaches the aggregated logs zip to a new draft in process. Null where no user is logged in (the
         * standalone entry point reachable from the login screen), as there is no composer to navigate to.
         */
        val onComposeWithLogs: ((Uri) -> Unit)?
    )
}
