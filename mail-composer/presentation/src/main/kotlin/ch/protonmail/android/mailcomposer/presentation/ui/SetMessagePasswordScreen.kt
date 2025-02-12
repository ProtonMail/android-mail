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

package ch.protonmail.android.mailcomposer.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.HyperlinkText
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.MessagePasswordOperation
import ch.protonmail.android.mailcomposer.presentation.model.SetMessagePasswordState
import ch.protonmail.android.mailcomposer.presentation.viewmodel.SetMessagePasswordViewModel
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.serialization.Serializable
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonOutlinedButton
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultInverted
import me.proton.core.compose.theme.defaultSmallUnspecified
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.compose.theme.defaultUnspecified

@Composable
fun SetMessagePasswordScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SetMessagePasswordViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            ProtonTopAppBar(
                modifier = Modifier.fillMaxWidth(),
                title = {
                    Text(
                        text = stringResource(id = R.string.set_message_password_title),
                        style = ProtonTheme.typography.defaultStrongNorm
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = stringResource(id = R.string.presentation_back),
                            tint = ProtonTheme.colors.iconNorm
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (state) {
            is SetMessagePasswordState.Loading -> ProtonCenteredProgress()
            is SetMessagePasswordState.Data -> {
                SetMessagePasswordContent(
                    modifier = Modifier.padding(paddingValues),
                    state = state as SetMessagePasswordState.Data,
                    actions = SetMessagePasswordContent.Actions(
                        validatePassword = { password ->
                            viewModel.submit(MessagePasswordOperation.Action.ValidatePassword(password))
                        },
                        validateRepeatedPassword = { password, repeatedPassword ->
                            viewModel.submit(
                                MessagePasswordOperation.Action.ValidateRepeatedPassword(password, repeatedPassword)
                            )
                        },
                        onApplyButtonClick = { messagePassword, messagePasswordHint ->
                            viewModel.submit(
                                if ((state as SetMessagePasswordState.Data).isInEditMode) {
                                    MessagePasswordOperation.Action.UpdatePassword(messagePassword, messagePasswordHint)
                                } else {
                                    MessagePasswordOperation.Action.ApplyPassword(messagePassword, messagePasswordHint)
                                }
                            )
                        },
                        onRemoveButtonClick = { viewModel.submit(MessagePasswordOperation.Action.RemovePassword) },
                        onBackClick = onBackClick
                    )
                )
            }
        }
    }
}

@Composable
@Suppress("ComplexMethod")
private fun SetMessagePasswordContent(
    state: SetMessagePasswordState.Data,
    actions: SetMessagePasswordContent.Actions,
    modifier: Modifier = Modifier
) {
    ConsumableLaunchedEffect(effect = state.exitScreen) {
        actions.onBackClick()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ProtonTheme.colors.backgroundNorm)
            .verticalScroll(rememberScrollState(), reverseScrolling = true)
            .padding(ProtonDimens.DefaultSpacing)
    ) {
        var messagePassword by rememberSaveable { mutableStateOf(state.initialMessagePasswordValue) }
        var repeatedMessagePassword by rememberSaveable { mutableStateOf(state.initialMessagePasswordValue) }
        var messagePasswordHint by rememberSaveable { mutableStateOf(state.initialMessagePasswordHintValue) }
        var isMessagePasswordFieldActivated by rememberSaveable { mutableStateOf(state.isInEditMode) }
        var isRepeatedMessagePasswordFieldActivated by rememberSaveable { mutableStateOf(state.isInEditMode) }

        fun validateMessagePassword() {
            actions.validatePassword(messagePassword)
        }
        fun validateRepeatedMessagePassword() {
            actions.validateRepeatedPassword(messagePassword, repeatedMessagePassword)
        }
        fun shouldApplyButtonBeEnabled() = isMessagePasswordFieldActivated && isRepeatedMessagePasswordFieldActivated &&
            !state.hasMessagePasswordError && !state.hasRepeatedMessagePasswordError

        MessagePasswordInfo()
        MessagePasswordSpacer()
        PasswordInputField(
            titleRes = R.string.set_message_password_label,
            supportingTextRes = if (state.hasMessagePasswordError) {
                R.string.set_message_password_supporting_error_text
            } else {
                R.string.set_message_password_supporting_text
            },
            value = messagePassword,
            showTrailingIcon = true,
            isError = state.hasMessagePasswordError,
            onValueChange = {
                messagePassword = it
                validateMessagePassword()
                if (isRepeatedMessagePasswordFieldActivated) validateRepeatedMessagePassword()
            },
            onFocusChanged = { hasFocus ->
                if (hasFocus) isMessagePasswordFieldActivated = true
                if (isMessagePasswordFieldActivated) validateMessagePassword()
                if (isRepeatedMessagePasswordFieldActivated) validateRepeatedMessagePassword()
            }
        )
        MessagePasswordSpacer()
        PasswordInputField(
            titleRes = R.string.set_message_password_label_repeat,
            supportingTextRes = if (state.hasRepeatedMessagePasswordError) {
                R.string.set_message_password_supporting_error_text_repeat
            } else {
                R.string.set_message_password_supporting_text_repeat
            },
            value = repeatedMessagePassword,
            showTrailingIcon = true,
            isError = state.hasRepeatedMessagePasswordError,
            onValueChange = {
                repeatedMessagePassword = it
                validateRepeatedMessagePassword()
                if (isMessagePasswordFieldActivated) validateMessagePassword()
            },
            onFocusChanged = { hasFocus ->
                if (hasFocus) isRepeatedMessagePasswordFieldActivated = true
                if (isRepeatedMessagePasswordFieldActivated) validateRepeatedMessagePassword()
                if (isMessagePasswordFieldActivated) validateMessagePassword()
            }
        )
        MessagePasswordSpacer()
        PasswordInputField(
            titleRes = R.string.set_message_password_label_hint,
            supportingTextRes = null,
            value = messagePasswordHint,
            showTrailingIcon = false,
            isError = false,
            onValueChange = { messagePasswordHint = it },
            onFocusChanged = {}
        )
        MessagePasswordSpacer(height = ProtonDimens.LargerSpacing)
        MessagePasswordButtons(
            shouldShowEditingButtons = state.isInEditMode,
            isApplyButtonEnabled = shouldApplyButtonBeEnabled(),
            onApplyButtonClick = { actions.onApplyButtonClick(messagePassword, messagePasswordHint) },
            onRemoveButtonClick = actions.onRemoveButtonClick
        )
    }
}

