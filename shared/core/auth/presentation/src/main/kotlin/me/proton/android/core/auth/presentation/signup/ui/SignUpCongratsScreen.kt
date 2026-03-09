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

package me.proton.android.core.auth.presentation.signup.ui

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.core.auth.presentation.R
import me.proton.android.core.auth.presentation.addaccount.SMALL_SCREEN_HEIGHT
import me.proton.android.core.auth.presentation.signup.SignUpAction.FinalizeSignup
import me.proton.android.core.auth.presentation.signup.SignUpState.LoginSuccess
import me.proton.android.core.auth.presentation.signup.SignUpState.SignUpError
import me.proton.android.core.auth.presentation.signup.viewmodel.SignUpViewModel
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTypography
import me.proton.core.compose.theme.defaultSmallWeak
import uniffi.mail_uniffi.SignupScreenId

@Composable
fun SignUpCongratsScreen(
    modifier: Modifier = Modifier,
    onStartUsingApp: (String) -> Unit = {},
    onErrorMessage: (String?) -> Unit = {},
    @DrawableRes congratsLogo: Int = R.drawable.ic_congratulations,
    @StringRes titleText: Int = R.string.auth_signup_congratulations_title,
    @StringRes subtitleText: Int = R.string.auth_signup_congratulations_subtitle,
    @StringRes buttonText: Int = R.string.auth_signup_start_using_proton,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    SignupScreenId.CONGRATULATIONS.LaunchOnScreenView(viewModel::onScreenView)

    val state by viewModel.state.collectAsStateWithLifecycle()

    val currentState = state
    LaunchedEffect(currentState) {
        when (currentState) {
            is LoginSuccess -> onStartUsingApp(currentState.userId)
            is SignUpError -> onErrorMessage(currentState.message)
            else -> Unit
        }
    }

    SignUpCongratsScreen(
        modifier = modifier,
        congratsLogo = congratsLogo,
        titleText = titleText,
        subtitleText = subtitleText,
        buttonText = buttonText,
        onStartClicked = { viewModel.perform(FinalizeSignup) }
    )
}

@Composable
fun SignUpCongratsScreen(
    modifier: Modifier = Modifier,
    onStartClicked: () -> Unit = {},
    @DrawableRes congratsLogo: Int = R.drawable.ic_congratulations,
    @StringRes titleText: Int = R.string.auth_signup_congratulations_title,
    @StringRes subtitleText: Int = R.string.auth_signup_congratulations_subtitle,
    @StringRes buttonText: Int = R.string.auth_signup_start_using_proton
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = ProtonDimens.DefaultSpacing)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
            ),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                modifier = Modifier
                    .width(160.dp)
                    .height(160.dp)
                    .align(Alignment.CenterHorizontally),
                painter = painterResource(congratsLogo),
                contentDescription = null,
                alignment = Alignment.Center
            )

            Text(
                text = stringResource(titleText),
                style = ProtonTypography.Default.headline,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = ProtonDimens.MediumSpacing)
            )

            Text(
                text = stringResource(subtitleText),
                style = ProtonTypography.Default.defaultSmallWeak,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = ProtonDimens.SmallSpacing)
            )
        }

        ProtonSolidButton(
            contained = false,
            onClick = onStartClicked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = ProtonDimens.LargeSpacing)
                .height(ProtonDimens.DefaultButtonMinHeight)
        ) {
            Text(text = stringResource(buttonText))
        }
    }
}

@Preview(name = "Light mode", showBackground = true)
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Small screen height", heightDp = SMALL_SCREEN_HEIGHT)
@Preview(name = "Foldable", device = Devices.PIXEL_FOLD)
@Preview(name = "Tablet", device = Devices.PIXEL_C)
@Preview(name = "Horizontal", widthDp = 800, heightDp = 360)
@Composable
internal fun SignUpCongratsPreview() {
    ProtonTheme {
        SignUpCongratsScreen()
    }
}
