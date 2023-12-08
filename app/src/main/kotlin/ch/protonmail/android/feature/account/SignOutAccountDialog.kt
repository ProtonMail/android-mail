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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.R
import ch.protonmail.android.feature.account.SignOutAccountViewModel.State
import ch.protonmail.android.feature.account.SignOutAccountViewModel.State.Initial
import ch.protonmail.android.feature.account.SignOutAccountViewModel.State.SignedOut
import ch.protonmail.android.feature.account.SignOutAccountViewModel.State.SigningOut
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogText
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.domain.entity.UserId

@Composable
fun SignOutAccountDialog(
    modifier: Modifier = Modifier,
    userId: UserId? = null,
    onSignedOut: () -> Unit,
    onCancelled: () -> Unit,
    signOutViewModel: SignOutAccountViewModel = hiltViewModel()
) {
    val viewState by rememberAsState(signOutViewModel.state, Initial)

    when (viewState) {
        Initial -> Unit
        SigningOut -> Unit
        SignedOut -> onSignedOut()
    }

    SignOutAccountDialog(
        modifier = modifier,
        viewState = viewState,
        onCancelClicked = onCancelled,
        onSignOutClicked = { signOutViewModel.signOut(userId) }
    )
}

@Composable
private fun SignOutAccountDialog(
    modifier: Modifier = Modifier,
    viewState: State,
    onCancelClicked: () -> Unit,
    onSignOutClicked: () -> Unit
) {
    ProtonAlertDialog(
        modifier = modifier.testTag(SignOutAccountDialogTestTags.RootItem),
        onDismissRequest = onCancelClicked,
        title = stringResource(id = R.string.dialog_sign_out_account_title),
        text = { ProtonAlertDialogText(text = stringResource(id = R.string.dialog_sign_out_account_description)) },
        confirmButton = {
            ProtonTextButton(
                modifier = Modifier.testTag(SignOutAccountDialogTestTags.YesButton),
                onClick = onSignOutClicked
            ) {
                when (viewState) {
                    Initial,
                    SignedOut -> Text(
                        text = stringResource(id = R.string.dialog_sign_out_account_confirm),
                        style = ProtonTheme.typography.defaultStrongNorm,
                        color = ProtonTheme.colors.textAccent
                    )

                    SigningOut -> CircularProgressIndicator()
                }
            }
        },
        dismissButton = {
            ProtonTextButton(
                modifier = Modifier.testTag(SignOutAccountDialogTestTags.NoButton),
                onClick = onCancelClicked
            ) {
                Text(
                    text = stringResource(id = R.string.dialog_sign_out_account_cancel),
                    style = ProtonTheme.typography.defaultStrongNorm,
                    color = ProtonTheme.colors.textAccent
                )
            }
        }
    )
}

object SignOutAccountDialog {

    const val USER_ID_KEY = "user id"
}

object SignOutAccountDialogTestTags {

    const val RootItem = "SignOutAccountDialogRootItem"
    const val YesButton = "YesButton"
    const val NoButton = "NoButton"
}
