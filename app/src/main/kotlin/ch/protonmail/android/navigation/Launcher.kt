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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.MainActivity
import ch.protonmail.android.navigation.model.LauncherState
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.domain.entity.UserId

@Composable
fun Launcher(activityActions: MainActivity.Actions, viewModel: LauncherViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState(LauncherState.Processing)

    when (state) {
        LauncherState.AccountNeeded -> viewModel.submit(LauncherViewModel.Action.AddAccount)
        LauncherState.PrimaryExist -> LauncherRouter(
            activityActions = activityActions,
            launcherActions = Launcher.Actions(
                onPasswordManagement = { viewModel.submit(LauncherViewModel.Action.OpenPasswordManagement) },
                onRecoveryEmail = { viewModel.submit(LauncherViewModel.Action.OpenRecoveryEmail) },
                onReportBug = { viewModel.submit(LauncherViewModel.Action.OpenReport) },
                onSignIn = { viewModel.submit(LauncherViewModel.Action.SignIn(it)) },
                onSubscription = { viewModel.submit(LauncherViewModel.Action.OpenSubscription) },
                onSwitchAccount = { viewModel.submit(LauncherViewModel.Action.Switch(it)) },
                onRequestNotificationPermission = {
                    viewModel.submit(LauncherViewModel.Action.RequestNotificationPermission)
                }
            )
        )
        LauncherState.Processing,
        LauncherState.StepNeeded -> ProtonCenteredProgress(Modifier.fillMaxSize())
    }
}

object Launcher {

    /**
     * A set of actions that can be executed in the scope of Core's Orchestrators
     */
    data class Actions(
        val onSignIn: (UserId?) -> Unit,
        val onSwitchAccount: (UserId) -> Unit,
        val onSubscription: () -> Unit,
        val onReportBug: () -> Unit,
        val onPasswordManagement: () -> Unit,
        val onRecoveryEmail: () -> Unit,
        val onRequestNotificationPermission: () -> Unit
    )
}
