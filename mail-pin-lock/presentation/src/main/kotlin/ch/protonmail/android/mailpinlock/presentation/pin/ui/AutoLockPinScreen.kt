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

package ch.protonmail.android.mailpinlock.presentation.pin.ui

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.component.ProtonCenteredProgress
import ch.protonmail.android.design.compose.component.ProtonTextButton
import ch.protonmail.android.design.compose.component.appbar.ProtonTopAppBar
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyMediumNorm
import ch.protonmail.android.design.compose.theme.titleLargeNorm
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailpinlock.presentation.R
import ch.protonmail.android.mailpinlock.presentation.autolock.standalone.LocalLockScreenEntryPointIsStandalone
import ch.protonmail.android.mailpinlock.presentation.pin.AutoLockPinState
import ch.protonmail.android.mailpinlock.presentation.pin.AutoLockPinViewAction
import ch.protonmail.android.mailpinlock.presentation.pin.AutoLockPinViewModel
import ch.protonmail.android.mailpinlock.presentation.pin.ConfirmButtonUiModel
import ch.protonmail.android.mailpinlock.presentation.pin.DescriptionUiModel
import ch.protonmail.android.mailpinlock.presentation.pin.preview.AutoLockPinScreenPreviewData
import timber.log.Timber

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoLockPinScreen(
    onClose: () -> Unit,
    onShowSuccessSnackbar: (String) -> Unit,
    viewModel: AutoLockPinViewModel = hiltViewModel()
) {
    val state: AutoLockPinState by viewModel.state.collectAsStateWithLifecycle()

    val pinTextFieldState = viewModel.pinTextFieldState

    val actions = AutoLockPinScreen.Actions(
        onBack = { viewModel.submit(AutoLockPinViewAction.PerformBack) },
        onClose = onClose,
        onNext = { viewModel.submit(AutoLockPinViewAction.PerformConfirm) },
        onShowSuccessSnackbar = onShowSuccessSnackbar
    )

    val signOutActions = AutoLockPinScreen.SignOutActions(
        onSignOut = { viewModel.submit(AutoLockPinViewAction.RequestSignOut) },
        onSignOutConfirmed = { viewModel.submit(AutoLockPinViewAction.ConfirmSignOut) },
        onSignOutCanceled = { viewModel.submit(AutoLockPinViewAction.CancelSignOut) }
    )

    AutoLockPinScreen(
        state = state,
        textFieldState = pinTextFieldState,
        actions = actions,
        signOutActions = signOutActions
    )
}

@Composable
private fun AutoLockPinScreen(
    state: AutoLockPinState,
    textFieldState: TextFieldState,
    actions: AutoLockPinScreen.Actions,
    signOutActions: AutoLockPinScreen.SignOutActions
) {
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenHeightDp < 480 || configuration.fontScale > 1.3f

    val isStandalone = LocalLockScreenEntryPointIsStandalone.current

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ProtonTheme.colors.backgroundNorm,
            topBar = {
                if (!isStandalone && state as? AutoLockPinState.DataLoaded != null) {
                    PinScreenTopBar(
                        state = state.topBarState,
                        isCompact = isCompact,
                        onBackClick = actions.onBack
                    )
                }
            }
        ) { paddingValues ->
            when (state) {
                AutoLockPinState.Loading -> ProtonCenteredProgress()
                is AutoLockPinState.DataLoaded -> {
                    Box(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        PinScreenContent(
                            state = state,
                            isCompact = isCompact,
                            pinTextFieldState = textFieldState,
                            actions = actions,
                            signOutActions = signOutActions
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinScreenTopBar(
    state: AutoLockPinState.TopBarState,
    isCompact: Boolean,
    onBackClick: () -> Unit
) {
    val uiModel = state.topBarStateUiModel

    ProtonTopAppBar(
        title = { if (isCompact) Text(text = stringResource(id = uiModel.textRes)) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.presentation_back)
                )
            }
        }
    )
}

@Composable
private fun PinScreenContent(
    state: AutoLockPinState.DataLoaded,
    modifier: Modifier = Modifier,
    isCompact: Boolean,
    pinTextFieldState: TextFieldState,
    actions: AutoLockPinScreen.Actions,
    signOutActions: AutoLockPinScreen.SignOutActions
) {
    val imeHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(state.pinInsertionState) {
        Timber.tag("Moved to state").d("${state.pinInsertionState}")
    }

    BackHandler(true) {
        actions.onBack()
    }

    ConsumableLaunchedEffect(state.closeScreenEffect) {
        actions.onClose()
    }

    ConsumableTextEffect(state.snackbarSuccessEffect) {
        actions.onShowSuccessSnackbar(it)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = ProtonDimens.Spacing.Large)
    ) {
        @Suppress("MagicNumber")
        Spacer(Modifier.weight(.3f))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AutoLockPinScreenHeader(
                descriptionUiModel = state.pinInsertionState.descriptionUiModel,
                isCompact = isCompact
            )

            PinInputSection(
                modifier = Modifier.focusRequester(focusRequester),
                pinTextFieldState = pinTextFieldState,
                maxLength = MAX_PIN_LENGTH,
                error = state.pinInsertionState.error,
                onNext = actions.onNext,
                triggerError = state.pinInsertionState.triggerError
            )

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))

            if (imeHeight > 0.dp) {
                @Suppress("MagicNumber")
                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Jumbo * 3))
            }
        }

        Column(
            modifier = Modifier.padding(bottom = imeHeight)
        ) {
            if (isCompact && imeHeight == 0.dp) {
                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))
            }

            PinScreenButton(
                uiModel = state.confirmButtonState.confirmButtonUiModel,
                isEnabled = pinTextFieldState.text.isNotEmpty(),
                onClick = actions.onNext
            )

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Standard))

            AutoLockPinSignOutItem(state = state.signOutButtonState, actions = signOutActions)

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Jumbo))
        }
    }
}


