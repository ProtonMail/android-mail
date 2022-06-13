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

import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.R
import ch.protonmail.android.feature.account.RemoveAccountViewModel.State
import ch.protonmail.android.feature.account.RemoveAccountViewModel.State.Initial
import ch.protonmail.android.feature.account.RemoveAccountViewModel.State.Removed
import ch.protonmail.android.feature.account.RemoveAccountViewModel.State.Removing
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.exhaustive

@Composable
fun RemoveAccountDialog(
    onRemoved: () -> Unit,
    onCancelled: () -> Unit,
    modifier: Modifier = Modifier,
    userId: UserId? = null,
    removeAccountViewModel: RemoveAccountViewModel = hiltViewModel()
) {
    val viewState by rememberAsState(removeAccountViewModel.state, Initial)

    when (viewState) {
        Initial -> Unit
        Removed -> onRemoved()
        Removing -> Unit
    }.exhaustive

    RemoveAccountDialog(
        viewState = viewState,
        onCancelClicked = onCancelled,
        onRemoveClicked = { removeAccountViewModel.remove(userId) },
        modifier
    )
}

@Composable
private fun RemoveAccountDialog(
    viewState: State,
    onCancelClicked: () -> Unit,
    onRemoveClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onCancelClicked,
        title = { Text(text = stringResource(id = R.string.title_remove_account)) },
        text = { Text(text = stringResource(id = R.string.description_remove_account)) },
        confirmButton = {
            TextButton(
                onClick = onRemoveClicked,
                content = {
                    when (viewState) {
                        Initial,
                        Removed -> Text(text = stringResource(id = R.string.title_remove))
                        Removing -> CircularProgressIndicator()
                    }.exhaustive
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onCancelClicked) {
                Text(text = stringResource(id = R.string.presentation_alert_cancel))
            }
        }
    )
}

object RemoveAccountDialog {

    const val USER_ID_KEY = "user id"
}
