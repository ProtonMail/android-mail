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

import java.util.Locale
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.proton.android.core.auth.presentation.R
import me.proton.android.core.auth.presentation.addaccount.SMALL_SCREEN_HEIGHT
import me.proton.android.core.auth.presentation.challenge.SIGNUP_CHALLENGE_FLOW_NAME
import me.proton.android.core.auth.presentation.challenge.SIGNUP_CHALLENGE_RECOVERY_FRAME
import me.proton.android.core.auth.presentation.challenge.TextChange
import me.proton.android.core.auth.presentation.signup.CreateRecoveryAction
import me.proton.android.core.auth.presentation.signup.CreateRecoveryAction.CreateRecoveryClosed
import me.proton.android.core.auth.presentation.signup.CreateRecoveryAction.DialogAction.PickCountry
import me.proton.android.core.auth.presentation.signup.CreateRecoveryAction.DialogAction.WantSkipRecovery
import me.proton.android.core.auth.presentation.signup.CreateRecoveryAction.SelectRecoveryMethod
import me.proton.android.core.auth.presentation.signup.CreateRecoveryAction.SubmitRecoveryEmail
import me.proton.android.core.auth.presentation.signup.CreateRecoveryAction.SubmitRecoveryPhone
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.Closed
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.CountryPickerFailed
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.Creating
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.Error
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.Idle
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.OnCountryPicked
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.SkipFailed
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.SkipSuccess
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.Success
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.ValidationError
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.ValidationError.Email
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.ValidationError.Phone
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.WantCountryPicker
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.WantSkip
import me.proton.android.core.auth.presentation.signup.RecoveryMethod
import me.proton.android.core.auth.presentation.signup.SignUpState
import me.proton.android.core.auth.presentation.signup.viewmodel.SignUpViewModel
import me.proton.core.challenge.domain.entity.ChallengeFrameDetails
import me.proton.core.challenge.presentation.compose.PayloadController
import me.proton.core.challenge.presentation.compose.payload
import me.proton.core.compose.component.ProtonOutlinedTextFieldWithError
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.theme.LocalColors
import me.proton.core.compose.theme.LocalTypography
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.LargeSpacing
import me.proton.core.compose.theme.ProtonDimens.MediumSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTypography
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.compose.theme.defaultStrongNorm
import uniffi.mail_uniffi.SignupScreenId

@Composable
fun CreateRecoveryScreen(
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit = {},
    onWantSkip: () -> Unit = {},
    onCountryPickerClicked: (List<Country>) -> Unit = {},
    onErrorMessage: (String?) -> Unit = {},
    onSuccess: (String) -> Unit = { },
    viewModel: SignUpViewModel = hiltViewModel()
) {
    SignupScreenId.SET_RECOVERY_METHOD.LaunchOnScreenView(viewModel::onScreenView)

    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler(enabled = true) {
        viewModel.perform(CreateRecoveryClosed(back = true))
    }

    LaunchedEffect(state) {
        when (state) {
            is Closed -> onBackClicked()
            is WantSkip -> onWantSkip()
            else -> Unit
        }
    }

    CreateRecoveryScreen(
        modifier = modifier,
        onBackClicked = { viewModel.perform(CreateRecoveryClosed(back = true)) },
        onWantSkip = { viewModel.perform(WantSkipRecovery(it)) },
        onCountryPickerClicked = onCountryPickerClicked,
        onTabSelected = {
            viewModel.perform(
                SelectRecoveryMethod(
                    recoveryMethod = it,
                    locale = Locale.getDefault().country
                )
            )
        },
        onRecoverySubmitted = { viewModel.perform(it) },
        onCountryPicker = { viewModel.perform(PickCountry()) },
        onErrorMessage = onErrorMessage,
        onSuccess = { route -> onSuccess(route) },
        state = state
    )
}

