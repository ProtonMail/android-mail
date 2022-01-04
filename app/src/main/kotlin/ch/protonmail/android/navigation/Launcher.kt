/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.core.compose.component.ProtonCenteredProgress

@Composable
fun Launcher(
    viewModel: LauncherViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState(LauncherViewModel.State.Processing)

    when (state) {
        LauncherViewModel.State.AccountNeeded -> viewModel.addAccount()
        LauncherViewModel.State.PrimaryExist -> Home(
            onSignIn = { viewModel.signIn(it) },
            onSignOut = { viewModel.signOut(it) },
            onSwitch = { viewModel.switch(it) }
        )
        LauncherViewModel.State.Processing,
        LauncherViewModel.State.StepNeeded -> ProtonCenteredProgress(Modifier.fillMaxSize())
    }
}
