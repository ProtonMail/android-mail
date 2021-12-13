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

package ch.protonmail.android.navigation.ui

import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.R
import ch.protonmail.android.compose.rememberFlowWithLifecycle
import ch.protonmail.android.navigation.viewmodel.SignOutViewModel
import ch.protonmail.android.navigation.viewmodel.SignOutViewModel.State
import ch.protonmail.android.navigation.viewmodel.SignOutViewModel.State.Initial
import ch.protonmail.android.navigation.viewmodel.SignOutViewModel.State.SignedOut
import ch.protonmail.android.navigation.viewmodel.SignOutViewModel.State.SigningOut
import me.proton.core.util.kotlin.exhaustive

@Composable
fun SignOutConfirmationDialog(
    onSignedOut: () -> Unit,
    onCancelled: () -> Unit,
    modifier: Modifier = Modifier,
    signOutViewModel: SignOutViewModel = hiltViewModel()
) {
    val viewState by rememberFlowWithLifecycle(flow = signOutViewModel.state)
        .collectAsState(initial = Initial)

    when (viewState) {
        Initial -> Unit
        SignedOut -> onSignedOut()
        SigningOut -> Unit
    }.exhaustive

    SignoutDialog(
        viewState = viewState,
        onDismiss = onCancelled,
        onSignOut = { signOutViewModel.signOut() },
        modifier
    )
}

@Composable
private fun SignoutDialog(
    viewState: State,
    onDismiss: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.title_remove_account)) },
        text = { Text(text = stringResource(id = R.string.description_remove_account)) },
        confirmButton = {
            TextButton(
                onClick = onSignOut,
                content = {
                    when (viewState) {
                        Initial,
                        SignedOut -> Text(text = stringResource(id = R.string.title_remove))
                        SigningOut -> CircularProgressIndicator()
                    }.exhaustive
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.presentation_alert_cancel))
            }
        }
    )
}
