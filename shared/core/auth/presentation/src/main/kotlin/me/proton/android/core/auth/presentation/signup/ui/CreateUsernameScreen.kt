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

@file:Suppress("UseComposableActions")

package me.proton.android.core.auth.presentation.signup.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.proton.android.core.auth.presentation.LocalClipManager
import me.proton.android.core.auth.presentation.LocalClipManager.OnClipChangedDisposableEffect
import me.proton.android.core.auth.presentation.R
import me.proton.android.core.auth.presentation.addaccount.SMALL_SCREEN_HEIGHT
import me.proton.android.core.auth.presentation.challenge.SIGNUP_CHALLENGE_FLOW_NAME
import me.proton.android.core.auth.presentation.challenge.SIGNUP_CHALLENGE_USERNAME_FRAME
import me.proton.android.core.auth.presentation.challenge.TextChange
import me.proton.android.core.auth.presentation.signup.CreateUsernameAction.CreateExternalAccount
import me.proton.android.core.auth.presentation.signup.CreateUsernameAction.CreateInternalAccount
import me.proton.android.core.auth.presentation.signup.CreateUsernameAction.CreateUsernameClosed
import me.proton.android.core.auth.presentation.signup.CreateUsernameAction.Perform
import me.proton.android.core.auth.presentation.signup.CreateUsernameState
import me.proton.android.core.auth.presentation.signup.SignUpState
import me.proton.android.core.auth.presentation.signup.viewmodel.SignUpViewModel
import me.proton.core.account.domain.entity.AccountType
import me.proton.core.challenge.presentation.compose.PayloadController
import me.proton.core.challenge.presentation.compose.payload
import me.proton.core.compose.component.ProtonCloseButton
import me.proton.core.compose.component.ProtonOutlinedTextFieldWithError
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.theme.LocalColors
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTypography
import me.proton.core.compose.theme.defaultSmallWeak
import uniffi.mail_uniffi.SignupScreenId

internal const val USERNAME_FIELD_TAG = "USERNAME_FIELD_TAG"
internal const val EMAIL_FIELD_TAG = "EMAIL_FIELD_TAG"
internal const val PHONE_FIELD_TAG = "PHONE_FIELD_TAG"

@Composable
fun CreateUsernameScreen(
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit = {},
    onErrorMessage: (String?, shouldClose: Boolean) -> Unit = { _, _ -> },
    onSuccess: (String) -> Unit = {},
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler(enabled = true) {
        viewModel.perform(CreateUsernameClosed(back = true))
    }

    val flowError = stringResource(R.string.auth_signup_general_error)
    LaunchedEffect(state) {
        when (val s = state) {
            is CreateUsernameState.Closed -> onBackClicked()
            is SignUpState.SignUpError -> onErrorMessage(s.message, false)
            is SignUpState.SignupFlowFailure -> onErrorMessage(flowError, true)
            else -> Unit
        }
    }

    CreateUsernameScreen(
        modifier = modifier,
        onScreenView = { viewModel.onScreenView(it) },
        onCloseClicked = { viewModel.perform(CreateUsernameClosed(back = true)) },
        onUsernameSubmitted = { viewModel.perform(it) },
        onCreateExternalClicked = { viewModel.perform(it) },
        onCreateInternalClicked = { viewModel.perform(it) },
        onErrorMessage = { onErrorMessage(it, false) },
        onSuccess = { signupState -> onSuccess(signupState) },
        state = state
    )
}

@Composable
fun CreateUsernameScreen(
    modifier: Modifier = Modifier,
    onScreenView: (SignupScreenId) -> Unit,
    onCloseClicked: () -> Unit = {},
    onUsernameSubmitted: (Perform) -> Unit = {},
    onCreateExternalClicked: (CreateExternalAccount) -> Unit = {},
    onCreateInternalClicked: (CreateInternalAccount) -> Unit = {},
    onErrorMessage: (String?) -> Unit = {},
    onSuccess: (String) -> Unit = {},
    state: SignUpState
) {
    LaunchedEffect(state) {
        when (state) {
            is CreateUsernameState.Idle -> {}
            is CreateUsernameState.Error -> onErrorMessage(state.message)
            is CreateUsernameState.Success -> onSuccess(state.route)
            else -> Unit
        }
    }

    UsernameScreenScaffold(
        modifier = modifier,
        onScreenView = onScreenView,
        onCloseClicked = onCloseClicked,
        onUsernameSubmitted = onUsernameSubmitted,
        onCreateExternalClicked = onCreateExternalClicked,
        onCreateInternalClicked = onCreateInternalClicked,
        state = state
    )
}