@Composable
fun CreateRecoveryScreen(
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit = {},
    onWantSkip: (ChallengeFrameDetails) -> Unit = {},
    onCountryPickerClicked: (List<Country>) -> Unit = {},
    onErrorMessage: (String?) -> Unit = {},
    onTabSelected: (RecoveryMethod) -> Unit = {},
    onRecoverySubmitted: (CreateRecoveryAction) -> Unit = {},
    onCountryPicker: () -> Unit = {},
    onSuccess: (String) -> Unit = { },
    state: SignUpState
) {
    LaunchedEffect(state) {
        when (state) {
            is ValidationError -> onErrorMessage(state.message)
            is Success -> onSuccess(state.route)
            is WantCountryPicker -> onCountryPickerClicked(state.countries)
            else -> Unit
        }
    }

    RecoveryMethodScaffold(
        modifier = modifier,
        onBackClicked = onBackClicked,
        onWantSkipClicked = onWantSkip,
        onRecoverySubmitted = onRecoverySubmitted,
        onTabSelected = onTabSelected,
        onCountryPicker = onCountryPicker,
        state = state
    )
}

@Composable
fun RecoveryMethodScaffold(
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit = {},
    onWantSkipClicked: (ChallengeFrameDetails) -> Unit = {},
    onTabSelected: (RecoveryMethod) -> Unit = {},
    onRecoverySubmitted: (CreateRecoveryAction) -> Unit = {},
    onCountryPicker: () -> Unit = {},
    state: SignUpState
) {
    val isLoading = state is Creating
    val emailError = (state as? Email)?.message
    val phoneError = (state as? Phone)?.message
    val selectedCountry = (state as? CreateRecoveryState)?.country()

    val currentMethod = (state as? CreateRecoveryState)?.currentMethod() ?: RecoveryMethod.Email
    val payloadController = remember(currentMethod) { PayloadController() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            ProtonTopAppBar(
                title = {},
                navigationIcon = {
                    NavigationBackButton(onBackClicked = onBackClicked)
                },
                actions = {
                    ProtonTextButton(
                        onClick = {
                            scope.launch {
                                val frame = payloadController.flush()
                                onWantSkipClicked(frame)
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.auth_signup_recovery_skip),
                            color = ProtonTheme.colors.textAccent,
                            style = ProtonTheme.typography.defaultStrongNorm
                        )
                    }
                },
                backgroundColor = LocalColors.current.backgroundNorm,
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(DefaultSpacing)
            ) {
                Text(
                    style = LocalTypography.current.headline,
                    text = stringResource(id = R.string.auth_signup_recovery_title)
                )

                Text(
                    text = stringResource(id = R.string.auth_signup_recovery_subtitle),
                    style = ProtonTypography.Default.defaultSmallWeak,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = SmallSpacing)
                )

                RecoveryTabs(
                    modifier = modifier,
                    tabs = listOf("Email", "Phone"),
                    initialSelectedMethod = currentMethod,
                    onTabSelected = {
                        onTabSelected(RecoveryMethod.enumOf(it))
                    }
                )

                RecoveryMethodsForms(
                    payloadController = payloadController,
                    currentMethod = currentMethod,
                    isLoading = isLoading,
                    emailError = emailError,
                    phoneError = phoneError,
                    selectedCountry = selectedCountry,
                    onRecoverySubmitted = onRecoverySubmitted,
                    onCountryPicker = onCountryPicker
                )
            }
        }
    }
}

@Composable
fun RecoveryMethodsForms(
    payloadController: PayloadController,
    currentMethod: RecoveryMethod,
    isLoading: Boolean,
    emailError: String?,
    phoneError: String?,
    selectedCountry: Country?,
    onRecoverySubmitted: (CreateRecoveryAction) -> Unit,
    onCountryPicker: () -> Unit
) {
    when (currentMethod) {
        RecoveryMethod.Email -> RecoveryMethodFormEmail(
            emailPayloadController = payloadController,
            loading = isLoading,
            emailError = emailError,
            onEmailSubmitted = onRecoverySubmitted
        )

        RecoveryMethod.Phone -> RecoveryMethodFormPhone(
            phonePayloadController = payloadController,
            loading = isLoading,
            selectedCountry = selectedCountry,
            emailError = phoneError,
            onPhoneSubmitted = onRecoverySubmitted,
            onCountryPicker = onCountryPicker
        )
    }
}