@Composable
private fun MessagePasswordInfo(modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Icon(
            painter = painterResource(id = R.drawable.ic_proton_info_circle),
            contentDescription = NO_CONTENT_DESCRIPTION,
            tint = ProtonTheme.colors.iconWeak
        )
        Spacer(modifier = Modifier.width(ProtonDimens.DefaultSpacing))
        Column {
            Text(
                text = stringResource(id = R.string.set_message_password_info_message),
                style = ProtonTheme.typography.defaultSmallWeak
            )
            HyperlinkText(
                textResource = R.string.set_message_password_info_link,
                textStyle = ProtonTheme.typography.defaultSmallUnspecified,
                linkTextColor = ProtonTheme.colors.interactionNorm
            )
        }
    }
}

@Composable
private fun MessagePasswordButtons(
    shouldShowEditingButtons: Boolean,
    isApplyButtonEnabled: Boolean,
    onApplyButtonClick: () -> Unit,
    onRemoveButtonClick: () -> Unit
) {
    ProtonSolidButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(ProtonDimens.DefaultButtonMinHeight),
        enabled = isApplyButtonEnabled,
        onClick = onApplyButtonClick
    ) {
        Text(
            text = stringResource(
                id = if (shouldShowEditingButtons) {
                    R.string.set_message_password_button_save_changes
                } else R.string.set_message_password_button_apply
            ),
            style = ProtonTheme.typography.defaultInverted
        )
    }
    if (shouldShowEditingButtons) {
        MessagePasswordSpacer(height = ProtonDimens.DefaultSpacing)
        ProtonOutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(ProtonDimens.DefaultButtonMinHeight),
            onClick = onRemoveButtonClick
        ) {
            Text(
                text = stringResource(id = R.string.set_message_password_button_remove_password),
                style = ProtonTheme.typography.defaultUnspecified,
                color = ProtonTheme.colors.interactionNorm
            )
        }
    }
}

@Composable
private fun MessagePasswordSpacer(modifier: Modifier = Modifier, height: Dp = ProtonDimens.MediumSpacing) {
    Spacer(modifier = modifier.height(height))
}

object SetMessagePasswordScreen {

    const val InputParamsKey = "InputParams"

    @Serializable
    data class InputParams(
        val messageId: MessageId,
        val senderEmail: SenderEmail
    )
}

object SetMessagePasswordContent {
    data class Actions(
        val validatePassword: (String) -> Unit,
        val validateRepeatedPassword: (String, String) -> Unit,
        val onApplyButtonClick: (String, String?) -> Unit,
        val onRemoveButtonClick: () -> Unit,
        val onBackClick: () -> Unit
    )
}