@Composable
fun UsernameScreenScaffold(
    modifier: Modifier = Modifier,
    onScreenView: (SignupScreenId) -> Unit,
    onCloseClicked: () -> Unit = {},
    onUsernameSubmitted: (Perform) -> Unit = {},
    onCreateExternalClicked: (CreateExternalAccount) -> Unit,
    onCreateInternalClicked: (CreateInternalAccount) -> Unit,
    @DrawableRes protonLogo: Int = R.drawable.ic_logo_proton,
    @StringRes titleText: Int = R.string.auth_signup_title,
    state: SignUpState
) {
    val isLoading = (state as? CreateUsernameState)?.isLoading ?: false
    val accountType = (state as? CreateUsernameState)?.accountType ?: AccountType.Internal // default
    var domains by rememberSaveable { mutableStateOf<List<Domain>>(emptyList()) }

    accountType.toSignupScreenId().LaunchOnScreenView(onScreenView)

    LaunchedEffect(state) {
        if (state is CreateUsernameState.LoadingComplete) {
            domains = state.domains ?: emptyList()
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            ProtonTopAppBar(
                title = {},
                navigationIcon = {
                    ProtonCloseButton(onCloseClicked = onCloseClicked)
                },
                backgroundColor = LocalColors.current.backgroundNorm,
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .padding(top = ProtonDimens.SmallSpacing)
                    .verticalScroll(rememberScrollState())
            ) {
                ScreenHeader(
                    logoResource = protonLogo,
                    titleTextResource = titleText
                )

                when (accountType) {
                    AccountType.Internal -> {
                        val validationError = when (state) {
                            is CreateUsernameState.ValidationError.InternalUsernameEmpty ->
                                stringResource(R.string.auth_signup_validation_username)

                            is CreateUsernameState.ValidationError.Other ->
                                state.message ?: stringResource(R.string.auth_signup_validation_username_input_invalid)

                            else -> null
                        }
                        InternalAccountForm(
                            enabled = !isLoading,
                            isLoading = isLoading,
                            onUsernameSubmitted = onUsernameSubmitted,
                            onCreateExternalClicked = onCreateExternalClicked,
                            validationError = validationError,
                            domains = domains
                        )
                    }

                    AccountType.External -> {
                        val validationError = when (state) {
                            is CreateUsernameState.ValidationError.EmailEmpty ->
                                stringResource(R.string.auth_signup_validation_email)

                            is CreateUsernameState.ValidationError.Other ->
                                state.message ?: stringResource(R.string.auth_signup_validation_email_input_invalid)

                            else -> null
                        }
                        ExternalAccountForm(
                            enabled = !isLoading,
                            onExternalEmailSubmitted = onUsernameSubmitted,
                            onCreateInternalClicked = onCreateInternalClicked,
                            validationError = validationError
                        )
                    }

                    AccountType.Username -> {
                        val validationError = when (state) {
                            is CreateUsernameState.ValidationError.UsernameEmpty ->
                                stringResource(R.string.auth_signup_validation_username)

                            is CreateUsernameState.ValidationError.Other ->
                                state.message ?: stringResource(R.string.auth_signup_validation_username_input_invalid)

                            else -> null
                        }
                        UsernameForm(
                            enabled = !isLoading,
                            isLoading = isLoading,
                            onUsernameSubmitted = onUsernameSubmitted,
                            validationError = validationError
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenHeader(@DrawableRes logoResource: Int, @StringRes titleTextResource: Int) {
    Image(
        modifier = Modifier
            .height(64.dp)
            .fillMaxWidth(),
        painter = painterResource(logoResource),
        contentDescription = null,
        alignment = Alignment.Center
    )

    Text(
        text = stringResource(titleTextResource),
        style = ProtonTypography.Default.headline,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ProtonDimens.MediumSpacing)
    )
}

@Composable
private fun UsernameForm(
    enabled: Boolean,
    isLoading: Boolean,
    onUsernameSubmitted: (Perform) -> Unit,
    validationError: String?,
    @StringRes subtitleText: Int = R.string.auth_signup_subtitle
) {
    val scope = rememberCoroutineScope()
    var username by rememberSaveable { mutableStateOf("") }
    val usernameChanges = remember { MutableStateFlow(TextChange()) }
    val usernameHasFocus = remember { mutableStateOf(false) }
    val usernamePayloadController = remember { PayloadController() }
    val usernameTextCopies = remember { MutableStateFlow("") }

    LocalClipManager.current?.OnClipChangedDisposableEffect {
        if (usernameHasFocus.value) usernameTextCopies.value = it
    }

    fun onSubmit() = scope.launch {
        val usernameFrameDetails = usernamePayloadController.flush()
        onUsernameSubmitted(
            Perform(
                value = username,
                domain = null,
                accountType = AccountType.Username,
                usernameFrameDetails = usernameFrameDetails
            )
        )
    }

    Column {
        FormSubtitle(subtitleText)

        Column(
            modifier = Modifier.padding(DefaultSpacing)
        ) {
            UsernameTextField(
                username = username,
                onUsernameChanged = {
                    usernameChanges.value = usernameChanges.value.roll(it)
                    username = it
                },
                enabled = enabled,
                errorText = validationError,
                onFocusChanged = { usernameHasFocus.value = it.hasFocus },
                usernameChanges = usernameChanges,
                usernameTextCopies = usernameTextCopies,
                usernamePayloadController = usernamePayloadController
            )

            NextButton(
                enabled = enabled,
                isLoading = isLoading,
                onClick = ::onSubmit
            )
        }
    }
}

@Composable
private fun InternalAccountForm(
    enabled: Boolean,
    isLoading: Boolean,
    onUsernameSubmitted: (Perform) -> Unit,
    onCreateExternalClicked: (CreateExternalAccount) -> Unit,
    validationError: String?,
    domains: List<Domain>,
    @StringRes subtitleText: Int = R.string.auth_signup_subtitle,
    showExternalAccountOption: Boolean = false
) {
    val scope = rememberCoroutineScope()
    var username by rememberSaveable { mutableStateOf("") }
    var domain by rememberSaveable { mutableStateOf("") }
    val usernameChanges = remember { MutableStateFlow(TextChange()) }
    val usernameHasFocus = remember { mutableStateOf(false) }
    val usernamePayloadController = remember { PayloadController() }
    val usernameTextCopies = remember { MutableStateFlow("") }

    LocalClipManager.current?.OnClipChangedDisposableEffect {
        if (usernameHasFocus.value) usernameTextCopies.value = it
    }

    fun onSubmit() = scope.launch {
        val usernameFrameDetails = usernamePayloadController.flush()
        onUsernameSubmitted(
            Perform(
                value = username,
                domain = domain,
                accountType = AccountType.Internal,
                usernameFrameDetails = usernameFrameDetails
            )
        )
    }

    Column {
        FormSubtitle(subtitleText)

        Column(
            modifier = Modifier.padding(DefaultSpacing)
        ) {
            UsernameTextField(
                username = username,
                onUsernameChanged = {
                    usernameChanges.value = usernameChanges.value.roll(it)
                    username = it
                },
                enabled = enabled,
                errorText = validationError,
                onFocusChanged = { usernameHasFocus.value = it.hasFocus },
                usernameChanges = usernameChanges,
                usernameTextCopies = usernameTextCopies,
                usernamePayloadController = usernamePayloadController
            )

            DomainDropDown(
                isLoading = isLoading,
                data = domains,
                onInputChanged = { domain = it ?: "" }
            )

            NextButton(
                enabled = enabled,
                isLoading = isLoading,
                onClick = ::onSubmit
            )

            if (showExternalAccountOption) {
                FormDivider()

                AlternateAccountOption(
                    onClick = { onCreateExternalClicked(CreateExternalAccount) },
                    textRes = R.string.auth_signup_use_current_email
                )

                FormFootnote(R.string.auth_signup_internal_footnote)
            }
        }
    }
}

@Composable
private fun ExternalAccountForm(
    enabled: Boolean,
    onExternalEmailSubmitted: (Perform) -> Unit,
    onCreateInternalClicked: (CreateInternalAccount) -> Unit,
    validationError: String?
) {
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    val emailChanges = remember { MutableStateFlow(TextChange()) }
    val emailHasFocus = remember { mutableStateOf(false) }
    val emailPayloadController = remember { PayloadController() }
    val emailTextCopies = remember { MutableStateFlow("") }

    LocalClipManager.current?.OnClipChangedDisposableEffect {
        if (emailHasFocus.value) emailTextCopies.value = it
    }

    fun onSubmit() = scope.launch {
        val emailFrameDetails = emailPayloadController.flush()
        onExternalEmailSubmitted(
            Perform(
                value = email,
                domain = null,
                accountType = AccountType.External,
                usernameFrameDetails = emailFrameDetails
            )
        )
    }

    Column(
        modifier = Modifier.padding(DefaultSpacing)
    ) {
        EmailTextField(
            email = email,
            onEmailChanged = {
                emailChanges.value = emailChanges.value.roll(it)
                email = it
            },
            enabled = enabled,
            errorText = validationError,
            onFocusChanged = { emailHasFocus.value = it.hasFocus },
            emailChanges = emailChanges,
            emailTextCopies = emailTextCopies,
            emailPayloadController = emailPayloadController
        )

        NextButton(
            enabled = enabled,
            isLoading = !enabled,
            onClick = ::onSubmit
        )

        FormDivider()

        AlternateAccountOption(
            onClick = { onCreateInternalClicked(CreateInternalAccount) },
            textRes = R.string.auth_signup_get_encrypted_email
        )

        FormFootnote(R.string.auth_signup_external_footnote)
    }
}

@Composable
private fun UsernameTextField(
    username: String,
    onUsernameChanged: (String) -> Unit,
    enabled: Boolean,
    errorText: String?,
    onFocusChanged: (FocusState) -> Unit,
    usernameChanges: MutableStateFlow<TextChange>,
    usernameTextCopies: MutableStateFlow<String>,
    usernamePayloadController: PayloadController
) {
    ProtonOutlinedTextFieldWithError(
        text = username,
        onValueChanged = onUsernameChanged,
        enabled = enabled,
        errorText = errorText,
        label = { Text(text = stringResource(id = R.string.auth_signup_username)) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            autoCorrectEnabled = false
        ),
        singleLine = true,
        modifier = Modifier
            .onFocusChanged(onFocusChanged)
            .fillMaxWidth()
            .padding(top = DefaultSpacing)
            .payload(
                flow = SIGNUP_CHALLENGE_FLOW_NAME,
                frame = SIGNUP_CHALLENGE_USERNAME_FRAME,
                onTextChanged = usernameChanges.map { it.toPair() },
                onTextCopied = usernameTextCopies,
                onFrameUpdated = {},
                payloadController = usernamePayloadController
            )
            .semantics { contentType = ContentType.NewUsername }
            .testTag(USERNAME_FIELD_TAG)
    )
}

@Composable
private fun EmailTextField(
    email: String,
    onEmailChanged: (String) -> Unit,
    enabled: Boolean,
    errorText: String?,
    onFocusChanged: (FocusState) -> Unit,
    emailChanges: MutableStateFlow<TextChange>,
    emailTextCopies: MutableStateFlow<String>,
    emailPayloadController: PayloadController
) {
    ProtonOutlinedTextFieldWithError(
        text = email,
        onValueChanged = onEmailChanged,
        enabled = enabled,
        errorText = errorText,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            autoCorrectEnabled = false
        ),
        label = { Text(text = stringResource(id = R.string.auth_email)) },
        singleLine = true,
        modifier = Modifier
            .onFocusChanged(onFocusChanged)
            .fillMaxWidth()
            .padding(top = DefaultSpacing)
            .payload(
                flow = SIGNUP_CHALLENGE_FLOW_NAME,
                frame = SIGNUP_CHALLENGE_USERNAME_FRAME,
                onTextChanged = emailChanges.map { it.toPair() },
                onTextCopied = emailTextCopies,
                onFrameUpdated = {},
                payloadController = emailPayloadController
            )
            .semantics { contentType = ContentType.EmailAddress }
            .testTag(EMAIL_FIELD_TAG)
    )
}

@Composable
private fun FormSubtitle(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = ProtonTypography.Default.defaultSmallWeak,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ProtonDimens.SmallSpacing)
    )
}

@Composable
private fun NextButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    ProtonSolidButton(
        contained = false,
        enabled = enabled,
        loading = isLoading,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ProtonDimens.MediumSpacing)
            .height(ProtonDimens.DefaultButtonMinHeight)
    ) {
        Text(text = stringResource(R.string.auth_signup_next))
    }
}

@Composable
private fun FormDivider() {
    Divider(
        modifier = Modifier.padding(top = ProtonDimens.MediumSpacing),
        color = LocalColors.current.separatorNorm
    )
}

@Composable
private fun AlternateAccountOption(onClick: () -> Unit, @StringRes textRes: Int) {
    ProtonTextButton(
        contained = false,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ProtonDimens.MediumSpacing)
            .height(ProtonDimens.DefaultButtonMinHeight)
    ) {
        Text(text = stringResource(textRes))
    }
}

