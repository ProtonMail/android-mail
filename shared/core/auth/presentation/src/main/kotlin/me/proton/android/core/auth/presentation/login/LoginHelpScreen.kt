/*
 * Copyright (C) 2024 Proton AG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

@file:Suppress("UseComposableActions")

package me.proton.android.core.auth.presentation.login

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import me.proton.android.core.auth.presentation.R
import me.proton.android.core.auth.presentation.help.LoginHelpItem
import me.proton.core.compose.component.ProtonCloseButton
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.theme.LocalColors
import me.proton.core.compose.theme.LocalTypography
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.presentation.R as CoreR

@Composable
fun LoginHelpScreen(
    modifier: Modifier = Modifier,
    onCloseClicked: () -> Unit = {},
    onCustomerSupportClicked: () -> Unit = {},
    onForgotPasswordClicked: () -> Unit = {},
    onForgotUsernameClicked: () -> Unit = {},
    onOtherLoginIssuesClicked: () -> Unit = {},
    onSignInWithQrCodeClicked: () -> Unit = {},
    onApplicationLogsClicked: (() -> Unit)? = null
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        topBar = {
            ProtonTopAppBar(
                title = {},
                navigationIcon = { ProtonCloseButton(onCloseClicked = onCloseClicked) },
                backgroundColor = LocalColors.current.backgroundNorm,
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
                )
        ) {
            HelpColumn(
                onCustomerSupportClicked = onCustomerSupportClicked,
                onForgotPasswordClicked = onForgotPasswordClicked,
                onForgotUsernameClicked = onForgotUsernameClicked,
                onOtherLoginIssuesClicked = onOtherLoginIssuesClicked,
                onSignInWithQrCodeClicked = onSignInWithQrCodeClicked,
                onApplicationLogsClicked = onApplicationLogsClicked,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun HelpColumn(
    modifier: Modifier = Modifier,
    onCustomerSupportClicked: () -> Unit = {},
    onForgotPasswordClicked: () -> Unit = {},
    onForgotUsernameClicked: () -> Unit = {},
    onOtherLoginIssuesClicked: () -> Unit = {},
    onSignInWithQrCodeClicked: () -> Unit = {},
    onApplicationLogsClicked: (() -> Unit)? = null
) {
    var contactHeaderTapCount by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(id = R.string.login_help_title),
            style = LocalTypography.current.headline,
            modifier = Modifier
                .padding(horizontal = ProtonDimens.DefaultSpacing)
                .padding(bottom = ProtonDimens.MediumSpacing)
        )
        LoginHelpItem(
            icon = CoreR.drawable.ic_proton_qr_code,
            title = R.string.login_help_sign_in_with_qr_code,
            onClick = onSignInWithQrCodeClicked
        )
        LoginHelpItem(
            icon = CoreR.drawable.ic_proton_user_circle,
            title = R.string.login_help_forgot_username,
            onClick = onForgotUsernameClicked
        )
        LoginHelpItem(
            icon = CoreR.drawable.ic_proton_key,
            title = R.string.login_help_forgot_password,
            onClick = onForgotPasswordClicked
        )
        LoginHelpItem(
            icon = CoreR.drawable.ic_proton_question_circle,
            title = R.string.login_help_other_issues,
            onClick = onOtherLoginIssuesClicked
        )
        Text(
            text = stringResource(id = R.string.login_help_contact_needed),
            style = LocalTypography.current.body2Regular,
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { if (onApplicationLogsClicked != null) contactHeaderTapCount++ }
                .padding(
                    start = ProtonDimens.DefaultSpacing,
                    end = ProtonDimens.DefaultSpacing,
                    top = ProtonDimens.LargerSpacing
                )
        )
        LoginHelpItem(
            icon = painterResource(id = CoreR.drawable.ic_proton_speech_bubble),
            title = stringResource(id = R.string.login_help_customer_support),
            onClick = onCustomerSupportClicked,
            modifier = Modifier.padding(top = ProtonDimens.MediumSpacing)
        )
        if (onApplicationLogsClicked != null && contactHeaderTapCount >= SHOW_LOGS_TAPS_THRESHOLD) {
            LoginHelpItem(
                icon = CoreR.drawable.ic_proton_bug,
                title = R.string.login_help_application_logs,
                onClick = onApplicationLogsClicked
            )
        }
        Spacer(
            modifier = Modifier
                .height(ProtonDimens.MediumSpacing)
                .fillMaxWidth()
        )
    }
}

private const val SHOW_LOGS_TAPS_THRESHOLD = 5

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Horizontal", widthDp = 800, heightDp = 360)
@Composable
internal fun LoginHelpScreenPreview() {
    ProtonTheme {
        LoginHelpScreen()
    }
}
