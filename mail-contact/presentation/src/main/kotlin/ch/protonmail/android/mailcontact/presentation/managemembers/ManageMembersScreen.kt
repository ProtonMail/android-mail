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

package ch.protonmail.android.mailcontact.presentation.managemembers

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.compose.dismissKeyboard
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.mailcontact.presentation.R
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonSnackbarHost
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultStrongNorm

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ManageMembersScreen(actions: ManageMembersScreen.Actions, viewModel: ManageMembersViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostErrorState = ProtonSnackbarHostState(defaultType = ProtonSnackbarType.ERROR)
    val state = rememberAsState(flow = viewModel.state, initial = ManageMembersViewModel.initialState).value

    Scaffold(
        topBar = {
            ManageMembersTopBar(
                actions = actions,
                isDoneEnabled = state is ManageMembersState.Data,
                onDoneClick = {
                    // Call VM with done view action here
                }
            )
        },
        content = { paddingValues ->
            when (state) {
                is ManageMembersState.Data -> {
                    // Display content here
                }
                is ManageMembersState.Loading -> {
                    ProtonCenteredProgress(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize()
                    )

                    ConsumableTextEffect(effect = state.errorLoading) { message ->
                        actions.exitWithErrorMessage(message)
                    }
                }
            }
        },
        snackbarHost = {
            ProtonSnackbarHost(
                modifier = Modifier.testTag(CommonTestTags.SnackbarHostError),
                hostState = snackbarHostErrorState
            )
        }
    )

    ConsumableLaunchedEffect(effect = state.close) {
        dismissKeyboard(context, view, keyboardController)
        actions.onClose
    }
}

@Composable
private fun ManageMembersTopBar(
    actions: ManageMembersScreen.Actions,
    isDoneEnabled: Boolean,
    onDoneClick: () -> Unit
) {
    ProtonTopAppBar(
        modifier = Modifier.fillMaxWidth(),
        title = { },
        navigationIcon = {
            IconButton(onClick = actions.onClose) {
                Icon(
                    tint = ProtonTheme.colors.iconNorm,
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(id = R.string.presentation_close)
                )
            }
        },
        actions = {
            ProtonTextButton(
                onClick = onDoneClick,
                enabled = isDoneEnabled
            ) {
                Text(
                    text = stringResource(id = R.string.members_done),
                    color = ProtonTheme.colors.textAccent,
                    style = ProtonTheme.typography.defaultStrongNorm
                )
            }
        }
    )
}

object ManageMembersScreen {

    const val ManageMembersSelectedContactEmailIdsKey = "manage_members_selected_contact_email_ids_key"

    data class Actions(
        val onDone: (List<String>) -> Unit,
        val onClose: () -> Unit,
        val exitWithErrorMessage: (String) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onDone = {},
                onClose = {},
                exitWithErrorMessage = {}
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ContactDetailsTopBarPreview() {
    ManageMembersTopBar(
        actions = ManageMembersScreen.Actions.Empty,
        isDoneEnabled = true,
        onDoneClick = {}
    )
}
