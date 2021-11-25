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

package ch.protonmail.android.navigation.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.navigation.common.rememberFlowWithLifecycle
import me.proton.core.domain.entity.UserId

@Composable
fun LauncherScreen(
    navigateToMailbox: (userId: UserId) -> Unit
) {
    val launcherViewModel = hiltViewModel<LauncherViewModel>()
    val viewState by rememberFlowWithLifecycle(launcherViewModel.viewState())
        .collectAsState(initial = LauncherViewState.initialValue)
    Launcher(viewState, navigateToMailbox)
}

@Composable
internal fun Launcher(
    viewState: LauncherViewState,
    navigateToMailbox: (userId: UserId) -> Unit
) {
    when (val state = viewState.primaryAccountState) {
        is PrimaryAccountState.SignedIn -> navigateToMailbox(state.userId)
        else -> CenteredProgress()
    }
}

@Composable
private fun CenteredProgress() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth()
    ) { CircularProgressIndicator() }
}