@Composable
private fun PinScreenButton(
    uiModel: ConfirmButtonUiModel,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    ProtonTextButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ProtonDimens.Spacing.Standard)
            .background(
                color = ProtonTheme.colors.brandNorm,
                shape = ProtonTheme.shapes.massive
            ),
        enabled = isEnabled,
        onClick = onClick
    ) {
        Text(
            text = stringResource(uiModel.textRes),
            style = ProtonTheme.typography.titleMedium,
            color = ProtonTheme.colors.textInverted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AutoLockPinScreenHeader(descriptionUiModel: DescriptionUiModel, isCompact: Boolean) {

    Column(
        modifier = Modifier.padding(ProtonDimens.Spacing.Standard),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isCompact) {
            AutoLockIcon()

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Standard))

            Text(
                text = stringResource(descriptionUiModel.titleRes),
                style = ProtonTheme.typography.titleLargeNorm,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Standard))
        }

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(descriptionUiModel.descriptionRes),
            style = ProtonTheme.typography.bodyMediumNorm,
            color = ProtonTheme.colors.textWeak,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Standard))
    }
}

private const val MAX_PIN_LENGTH = 21

object AutoLockPinScreen {
    data class Actions(
        val onBack: () -> Unit,
        val onClose: () -> Unit,
        val onNext: () -> Unit,
        val onShowSuccessSnackbar: (snackbarText: String) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBack = {},
                onClose = {},
                onNext = {},
                onShowSuccessSnackbar = { _ -> }
            )
        }
    }

    data class SignOutActions(
        val onSignOut: () -> Unit,
        val onSignOutConfirmed: () -> Unit,
        val onSignOutCanceled: () -> Unit
    ) {

        companion object {

            val Empty = SignOutActions(
                onSignOut = {},
                onSignOutConfirmed = {},
                onSignOutCanceled = {}
            )
        }
    }

    const val AutoLockPinModeKey = "auto_lock_pin_open_mode"
}

@Composable
@Preview(name = "Light mode", showBackground = true)
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun AutoLockPinScreenPreview() {
    AutoLockPinScreen(
        state = AutoLockPinScreenPreviewData.DataLoaded,
        textFieldState = TextFieldState(),
        actions = AutoLockPinScreen.Actions.Empty,
        signOutActions = AutoLockPinScreen.SignOutActions.Empty
    )
}

@Composable
@Preview(name = "Light mode", showBackground = true)
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun AutoLockPinScreenCompatPreview() {
    val customConfiguration = Configuration().apply {
        screenHeightDp = 400
        fontScale = 1.0f
    }
    CompositionLocalProvider(LocalConfiguration provides customConfiguration) {
        AutoLockPinScreen(
            state = AutoLockPinScreenPreviewData.DataLoaded,
            textFieldState = TextFieldState(),
            actions = AutoLockPinScreen.Actions.Empty,
            signOutActions = AutoLockPinScreen.SignOutActions.Empty
        )
    }
}