@Composable
private fun FormFootnote(@StringRes textRes: Int) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        text = stringResource(id = textRes),
        style = ProtonTypography.Default.defaultSmallWeak
    )
}

private fun AccountType.toSignupScreenId() = when (this) {
    AccountType.Username -> SignupScreenId.CHOOSE_USERNAME
    AccountType.Internal -> SignupScreenId.CHOOSE_INTERNAL_EMAIL
    AccountType.External -> SignupScreenId.CHOOSE_EXTERNAL_EMAIL
}

@Preview(name = "Light mode", showBackground = true)
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Small screen height", heightDp = SMALL_SCREEN_HEIGHT)
@Preview(name = "Foldable", device = Devices.PIXEL_FOLD)
@Preview(name = "Tablet", device = Devices.PIXEL_C)
@Preview(name = "Horizontal", widthDp = 800, heightDp = 360)
@Composable
internal fun CreateUsernameScreenPreview() {
    ProtonTheme {
        CreateUsernameScreen(
            onScreenView = {},
            state = CreateUsernameState.Idle(AccountType.Internal)
        )
    }
}

@Preview(name = "Light mode", showBackground = true)
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Small screen height", heightDp = SMALL_SCREEN_HEIGHT)
@Preview(name = "Foldable", device = Devices.PIXEL_FOLD)
@Preview(name = "Tablet", device = Devices.PIXEL_C)
@Preview(name = "Horizontal", widthDp = 800, heightDp = 360)
@Composable
internal fun CreateInternalPreview() {
    ProtonTheme {
        InternalAccountForm(
            enabled = true,
            isLoading = false,
            onUsernameSubmitted = {},
            onCreateExternalClicked = {},
            validationError = null,
            domains = listOf("protonmail.com", "protonmail.ch")
        )
    }
}

@Preview(name = "Light mode", showBackground = true)
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Small screen height", heightDp = SMALL_SCREEN_HEIGHT)
@Preview(name = "Foldable", device = Devices.PIXEL_FOLD)
@Preview(name = "Tablet", device = Devices.PIXEL_C)
@Preview(name = "Horizontal", widthDp = 800, heightDp = 360)
@Composable
internal fun CreateExternalPreview() {
    ProtonTheme {
        ExternalAccountForm(
            enabled = true,
            onExternalEmailSubmitted = {},
            onCreateInternalClicked = {},
            validationError = "Email is empty"
        )
    }
}

@Preview(name = "Light mode", showBackground = true)
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Small screen height", heightDp = SMALL_SCREEN_HEIGHT)
@Preview(name = "Foldable", device = Devices.PIXEL_FOLD)
@Preview(name = "Tablet", device = Devices.PIXEL_C)
@Preview(name = "Horizontal", widthDp = 800, heightDp = 360)
@Composable
internal fun CreateUsernamePreview() {
    ProtonTheme {
        UsernameForm(
            enabled = true,
            isLoading = false,
            validationError = null,
            onUsernameSubmitted = {}
        )
    }
}
