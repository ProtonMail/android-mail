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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.R
import ch.protonmail.android.feature.account.SignOutAccountViewModel.State
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogText
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.domain.entity.UserId

@Composable
fun RemoveAccountDialog(
    modifier: Modifier = Modifier,
    userId: UserId? = null,
    onCancelled: () -> Unit,
    onRemoved: () -> Unit,
    viewModel: SignOutAccountViewModel = hiltViewModel()
) {
    val viewState by viewModel.state.collectAsStateWithLifecycle()

    when (viewState) {
        State.Removed -> onRemoved()
        else -> Unit
    }

    RemoveAccountDialog(
        modifier = modifier,
        viewState = viewState,
        onCancelClicked = onCancelled,
        onRemoveClicked = { viewModel.signOut(userId, removeAccount = true) }
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
                        State.Initial,
                        State.Removed -> Text(
                            text = stringResource(id = R.string.dialog_remove_account_confirm),
                            style = ProtonTheme.typography.defaultStrongNorm,
                            color = ProtonTheme.colors.textAccent
                        )

                        State.Removing -> CircularProgressIndicator()
                        else -> Unit
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
