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

package ch.protonmail.android.feature.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.feature.account.SignOutAccountViewModel.State
import me.proton.core.accountmanager.presentation.compose.SignOutDialog
import me.proton.core.domain.entity.UserId

@Composable
fun SignOutAccountDialog(
    modifier: Modifier = Modifier,
    userId: UserId? = null,
    actions: SignOutAccountDialog.Actions,
    viewModel: SignOutAccountViewModel = hiltViewModel()
) {
    val viewState by viewModel.state.collectAsStateWithLifecycle()

    when (viewState) {
        State.SignedOut -> actions.onSignedOut()
        State.Removed -> actions.onRemoved()
        else -> Unit
    }

    SignOutDialog(
        modifier = modifier,
        onDismiss = actions.onCancelled,
        onDisableAccount = { viewModel.signOut(userId, removeAccount = false) },
        onRemoveAccount = { viewModel.signOut(userId, removeAccount = true) }
    )
}

object SignOutAccountDialog {

    const val USER_ID_KEY = "user id"

    data class Actions(
        val onSignedOut: () -> Unit,
        val onRemoved: () -> Unit,
        val onCancelled: () -> Unit
    )
}

object SignOutAccountDialogTestTags {

    const val RootItem = "SignOutAccountDialogRootItem"
    const val YesButton = "YesButton"
    const val NoButton = "NoButton"
}
