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

package me.proton.android.core.auth.presentation.login

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import me.proton.android.core.auth.presentation.AuthOrchestrator
import me.proton.android.core.auth.presentation.R
import me.proton.android.core.devicemigration.presentation.StartMigrationFromTarget
import me.proton.android.core.devicemigration.presentation.TargetDeviceMigrationResult
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.presentation.ui.ProtonActivity
import me.proton.core.presentation.utils.openBrowserLink
import javax.inject.Inject

@AndroidEntryPoint
class LoginHelpActivity : ProtonActivity() {

    @Inject
    lateinit var authOrchestrator: AuthOrchestrator

    @Inject
    lateinit var applicationLogsNavigator: LoginHelpApplicationLogsNavigator

    private lateinit var qrLoginLauncher: ActivityResultLauncher<Unit>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authOrchestrator.register(this)

        qrLoginLauncher = registerForActivityResult(StartMigrationFromTarget()) {
            when (it) {
                is TargetDeviceMigrationResult.NavigateToSignIn -> authOrchestrator.startLoginWorkflow()
                is TargetDeviceMigrationResult.SignedIn -> {
                    setResult(
                        RESULT_OK,
                        Intent().apply { putExtra(ARG_OUTPUT, LoginHelpOutput.SignedInWithQrCode(it.userId)) }
                    )
                    finish()
                }

                null -> Unit
            }
        }

        setContent {
            ProtonTheme {
                LoginHelpScreen(
                    onCloseClicked = { finish() },
                    onCustomerSupportClicked = {
                        openBrowserLink(getString(R.string.login_help_link_customer_support))
                    },
                    onForgotPasswordClicked = {
                        openBrowserLink(getString(R.string.login_help_link_forgot_password))
                    },
                    onForgotUsernameClicked = {
                        openBrowserLink(getString(R.string.login_help_link_forgot_username))
                    },
                    onOtherLoginIssuesClicked = {
                        openBrowserLink(getString(R.string.login_help_link_other_issues))
                    },
                    onSignInWithQrCodeClicked = {
                        qrLoginLauncher.launch(Unit)
                    },
                    onApplicationLogsClicked = { applicationLogsNavigator.navigate(this) }
                )
            }
        }
    }

    override fun onDestroy() {
        authOrchestrator.unregister()
        super.onDestroy()
    }

    companion object {

        const val ARG_OUTPUT = "arg.loginHelpOutput"
    }
}

@Parcelize
sealed interface LoginHelpOutput : Parcelable {

    /** User has signed in using Easy Device Migration (QR code). */
    data class SignedInWithQrCode(val userId: String) : LoginHelpOutput
}