@Composable
fun RecoveryMethodFormEmail(
    emailPayloadController: PayloadController,
    loading: Boolean = false,
    emailError: String? = null,
    onEmailSubmitted: (SubmitRecoveryEmail) -> Unit = {}
) {
    var email by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val emailChanges = remember { MutableStateFlow(TextChange()) }
    val emailHasFocus = remember { mutableStateOf(false) }
    val emailTextCopies = remember { MutableStateFlow("") }

    fun onSubmit() = scope.launch {
        val frameDetails = emailPayloadController.flush()
        onEmailSubmitted(SubmitRecoveryEmail(email = email, recoveryFrameDetails = frameDetails))
    }

    ProtonOutlinedTextFieldWithError(
        text = email,
        onValueChanged = {
            emailChanges.value = emailChanges.value.roll(it)
            email = it
        },
        enabled = !loading,
        errorText = emailError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        label = { Text(text = stringResource(id = R.string.auth_email)) },
        singleLine = true,
        modifier = Modifier
            .onFocusChanged { emailHasFocus.value = it.hasFocus }
            .fillMaxWidth()
            .padding(top = DefaultSpacing)
            .payload(
                flow = SIGNUP_CHALLENGE_FLOW_NAME,
                frame = SIGNUP_CHALLENGE_RECOVERY_FRAME,
                onTextChanged = emailChanges.map { it.toPair() },
                onTextCopied = emailTextCopies,
                onFrameUpdated = {},
                payloadController = emailPayloadController
            )
            .testTag(EMAIL_FIELD_TAG)
    )

    ProtonSolidButton(
        contained = false,
        loading = loading,
        modifier = Modifier
            .padding(top = MediumSpacing)
            .height(ProtonDimens.DefaultButtonMinHeight),
        onClick = ::onSubmit
    ) {
        Text(
            text = stringResource(id = R.string.auth_signup_next)
        )
    }
}

@Composable
fun RecoveryMethodFormPhone(
    phonePayloadController: PayloadController,
    emailError: String? = null,
    loading: Boolean = false,
    selectedCountry: Country?,
    onPhoneSubmitted: (SubmitRecoveryPhone) -> Unit,
    onCountryPicker: () -> Unit
) {
    var callingCode by rememberSaveable { mutableStateOf(selectedCountry?.callingCode?.toString() ?: "") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val phoneChanges = remember { MutableStateFlow(TextChange()) }
    val phoneHasFocus = remember { mutableStateOf(false) }

    val phoneTextCopies = remember { MutableStateFlow("") }

    LaunchedEffect(selectedCountry) {
        callingCode = selectedCountry?.callingCode?.toString() ?: ""
    }

    fun onSubmit() = scope.launch {
        val frameDetails = phonePayloadController.flush()
        onPhoneSubmitted(
            SubmitRecoveryPhone(
                callingCode = callingCode,
                phoneNumber = phoneNumber,
                recoveryFrameDetails = frameDetails
            )
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            CountryCodeDropDown(
                modifier = Modifier
                    .padding(top = LargeSpacing)
                    .height(TextFieldDefaults.MinHeight),
                country = selectedCountry,
                onNavigateToCountryPicker = onCountryPicker
            )
        }

        Spacer(modifier = Modifier.width(SmallSpacing))

        Column(
            modifier = Modifier.weight(2f)
        ) {
            ProtonOutlinedTextFieldWithError(
                text = phoneNumber,
                onValueChanged = {
                    phoneChanges.value = phoneChanges.value.roll(it)
                    phoneNumber = it
                },
                enabled = !loading,
                errorText = emailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                label = { Text(text = stringResource(id = R.string.auth_signup_phone_placeholder)) },
                singleLine = true,
                modifier = Modifier
                    .onFocusChanged { phoneHasFocus.value = it.hasFocus }
                    .padding(top = MediumSpacing)
                    .payload(
                        flow = SIGNUP_CHALLENGE_FLOW_NAME,
                        frame = SIGNUP_CHALLENGE_RECOVERY_FRAME,
                        onTextChanged = phoneChanges.map { it.toPair() },
                        onTextCopied = phoneTextCopies,
                        onFrameUpdated = {},
                        payloadController = phonePayloadController
                    )
                    .testTag(PHONE_FIELD_TAG)
            )
        }
    }

    ProtonSolidButton(
        contained = false,
        loading = loading,
        modifier = Modifier
            .padding(top = MediumSpacing)
            .height(ProtonDimens.DefaultButtonMinHeight),
        onClick = ::onSubmit
    ) {
        Text(
            text = stringResource(id = R.string.auth_signup_next)
        )
    }
}

@Composable
fun RecoveryTabs(
    modifier: Modifier = Modifier,
    tabs: List<String>,
    initialSelectedMethod: RecoveryMethod = RecoveryMethod.Email,
    onTabSelected: (Int) -> Unit = {}
) {
    var selectedIndex by remember { mutableIntStateOf(initialSelectedMethod.value) }

    TabRow(
        modifier = modifier
            .fillMaxSize()
            .padding(top = DefaultSpacing),
        selectedTabIndex = selectedIndex,
        contentColor = ProtonTheme.colors.textNorm,
        backgroundColor = Color.Transparent,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex])
            )
        }
    ) {
        tabs.forEachIndexed { index, tabTitle ->
            Tab(
                modifier = Modifier.heightIn(min = LocalViewConfiguration.current.minimumTouchTargetSize.height),
                selected = selectedIndex == index,
                onClick = {
                    selectedIndex = index
                    onTabSelected(index)
                }
            ) {
                Text(
                    style = ProtonTheme.typography.defaultStrongNorm,
                    color = if (selectedIndex == index) ProtonTheme.colors.textNorm else ProtonTheme.colors.textWeak,
                    text = tabTitle.uppercase()
                )
            }
        }
    }
}

