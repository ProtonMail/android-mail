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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.EditAddressIdentityState
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.EditAddressIdentityViewAction
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.previewdata.EditAddressIdentityScreenPreviewData
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.upselling.MobileSignatureUpsellingBottomSheet
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.viewmodel.EditAddressIdentityViewModel
import ch.protonmail.android.mailupselling.presentation.model.BottomSheetVisibilityEffect
import ch.protonmail.android.mailupselling.presentation.ui.bottomsheet.UpsellingBottomSheet.DELAY_SHOWING
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen
import ch.protonmail.android.uicomponents.bottomsheet.bottomSheetHeightConstrainedContent
import ch.protonmail.android.uicomponents.snackbar.DismissableSnackbarHost
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonModalBottomSheetLayout
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
    val state: EditAddressIdentityState by viewModel.state.collectAsStateWithLifecycle()

    val listActions = EditAddressIdentityScreenList.Actions(
        onDisplayNameChanged = { viewModel.submit(EditAddressIdentityViewAction.DisplayName.UpdateValue(it)) },
        onSignatureValueChanged = { viewModel.submit(EditAddressIdentityViewAction.Signature.UpdateValue(it)) },
        onSignatureToggled = { viewModel.submit(EditAddressIdentityViewAction.Signature.ToggleState(it)) },
        onMobileFooterValueChanged = { viewModel.submit(EditAddressIdentityViewAction.MobileFooter.UpdateValue(it)) },
        onMobileFooterToggled = { viewModel.submit(EditAddressIdentityViewAction.MobileFooter.ToggleState(it)) }
    )

    EditAddressIdentityScreen(
        modifier = modifier,
        state = state,
        onBackClick = onBackClick,
        onSaveClick = { viewModel.submit(EditAddressIdentityViewAction.Save) },
        onCloseScreen = onCloseScreen,
        onDismissUpselling = { viewModel.submit(EditAddressIdentityViewAction.HideUpselling) },
        listActions = listActions
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Suppress("UseComposableActions")
fun EditAddressIdentityScreen(
    modifier: Modifier = Modifier,
    state: EditAddressIdentityState,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCloseScreen: () -> Unit,
    onDismissUpselling: () -> Unit,
    listActions: EditAddressIdentityScreenList.Actions
) {
    val snackbarHostState = remember { ProtonSnackbarHostState() }
    val updateErrorMessage = stringResource(id = R.string.mail_settings_identity_error_updating)

    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    var showBottomSheet by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    ProtonModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetContent = bottomSheetHeightConstrainedContent {
            if (showBottomSheet) {
                MobileSignatureUpsellingBottomSheet(
                    actions = UpsellingScreen.Actions.Empty.copy(
                        onDismiss = onDismissUpselling,
                        onUpgrade = { message ->
                            scope.launch {
                                snackbarHostState.showSnackbar(ProtonSnackbarType.NORM, message = message)
                            }
                        },
                        onError = { message ->
                            scope.launch {
                                snackbarHostState.showSnackbar(ProtonSnackbarType.ERROR, message = message)
                            }
                        }
                    )
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier,
            topBar = { EditAddressIdentityTopBar(onBackClick = onBackClick, onSaveClick = onSaveClick) },
            snackbarHost = { DismissableSnackbarHost(protonSnackbarHostState = snackbarHostState) },
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
                        ConsumableLaunchedEffect(effect = state.upsellingVisibility) { bottomSheetEffect ->
                            when (bottomSheetEffect) {
                                BottomSheetVisibilityEffect.Hide -> scope.launch {
                                    bottomSheetState.hide()
                                    showBottomSheet = false
                                }

                                BottomSheetVisibilityEffect.Show -> scope.launch {
                                    showBottomSheet = true
                                    delay(DELAY_SHOWING)
                                    focusManager.clearFocus()
                                    bottomSheetState.show()
                                }
                            }
                        }
                        ConsumableTextEffect(effect = state.upsellingInProgress) { message ->
                            snackbarHostState.snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(ProtonSnackbarType.NORM, message)
                        }
                    }
                }
            }
        )
    }
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
            onDismissUpselling = {},
            listActions = EditAddressIdentityScreenPreviewData.listActions
        )
    }
}
