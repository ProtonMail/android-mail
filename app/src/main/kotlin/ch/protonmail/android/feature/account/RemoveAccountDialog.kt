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

import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
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
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogText
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.domain.entity.UserId

@Composable
fun RemoveAccountDialog(
    modifier: Modifier = Modifier,
    userId: UserId? = null,
    onCancelled: () -> Unit,
    onRemoved: () -> Unit,
    removeAccountViewModel: RemoveAccountViewModel = hiltViewModel()
) {
    val viewState by rememberAsState(removeAccountViewModel.state, Initial)

    when (viewState) {
        Initial -> Unit
        Removing -> Unit
        Removed -> onRemoved()
    }

    RemoveAccountDialog(
        modifier = modifier,
        viewState = viewState,
        onCancelClicked = onCancelled,
        onRemoveClicked = { removeAccountViewModel.remove(userId) }
    )
}

@Composable
private fun RemoveAccountDialog(
    modifier: Modifier = Modifier,
    viewState: State,
    onCancelClicked: () -> Unit,
    onRemoveClicked: () -> Unit
) {
    ProtonAlertDialog(
        modifier = modifier,
        onDismissRequest = onCancelClicked,
        title = stringResource(id = R.string.dialog_remove_account_title),
        text = { ProtonAlertDialogText(text = stringResource(id = R.string.dialog_remove_account_description)) },
        confirmButton = {
            ProtonTextButton(
                onClick = onRemoveClicked,
                content = {
                    when (viewState) {
                        Initial,
                        Removed -> Text(
                            text = stringResource(id = R.string.dialog_remove_account_confirm),
                            style = ProtonTheme.typography.defaultStrongNorm,
                            color = ProtonTheme.colors.textAccent
                        )

                        Removing -> CircularProgressIndicator()
                    }
                }
            )
        },
        dismissButton = {
            ProtonTextButton(onClick = onCancelClicked) {
                Text(
                    text = stringResource(id = R.string.dialog_remove_account_cancel),
                    style = ProtonTheme.typography.defaultStrongNorm,
                    color = ProtonTheme.colors.textAccent
                )
            }
        }
    )
}

object RemoveAccountDialog {

    const val USER_ID_KEY = "user id"
}