private fun CreateRecoveryState.country(): Country? = when (this) {
    is OnCountryPicked -> country
    is CountryPickerFailed -> country
    is Idle -> defaultCountry
    else -> null
}

private fun CreateRecoveryState.currentMethod() = when (this) {
    is Idle -> recoveryMethod
    is WantCountryPicker -> recoveryMethod
    is OnCountryPicked -> recoveryMethod
    is Creating -> recoveryMethod
    is Error -> recoveryMethod
    is Success -> recoveryMethod
    is Email -> RecoveryMethod.Email
    is Phone -> RecoveryMethod.Phone
    is WantSkip -> recoveryMethod
    is SkipSuccess -> recoveryMethod
    is SkipFailed -> recoveryMethod
    is CountryPickerFailed -> recoveryMethod
    Closed -> RecoveryMethod.Email // default
}

@Preview(name = "Light mode", showBackground = true)
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Small screen height", heightDp = SMALL_SCREEN_HEIGHT)
@Preview(name = "Foldable", device = Devices.PIXEL_FOLD)
@Preview(name = "Tablet", device = Devices.PIXEL_C)
@Preview(name = "Horizontal", widthDp = 800, heightDp = 360)
@Composable
internal fun RecoveryMethodScreenPreview() {
    ProtonTheme {
        CreateRecoveryScreen()
    }
}

@Preview(name = "Light mode", showBackground = true)
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Small screen height", heightDp = SMALL_SCREEN_HEIGHT)
@Preview(name = "Foldable", device = Devices.PIXEL_FOLD)
@Preview(name = "Tablet", device = Devices.PIXEL_C)
@Preview(name = "Horizontal", widthDp = 800, heightDp = 360)
@Composable
@Suppress("MagicNumber")
internal fun CreateRecoveryPhoneScreenPreview() {
    ProtonTheme {
        CreateRecoveryScreen(
            state = Idle(
                recoveryMethod = RecoveryMethod.Phone,
                listOf(
                    Country("CH", 41, "Switzerland"),
                    Country("US", 1, "US")
                ),
                defaultCountry = Country("CH", 41, "Switzerland")
            )
        )
    }
}
