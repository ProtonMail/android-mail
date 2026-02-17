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

package me.proton.android.core.auth.presentation.secondfactor.fido

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.theme.ProtonTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.proton.android.core.auth.presentation.R
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.theme.ProtonDimens

@Suppress("UseComposableActions")
@Composable
fun Fido2InputForm(
    modifier: Modifier = Modifier,
    onError: (String?) -> Unit,
    onSuccess: () -> Unit,
    onClose: () -> Unit = {},
    @DrawableRes fido2Logo: Int = R.drawable.ic_fido2,
    externalAction: StateFlow<Fido2InputAction?> = MutableStateFlow(null),
    onEmitAction: (Fido2InputAction) -> Unit = {},
    viewModel: Fido2InputViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val externalActionValue by externalAction.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(externalActionValue) {
        externalActionValue?.let { action ->
            when (action) {
                is Fido2InputAction.SecurityKeyResult -> viewModel.perform(action)
                else -> Unit
            }
        }
    }

    LaunchedEffect(state) {
        when (val currentState = state) {
            is Fido2InputState.Error.General -> onError(currentState.error)
            is Fido2InputState.Error.StoredKeysConfig ->
                onError(context.getString(R.string.auth_fido_config_error))

            is Fido2InputState.Error.ReadingSecurityKey ->
                onError(context.getString(R.string.auth_fido_reading_key_error))

            is Fido2InputState.Error.SubmitFido ->
                onError(context.getString(R.string.auth_fido_submit_key_error))

            is Fido2InputState.Closed -> onClose()

            is Fido2InputState.Awaiting2Pass,
            is Fido2InputState.LoggedIn -> onSuccess()

            is Fido2InputState.Authenticating,
            is Fido2InputState.InitiatedReadingSecurityKey,
            is Fido2InputState.Idle -> Unit

            is Fido2InputState.ReadingSecurityKey -> {
                onEmitAction(Fido2InputAction.ReadSecurityKey(currentState.options))
            }
        }
    }

    Fido2InputContent(
        modifier = modifier,
        fido2Logo = fido2Logo,
        state = state,
        onAuthenticate = { viewModel.perform(Fido2InputAction.Authenticate()) }
    )
}

@Composable
fun Fido2InputContent(
    modifier: Modifier = Modifier,
    @DrawableRes fido2Logo: Int,
    state: Fido2InputState,
    onAuthenticate: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.DefaultSpacing),
        modifier = modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(ProtonDimens.DefaultSpacing))

        Fido2Logo(
            logoRes = fido2Logo,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Fido2InstructionText()

        Fido2AuthenticateButton(
            onClick = onAuthenticate,
            isLoading = state is Fido2InputState.InitiatedReadingSecurityKey || state is Fido2InputState.Authenticating,
            isEnabled = state.isInteractionEnabled()
        )

        Spacer(modifier = Modifier.height(ProtonDimens.DefaultSpacing))
    }
}

@Composable
private fun Fido2Logo(@DrawableRes logoRes: Int, modifier: Modifier = Modifier) {
    val securityKey = stringResource(R.string.auth_fido_security_key)
    Image(
        modifier = modifier.semantics {
            contentDescription = securityKey
        },
        painter = painterResource(logoRes),
        contentDescription = null,
        alignment = Alignment.Center
    )
}

@Composable
private fun Fido2InstructionText() {
    AnnotatedLinkText(
        fullText = stringResource(R.string.auth_second_factor_insert_security_key),
        linkText = stringResource(R.string.auth_second_factor_insert_security_key_link),
        linkUrl = stringResource(R.string.second_factor_security_key_link)
    )
}

@Composable
private fun Fido2AuthenticateButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val authenticatingText = stringResource(R.string.auth_fido_authenticating_in_progress)
    ProtonSolidButton(
        onClick = onClick,
        contained = false,
        enabled = isEnabled,
        loading = isLoading,
        modifier = modifier
            .height(ProtonDimens.DefaultButtonMinHeight)
            .semantics {
                if (isLoading) {
                    stateDescription = authenticatingText
                }
            }
    ) {
        Text(text = stringResource(R.string.auth_second_factor_authenticate))
    }
}

private fun Fido2InputState.isInteractionEnabled(): Boolean {
    return when (this) {
        is Fido2InputState.InitiatedReadingSecurityKey,
        is Fido2InputState.Authenticating -> false

        else -> true
    }
}

@Composable
fun AnnotatedLinkText(
    fullText: String,
    linkText: String,
    linkUrl: String,
    linkColor: Color = ProtonTheme.colors.interactionBrandDefaultNorm
) {
    val annotatedString = remember(fullText, linkText, linkUrl) {
        buildAnnotatedString {
            append(fullText)
            append(" ")
            withLink(
                LinkAnnotation.Url(
                    url = linkUrl,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline
                        )
                    )
                )
            ) {
                append(linkText)
            }
        }
    }

    Text(text = annotatedString)
}

// Enhanced previews with different states
@Preview(showBackground = true, name = "Idle State")
@Composable
fun Fido2InputFormIdlePreview() {
    ProtonTheme {
        Fido2InputContent(
            fido2Logo = R.drawable.ic_fido2,
            state = Fido2InputState.Idle,
            onAuthenticate = {}
        )
    }
}

@Preview(showBackground = true, name = "Loading State")
@Composable
fun Fido2InputFormLoadingPreview() {
    ProtonTheme {
        Fido2InputContent(
            fido2Logo = R.drawable.ic_fido2,
            state = Fido2InputState.InitiatedReadingSecurityKey,
            onAuthenticate = {}
        )
    }
}

@Preview(showBackground = true, name = "Authenticating State")
@Composable
fun Fido2InputFormAuthenticatingPreview() {
    ProtonTheme {
        Fido2InputContent(
            fido2Logo = R.drawable.ic_fido2,
            state = Fido2InputState.Authenticating,
            onAuthenticate = {}
        )
    }
}

@Preview(showBackground = true, name = "Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun Fido2InputFormDarkPreview() {
    ProtonTheme {
        Fido2InputContent(
            fido2Logo = R.drawable.ic_fido2,
            state = Fido2InputState.Idle,
            onAuthenticate = {}
        )
    }
}
