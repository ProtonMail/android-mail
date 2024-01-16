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
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.HyperlinkText
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.MessagePasswordOperation
import ch.protonmail.android.mailcomposer.presentation.model.SetMessagePasswordState
import ch.protonmail.android.mailcomposer.presentation.ui.SetMessagePasswordScreen.MAX_PASSWORD_LENGTH
import ch.protonmail.android.mailcomposer.presentation.ui.SetMessagePasswordScreen.MIN_PASSWORD_LENGTH
import ch.protonmail.android.mailcomposer.presentation.viewmodel.SetMessagePasswordViewModel
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.serialization.Serializable
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonOutlinedButton
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.flow.rememberAsState
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
    val state by rememberAsState(flow = viewModel.state, initial = SetMessagePasswordState.Loading)

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
                            contentDescription = stringResource(id = R.string.presentation_back)
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
                        onApplyButtonClick = { messagePassword, messagePasswordHint ->
                            viewModel.submit(
                                MessagePasswordOperation.Action.ApplyPassword(messagePassword, messagePasswordHint)
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
fun SetMessagePasswordContent(
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
        var messagePassword by rememberSaveable { mutableStateOf(state.messagePassword) }
        var repeatedMessagePassword by rememberSaveable { mutableStateOf(state.messagePassword) }
        var messagePasswordHint by rememberSaveable { mutableStateOf(state.messagePasswordHint) }
        var isMessagePasswordError by rememberSaveable { mutableStateOf(false) }
        var isRepeatedMessagePasswordError by rememberSaveable { mutableStateOf(false) }
        var isMessagePasswordFieldActivated by rememberSaveable { mutableStateOf(false) }
        var isRepeatedMessagePasswordFieldActivated by rememberSaveable { mutableStateOf(false) }

        fun isMessagePasswordError() {
            isMessagePasswordError = messagePassword.length !in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH
        }
        fun isRepeatedMessagePasswordError() {
            isRepeatedMessagePasswordError = messagePassword != repeatedMessagePassword
        }
        fun isApplyButtonEnabled() = isMessagePasswordFieldActivated && isRepeatedMessagePasswordFieldActivated &&
            !isMessagePasswordError && !isRepeatedMessagePasswordError

        MessagePasswordInfo()
        MessagePasswordSpacer()
        PasswordInputField(
            titleRes = R.string.set_message_password_label,
            supportingTextRes = if (isMessagePasswordError) {
                R.string.set_message_password_supporting_error_text
            } else {
                R.string.set_message_password_supporting_text
            },
            value = messagePassword,
            showTrailingIcon = true,
            isError = isMessagePasswordError,
            onValueChange = {
                messagePassword = it
                isMessagePasswordError()
                if (isRepeatedMessagePasswordFieldActivated) isRepeatedMessagePasswordError()
            },
            onFocusChanged = { hasFocus ->
                if (hasFocus) isMessagePasswordFieldActivated = true
                if (isMessagePasswordFieldActivated) isMessagePasswordError()
                if (isRepeatedMessagePasswordFieldActivated) isRepeatedMessagePasswordError()
            }
        )
        MessagePasswordSpacer()
        PasswordInputField(
            titleRes = R.string.set_message_password_label_repeat,
            supportingTextRes = if (isRepeatedMessagePasswordError) {
                R.string.set_message_password_supporting_error_text_repeat
            } else {
                R.string.set_message_password_supporting_text_repeat
            },
            value = repeatedMessagePassword,
            showTrailingIcon = true,
            isError = isRepeatedMessagePasswordError,
            onValueChange = {
                repeatedMessagePassword = it
                isRepeatedMessagePasswordError()
                if (isMessagePasswordFieldActivated) isMessagePasswordError()
            },
            onFocusChanged = { hasFocus ->
                if (hasFocus) isRepeatedMessagePasswordFieldActivated = true
                if (isRepeatedMessagePasswordFieldActivated) isRepeatedMessagePasswordError()
                if (isMessagePasswordFieldActivated) isMessagePasswordError()
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
            shouldShowEditingButtons = state.shouldShowEditingButtons,
            isApplyButtonEnabled = isApplyButtonEnabled(),
            onApplyButtonClick = { actions.onApplyButtonClick(messagePassword, messagePasswordHint) },
            onRemoveButtonClick = actions.onRemoveButtonClick
        )
    }
}

@Composable
fun MessagePasswordInfo(modifier: Modifier = Modifier) {
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
fun MessagePasswordButtons(
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
fun MessagePasswordSpacer(modifier: Modifier = Modifier, height: Dp = ProtonDimens.MediumSpacing) {
    Spacer(modifier = modifier.height(height))
}

object SetMessagePasswordScreen {
    const val MIN_PASSWORD_LENGTH = 4
    const val MAX_PASSWORD_LENGTH = 21

    const val InputParamsKey = "InputParams"

    @Serializable
    data class InputParams(
        val messageId: MessageId,
        val senderEmail: SenderEmail
    )
}

object SetMessagePasswordContent {
    data class Actions(
        val onApplyButtonClick: (String, String?) -> Unit,
        val onRemoveButtonClick: () -> Unit,
        val onBackClick: () -> Unit
    )
}
