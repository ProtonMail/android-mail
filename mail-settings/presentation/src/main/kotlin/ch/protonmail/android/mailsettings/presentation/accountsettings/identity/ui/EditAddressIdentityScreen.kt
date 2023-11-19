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

package ch.protonmail.android.mailsettings.presentation.accountsettings.identity.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.EditAddressIdentityState
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.EditAddressIdentityViewAction
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.previewdata.EditAddressIdentityScreenPreviewData
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.viewmodel.EditAddressIdentityViewModel
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonSnackbarHost
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun EditAddressIdentityScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onCloseScreen: () -> Unit = {},
    viewModel: EditAddressIdentityViewModel = hiltViewModel()
) {
    val state: EditAddressIdentityState by viewModel.state.collectAsState()

    val listActions = EditAddressIdentityScreenList.Actions(
        onDisplayNameChanged = { viewModel.submit(EditAddressIdentityViewAction.DisplayName.UpdateValue(it)) },
        onSignatureValueChanged = { viewModel.submit(EditAddressIdentityViewAction.Signature.UpdateValue(it)) },
        onSignatureToggled = { viewModel.submit(EditAddressIdentityViewAction.Signature.ToggleState(it)) },
        onMobileFooterValueChanged = { viewModel.submit(EditAddressIdentityViewAction.MobileFooter.UpdateValue(it)) },
        onMobileFooterToggled = { viewModel.submit(EditAddressIdentityViewAction.MobileFooter.ToggleState(it)) },
    )

    EditAddressIdentityScreen(
        modifier = modifier,
        state = state,
        onBackClick = onBackClick,
        onSaveClick = { viewModel.submit(EditAddressIdentityViewAction.Save) },
        onCloseScreen = onCloseScreen,
        listActions = listActions
    )
}

@Composable
@Suppress("UseComposableActions")
fun EditAddressIdentityScreen(
    modifier: Modifier = Modifier,
    state: EditAddressIdentityState,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCloseScreen: () -> Unit,
    listActions: EditAddressIdentityScreenList.Actions
) {
    val snackbarHostState = ProtonSnackbarHostState()
    val updateErrorMessage = stringResource(id = R.string.mail_settings_identity_error_updating)

    Scaffold(
        modifier = modifier,
        topBar = { EditAddressIdentityTopBar(onBackClick = onBackClick, onSaveClick = onSaveClick) },
        snackbarHost = { ProtonSnackbarHost(hostState = snackbarHostState) },
        content = { paddingValues ->
            when (state) {
                EditAddressIdentityState.Loading -> ProtonCenteredProgress()
                EditAddressIdentityState.LoadingError -> EditAddressIdentityErrorScreen()
                is EditAddressIdentityState.DataLoaded -> {
                    EditAddressIdentityScreenList(
                        modifier = Modifier.padding(paddingValues),
                        displayNameState = state.displayNameState,
                        signatureState = state.signatureState,
                        mobileFooterState = state.mobileFooterState,
                        actions = listActions
                    )
                    ConsumableLaunchedEffect(state.updateError) {
                        snackbarHostState.showSnackbar(ProtonSnackbarType.ERROR, message = updateErrorMessage)
                    }
                    ConsumableLaunchedEffect(state.close) {
                        onCloseScreen()
                    }
                }
            }
        }
    )
}

@Preview
@Composable
private fun EditAddressIdentityScreenPreview() {
    ProtonTheme {
        EditAddressIdentityScreen(
            state = EditAddressIdentityScreenPreviewData.state,
            onBackClick = {},
            onSaveClick = {},
            onCloseScreen = {},
            listActions = EditAddressIdentityScreenPreviewData.listActions
        )
    }
}
