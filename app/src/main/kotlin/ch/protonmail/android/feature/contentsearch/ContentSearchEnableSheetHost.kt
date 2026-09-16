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

package ch.protonmail.android.feature.contentsearch

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.component.ProtonModalBottomSheetLayout
import ch.protonmail.android.mailcontentsearch.presentation.bottomsheet.ContentSearchBottomSheet
import ch.protonmail.android.mailcontentsearch.presentation.bottomsheet.ContentSearchBottomSheetState
import kotlinx.coroutines.launch

/**
 * Shows the content search sheet over [content] once [showRequested] turns true.
 *
 * This is the sheet the search screen puts up after its set-up page asks to turn content search on.
 * It is driven by that request alone, not by Home's interstitials, which only run on the mailbox.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContentSearchEnableSheetHost(
    showRequested: Boolean,
    onDismissed: () -> Unit,
    content: @Composable () -> Unit
) {
    val viewModel: HomeContentSearchBottomSheetViewModel = hiltViewModel()
    val sheetState by viewModel.state.collectAsStateWithLifecycle()
    val isMobileDataEnabled by viewModel.isMobileDataEnabled.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var preventDismissal by remember { mutableStateOf(false) }
    val modalSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !preventDismissal }
    )
    var showSheet by remember { mutableStateOf(false) }

    LaunchedEffect(showRequested) {
        if (showRequested) viewModel.submit(HomeContentSearchBottomSheetViewAction.RequestToEnable)
    }

    LaunchedEffect(sheetState) {
        if (sheetState is ContentSearchBottomSheetState.Show) {
            scope.launch { modalSheetState.show() }.invokeOnCompletion { showSheet = true }
        }
    }

    ProtonModalBottomSheetLayout(
        showBottomSheet = showSheet,
        sheetState = modalSheetState,
        sheetContent = {
            ContentSearchBottomSheet(
                isMobileDataEnabled = isMobileDataEnabled,
                actions = ContentSearchBottomSheet.Actions(
                    onDismiss = {
                        viewModel.submit(HomeContentSearchBottomSheetViewAction.EnableForPrimaryUser)
                        preventDismissal = false
                        scope.launch { modalSheetState.hide() }.invokeOnCompletion {
                            showSheet = false
                            viewModel.submit(HomeContentSearchBottomSheetViewAction.Dismiss)
                            onDismissed()
                        }
                    },
                    onToggleMobileData = { enabled ->
                        viewModel.submit(HomeContentSearchBottomSheetViewAction.ToggleMobileData(enabled))
                    }
                )
            ).also { preventDismissal = true }
        },
        dismissOnBack = false,
        onDismissed = {},
        content = content
    )
}
