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

package ch.protonmail.android.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.MainActivity
import ch.protonmail.android.R
import ch.protonmail.android.design.compose.component.ProtonCenteredProgress
import ch.protonmail.android.legacymigration.presentation.MigrationLoadingScreen
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.navigation.model.LauncherState
import me.proton.core.domain.entity.UserId

@Composable
fun Launcher(activityActions: MainActivity.Actions, viewModel: LauncherViewModel = hiltViewModel()) {
    // Do not set a default initial value because we may miss a state update
    val state by viewModel.state.collectAsStateWithLifecycle()
    val effects = viewModel.duplicateDialogErrorEffect.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    ConsumableLaunchedEffect(effects) {
        Toast.makeText(
            context,
            context.getString(R.string.launcher_alert_duplicate_account_description),
            Toast.LENGTH_LONG
        ).show()
    }

    val signedInContent: @Composable (() -> Unit) = {
        LauncherRouter(
            activityActions = activityActions,
            launcherActions = Launcher.Actions(
                onPasswordManagement = { viewModel.submit(LauncherViewModel.Action.OpenPasswordManagement(it)) },
                onRecoveryEmail = { viewModel.submit(LauncherViewModel.Action.OpenRecoveryEmail) },
                onReportBug = { viewModel.submit(LauncherViewModel.Action.OpenReport) },
                onSignIn = { viewModel.submit(LauncherViewModel.Action.SignIn(it)) },
                onSignUp = { viewModel.submit(LauncherViewModel.Action.SignUp) },
                onSubscription = { viewModel.submit(LauncherViewModel.Action.OpenSubscription) },
                onSwitchToAccount = { viewModel.submit(LauncherViewModel.Action.SwitchToAccount(it)) },
                onRequestNotificationPermission = {
                    viewModel.submit(LauncherViewModel.Action.RequestNotificationPermission)
                }
            )
        )
    }

    when (state) {
        LauncherState.AccountNeeded -> viewModel.accountWelcomeProvider.Welcome(viewModel, signedInContent)
        LauncherState.PrimaryExist -> signedInContent()

        LauncherState.Processing,
        LauncherState.StepNeeded -> ProtonCenteredProgress(Modifier.fillMaxSize())

        LauncherState.MigrationInProgress,
        LauncherState.ProcessingAfterMigration -> MigrationLoadingScreen()
    }
}

object Launcher {

    data class Actions(
        val onSignIn: (UserId?) -> Unit,
        val onSignUp: () -> Unit,
        val onSubscription: () -> Unit,
        val onReportBug: () -> Unit,
        val onPasswordManagement: (UserId?) -> Unit,
        val onRecoveryEmail: () -> Unit,
        val onSwitchToAccount: (UserId) -> Unit,
        val onRequestNotificationPermission: () -> Unit
    )
}
