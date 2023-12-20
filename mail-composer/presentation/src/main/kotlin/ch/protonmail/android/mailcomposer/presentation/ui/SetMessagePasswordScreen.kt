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
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.HyperlinkText
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.ui.SetMessagePasswordScreen.MAX_PASSWORD_LENGTH
import ch.protonmail.android.mailcomposer.presentation.ui.SetMessagePasswordScreen.MIN_PASSWORD_LENGTH
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultInverted
import me.proton.core.compose.theme.defaultSmallUnspecified
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.util.kotlin.EMPTY_STRING

@Composable
fun SetMessagePasswordScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
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
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(ProtonTheme.colors.backgroundNorm)
                .verticalScroll(rememberScrollState(), reverseScrolling = true)
                .padding(ProtonDimens.DefaultSpacing)
        ) {
            var messagePassword by rememberSaveable { mutableStateOf(EMPTY_STRING) }
            var repeatedMessagePassword by rememberSaveable { mutableStateOf(EMPTY_STRING) }
            var messagePasswordHint by rememberSaveable { mutableStateOf(EMPTY_STRING) }
            var isMessagePasswordError by rememberSaveable { mutableStateOf(false) }
            var isRepeatedMessagePasswordError by rememberSaveable { mutableStateOf(false) }

            fun isMessagePasswordError() {
                isMessagePasswordError = messagePassword.length !in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH
            }
            fun isRepeatedMessagePasswordError() {
                isRepeatedMessagePasswordError = messagePassword != repeatedMessagePassword
            }

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
                onValueChange = { messagePassword = it },
                onFocusChanged = { isMessagePasswordError() }
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
                onValueChange = { repeatedMessagePassword = it },
                onFocusChanged = { isRepeatedMessagePasswordError() }
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
            ProtonSolidButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ProtonDimens.DefaultButtonMinHeight),
                onClick = {}
            ) {
                Text(
                    text = stringResource(id = R.string.set_message_password_button_apply),
                    style = ProtonTheme.typography.defaultInverted
                )
            }
        }
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
fun MessagePasswordSpacer(modifier: Modifier = Modifier, height: Dp = ProtonDimens.MediumSpacing) {
    Spacer(modifier = modifier.height(height))
}

object SetMessagePasswordScreen {
    const val MIN_PASSWORD_LENGTH = 4
    const val MAX_PASSWORD_LENGTH = 21
}
