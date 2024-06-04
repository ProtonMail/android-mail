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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.uicomponents.dismissKeyboard
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.ManageMembersUiModel
import ch.protonmail.android.mailcontact.presentation.previewdata.ManageMembersPreviewData
import ch.protonmail.android.mailcontact.presentation.ui.FormInputField
import ch.protonmail.android.uicomponents.snackbar.DismissableSnackbarHost
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.contact.domain.entity.ContactEmailId

@Composable
fun ManageMembersScreen(
    actions: ManageMembersScreen.Actions,
    selectedContactEmailsIds: State<List<String>?>?,
    viewModel: ManageMembersViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostErrorState = ProtonSnackbarHostState(defaultType = ProtonSnackbarType.ERROR)
    val state = rememberAsState(flow = viewModel.state, initial = ManageMembersViewModel.initialState).value

    if (state !is ManageMembersState.Data) {
        viewModel.initViewModelWithData(
            selectedContactEmailsIds?.value?.map { ContactEmailId(it) } ?: emptyList()
        )
    }

    Scaffold(
        topBar = {
            ManageMembersTopBar(
                actions = actions,
                isDoneEnabled = state is ManageMembersState.Data,
                onDoneClick = {
                    viewModel.submit(ManageMembersViewAction.OnDoneClick)
                }
            )
        },
        content = { paddingValues ->
            when (state) {
                is ManageMembersState.Data -> {
                    ManageMembersContent(
                        state = state,
                        actions = ManageMembersContent.Actions(
                            onMemberClick = {
                                viewModel.submit(ManageMembersViewAction.OnMemberClick(it))
                            },
                            onSearchValueChange = {
                                viewModel.submit(ManageMembersViewAction.OnSearchValueChanged(it))
                            }
                        )
                    )

                    ConsumableTextEffect(effect = state.showErrorSnackbar) { message ->
                        snackbarHostErrorState.showSnackbar(
                            message = message,
                            type = ProtonSnackbarType.ERROR
                        )
                    }
                    ConsumableLaunchedEffect(effect = state.onDone) { selectedContactEmailIds ->
                        actions.onDone(selectedContactEmailIds)
                    }
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
            DismissableSnackbarHost(
                modifier = Modifier.testTag(CommonTestTags.SnackbarHostError),
                protonSnackbarHostState = snackbarHostErrorState
            )
        }
    )

    ConsumableLaunchedEffect(effect = state.close) {
        dismissKeyboard(context, view, keyboardController)
        actions.onClose
    }
}

@Composable
fun ManageMembersContent(
    modifier: Modifier = Modifier,
    state: ManageMembersState.Data,
    actions: ManageMembersContent.Actions
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        item {
            FormInputField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ProtonDimens.DefaultSpacing),
                initialValue = state.searchValue,
                hint = stringResource(R.string.search_contact),
                showClearTextIcon = true,
                onTextChange = {
                    actions.onSearchValueChange(it)
                }
            )
        }
        items(state.members) { member ->
            if (member.isDisplayed) {
                ManageMembersItem(
                    member = member,
                    actions = actions
                )
            }
        }
    }
}

@Composable
fun ManageMembersItem(
    modifier: Modifier = Modifier,
    actions: ManageMembersContent.Actions,
    member: ManageMembersUiModel
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClick = {
                    actions.onMemberClick(member.id)
                }
            )
            .padding(start = ProtonDimens.DefaultSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ManageMembersAvatar(member)

        Column(
            modifier = Modifier
                .padding(
                    start = ProtonDimens.ListItemTextStartPadding,
                    top = ProtonDimens.ListItemTextStartPadding,
                    bottom = ProtonDimens.ListItemTextStartPadding,
                    end = ProtonDimens.DefaultSpacing
                )
                .weight(1f)
        ) {
            Text(
                text = member.name,
                style = ProtonTheme.typography.defaultNorm
            )
            Text(
                text = member.email,
                style = ProtonTheme.typography.defaultSmallWeak
            )
        }

        if (member.isSelected) {
            Icon(
                modifier = Modifier.padding(
                    start = ProtonDimens.SmallSpacing,
                    end = ProtonDimens.DefaultSpacing
                ),
                painter = painterResource(id = R.drawable.ic_proton_checkmark),
                tint = ProtonTheme.colors.iconAccent,
                contentDescription = NO_CONTENT_DESCRIPTION
            )
        }
    }
}

@Composable
private fun ManageMembersAvatar(member: ManageMembersUiModel) {
    Box(
        modifier = Modifier
            .sizeIn(
                minWidth = MailDimens.AvatarMinSize,
                minHeight = MailDimens.AvatarMinSize
            )
            .background(
                color = if (member.isSelected) ProtonTheme.colors.iconAccent
                else ProtonTheme.colors.interactionWeakNorm,
                shape = ProtonTheme.shapes.medium
            ),
        contentAlignment = Alignment.Center
    ) {
        if (member.isSelected) {
            Icon(
                modifier = Modifier.size(ProtonDimens.SmallIconSize),
                painter = painterResource(id = R.drawable.ic_proton_users_filled),
                tint = Color.White,
                contentDescription = NO_CONTENT_DESCRIPTION
            )
        } else {
            Text(
                textAlign = TextAlign.Center,
                text = member.initials
            )
        }
    }
}

@Composable
fun ManageMembersTopBar(
    actions: ManageMembersScreen.Actions,
    isDoneEnabled: Boolean,
    onDoneClick: () -> Unit
) {
    ProtonTopAppBar(
        modifier = Modifier.fillMaxWidth(),
        title = {
            Text(text = stringResource(id = R.string.manage_members_title))
        },
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

    data class Actions(
        val onDone: (List<ContactEmailId>) -> Unit,
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

object ManageMembersContent {

    data class Actions(
        val onMemberClick: (ContactEmailId) -> Unit,
        val onSearchValueChange: (String) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onMemberClick = {},
                onSearchValueChange = {}
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ManageMembersContentPreview() {
    ManageMembersContent(
        state = ManageMembersState.Data(
            members = ManageMembersPreviewData.manageMembersSampleData()
        ),
        actions = ManageMembersContent.Actions.Empty
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EmptyManageMembersContentPreview() {
    ManageMembersContent(
        state = ManageMembersState.Data(
            members = emptyList()
        ),
        actions = ManageMembersContent.Actions.Empty
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ManageMembersTopBarPreview() {
    ManageMembersTopBar(
        actions = ManageMembersScreen.Actions.Empty,
        isDoneEnabled = true,
        onDoneClick = {}
    )
}
