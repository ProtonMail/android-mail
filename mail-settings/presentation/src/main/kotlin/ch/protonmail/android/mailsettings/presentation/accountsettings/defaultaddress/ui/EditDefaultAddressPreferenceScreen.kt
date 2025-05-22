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

package ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model.EditDefaultAddressState
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.previewdata.EditDefaultAddressPreviewData
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.viewmodel.EditDefaultAddressViewModel
import ch.protonmail.android.uicomponents.snackbar.DismissableSnackbarHost
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonSettingsTopBar
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun EditDefaultAddressScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    viewModel: EditDefaultAddressViewModel = hiltViewModel()
) {

    val state by viewModel.state.collectAsState()

    EditDefaultAddressScreen(
        modifier = modifier,
        state = state,
        onBackClick = onBackClick,
        onAddressSelected = { viewModel.setPrimaryAddress(it) }
    )
}

@Composable
fun EditDefaultAddressScreen(
    modifier: Modifier = Modifier,
    state: EditDefaultAddressState,
    onBackClick: () -> Unit = {},
    onAddressSelected: (String) -> Unit
) {
    val snackbarHostState = ProtonSnackbarHostState()
    val updateErrorMessage = stringResource(id = R.string.mail_settings_default_email_address_error_update)
    val subscriptionErrorMessage =
        stringResource(id = R.string.mail_settings_default_email_address_error_update_subscription)

    val activeListActions = ActiveAddressesList.Actions(
        onAddressSelected = onAddressSelected,
        showGenericUpdateError = {
            snackbarHostState.showSnackbar(type = ProtonSnackbarType.ERROR, message = updateErrorMessage)
        },
        showSubscriptionUpdateError = {
            snackbarHostState.showSnackbar(type = ProtonSnackbarType.NORM, message = subscriptionErrorMessage)
        }
    )

    if ((state as? EditDefaultAddressState.WithData)?.showOverlayLoader == true) {
        OverlayLoadingIndicator()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ProtonSettingsTopBar(
                title = stringResource(id = R.string.mail_settings_default_email_address_title),
                onBackClick = onBackClick
            )
        },
        snackbarHost = { DismissableSnackbarHost(protonSnackbarHostState = snackbarHostState) },
        content = { paddingValues ->
            when (state) {
                EditDefaultAddressState.Loading -> ProtonCenteredProgress()
                EditDefaultAddressState.LoadingError -> EditDefaultAddressListError()
                is EditDefaultAddressState.WithData -> {
                    EditDefaultAddressList(
                        modifier = modifier.padding(paddingValues),
                        state = state,
                        actions = activeListActions
                    )
                }
            }
        }
    )
}

@Composable
private fun OverlayLoadingIndicator(preventBackNavigation: Boolean = true) {
    BackHandler(enabled = preventBackNavigation) { }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f))
            .zIndex(1f)
            .pointerInput(Unit) {
                detectTapGestures { }
                detectDragGestures { _, _ -> }
            }
    ) {
        ProtonCenteredProgress()
    }
}

@Preview
@Composable
private fun DefaultAddressPreview() {
    ProtonTheme {
        EditDefaultAddressScreen(
            state = EditDefaultAddressState.WithData(
                activeAddressesState = EditDefaultAddressPreviewData.ActiveAddressesState,
                inactiveAddressesState = EditDefaultAddressPreviewData.InactiveAddressesEmptyState,
                updateErrorState = EditDefaultAddressPreviewData.NoErrorState,
                showOverlayLoader = false
            ),
            onBackClick = {},
            onAddressSelected = {}
        )
    }
}

@Preview
@Composable
private fun DefaultAddressPreviewInactiveAddresses() {
    ProtonTheme {
        EditDefaultAddressScreen(
            state = EditDefaultAddressState.WithData(
                activeAddressesState = EditDefaultAddressPreviewData.ActiveAddressesState,
                inactiveAddressesState = EditDefaultAddressPreviewData.InactiveAddressesState,
                updateErrorState = EditDefaultAddressPreviewData.NoErrorState,
                showOverlayLoader = false
            ),
            onBackClick = {},
            onAddressSelected = {}
        )
    }
}
