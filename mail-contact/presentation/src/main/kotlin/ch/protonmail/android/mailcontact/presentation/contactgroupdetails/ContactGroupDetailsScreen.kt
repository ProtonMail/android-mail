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

package ch.protonmail.android.mailcontact.presentation.contactgroupdetails

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupDetailsMember
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactGroupDetailsPreviewData.contactGroupDetailsSampleData
import ch.protonmail.android.mailcontact.presentation.ui.DeleteContactGroupDialog
import ch.protonmail.android.mailcontact.presentation.ui.IconContactAvatar
import ch.protonmail.android.uicomponents.dismissKeyboard
import ch.protonmail.android.uicomponents.snackbar.DismissableSnackbarHost
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionWeak
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultSmallNorm
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.compose.theme.headlineNorm
import me.proton.core.label.domain.entity.LabelId

@Composable
fun ContactGroupDetailsScreen(
    actions: ContactGroupDetailsScreen.Actions,
    viewModel: ContactGroupDetailsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostErrorState = ProtonSnackbarHostState(defaultType = ProtonSnackbarType.ERROR)
    val state = rememberAsState(flow = viewModel.state, initial = ContactGroupDetailsViewModel.initialState).value

    Scaffold(
        topBar = {
            ContactGroupDetailsTopBar(
                state = state,
                actions = actions,
                onDeleteClick = {
                    viewModel.submit(ContactGroupDetailsViewAction.OnDeleteClick)
                }
            )
        },
        content = { paddingValues ->
            when (state) {
                is ContactGroupDetailsState.Data -> {
                    ContactGroupDetailsContent(
                        state = state,
                        onSendClick = {
                            viewModel.submit(ContactGroupDetailsViewAction.OnEmailClick)
                        }
                    )

                    ConsumableLaunchedEffect(effect = state.openComposer) { actions.navigateToComposer(it) }
                    ConsumableTextEffect(effect = state.deletionSuccess) { message ->
                        actions.exitWithNormMessage(message)
                    }
                    ConsumableTextEffect(effect = state.deletionError) { message ->
                        actions.showErrorMessage(message)
                    }

                    DeleteContactGroupDialog(
                        state = state.deleteDialogState,
                        confirm = { viewModel.submit(ContactGroupDetailsViewAction.OnDeleteConfirmedClick) },
                        dismiss = { viewModel.submit(ContactGroupDetailsViewAction.OnDeleteDismissedClick) }
                    )
                }

                is ContactGroupDetailsState.Loading -> {
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
        actions.onBackClick()
    }
}

@Composable
fun ContactGroupDetailsContent(
    state: ContactGroupDetailsState.Data,
    modifier: Modifier = Modifier,
    onSendClick: () -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
    ) {
        item {
            Column(modifier.fillMaxWidth()) {
                IconContactAvatar(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = ProtonDimens.DefaultSpacing),
                    iconResId = R.drawable.ic_proton_users,
                    backgroundColor = state.contactGroup.color
                )
                Text(
                    modifier = Modifier
                        .padding(
                            top = ProtonDimens.MediumSpacing,
                            start = ProtonDimens.MediumSpacing,
                            end = ProtonDimens.MediumSpacing
                        )
                        .align(Alignment.CenterHorizontally),
                    style = ProtonTheme.typography.headlineNorm,
                    text = state.contactGroup.name,
                    textAlign = TextAlign.Center
                )
                Text(
                    modifier = Modifier
                        .padding(
                            top = ProtonDimens.ExtraSmallSpacing,
                            start = ProtonDimens.MediumSpacing,
                            end = ProtonDimens.MediumSpacing
                        )
                        .align(Alignment.CenterHorizontally),
                    style = ProtonTheme.typography.captionWeak,
                    text = pluralStringResource(
                        R.plurals.contact_group_details_member_count,
                        state.contactGroup.memberCount,
                        state.contactGroup.memberCount
                    ),
                    textAlign = TextAlign.Center
                )
                ContactGroupDetailsSendTextButton(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    isEnabled = state.isSendEnabled,
                    onClick = onSendClick
                )
            }
        }
        items(state.contactGroup.members) { member ->
            ContactGroupMemberItem(
                contactGroupMember = member
            )
        }
    }
}

@Composable
private fun ContactGroupDetailsSendTextButton(
    modifier: Modifier = Modifier,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .padding(
                top = ProtonDimens.DefaultSpacing,
                bottom = ProtonDimens.MediumSpacing
            )
            .sizeIn(
                minWidth = MailDimens.ContactActionSize,
                minHeight = MailDimens.ContactActionSize
            )
            .background(
                color = if (isEnabled) ProtonTheme.colors.interactionWeakNorm
                else ProtonTheme.colors.interactionWeakDisabled,
                shape = RoundedCornerShape(MailDimens.ContactActionCornerRadius)
            )
            .clip(shape = RoundedCornerShape(MailDimens.ContactActionCornerRadius))
            .clickable(
                enabled = isEnabled,
                role = Role.Button,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = modifier.padding(start = ProtonDimens.DefaultSpacing),
            painter = painterResource(id = R.drawable.ic_proton_pen_square),
            tint = if (isEnabled) ProtonTheme.colors.iconNorm else ProtonTheme.colors.iconDisabled,
            contentDescription = NO_CONTENT_DESCRIPTION
        )
        Text(
            modifier = Modifier.padding(
                start = ProtonDimens.SmallSpacing,
                end = ProtonDimens.DefaultSpacing
            ),
            text = stringResource(R.string.send_group_message),
            style = ProtonTheme.typography.defaultSmallNorm
        )
    }
}

@Composable
fun ContactGroupMemberItem(modifier: Modifier = Modifier, contactGroupMember: ContactGroupDetailsMember) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = ProtonDimens.DefaultSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .sizeIn(
                    minWidth = MailDimens.AvatarMinSize,
                    minHeight = MailDimens.AvatarMinSize
                )
                .background(
                    color = ProtonTheme.colors.interactionWeakNorm,
                    shape = ProtonTheme.shapes.medium
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                textAlign = TextAlign.Center,
                text = contactGroupMember.initials
            )
        }
        Column(
            modifier = Modifier.padding(
                start = ProtonDimens.ListItemTextStartPadding,
                top = ProtonDimens.ListItemTextStartPadding,
                bottom = ProtonDimens.ListItemTextStartPadding,
                end = ProtonDimens.DefaultSpacing
            )
        ) {
            Text(
                text = contactGroupMember.name,
                style = ProtonTheme.typography.defaultNorm
            )
            Text(
                text = contactGroupMember.email,
                style = ProtonTheme.typography.defaultSmallWeak
            )
        }
    }
}

@Composable
fun ContactGroupDetailsTopBar(
    state: ContactGroupDetailsState,
    actions: ContactGroupDetailsScreen.Actions,
    onDeleteClick: () -> Unit
) {
    ProtonTopAppBar(
        modifier = Modifier.fillMaxWidth(),
        title = { },
        navigationIcon = {
            IconButton(onClick = actions.onBackClick) {
                Icon(
                    tint = ProtonTheme.colors.iconNorm,
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.presentation_back)
                )
            }
        },
        actions = {
            if (state is ContactGroupDetailsState.Data) {
                IconButton(onClick = { actions.onEditClick(state.contactGroup.id) }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_proton_pen),
                        tint = ProtonTheme.colors.iconNorm,
                        contentDescription = stringResource(R.string.edit_contact_group_content_description)
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_proton_trash),
                        tint = ProtonTheme.colors.iconNorm,
                        contentDescription = stringResource(R.string.delete_contact_group_content_description)
                    )
                }
            }
        }
    )
}

object ContactGroupDetailsScreen {

    const val ContactGroupDetailsLabelIdKey = "contact_group_details_label_id"

    data class Actions(
        val onBackClick: () -> Unit,
        val exitWithErrorMessage: (String) -> Unit,
        val exitWithNormMessage: (String) -> Unit,
        val showErrorMessage: (String) -> Unit,
        val onEditClick: (LabelId) -> Unit,
        val navigateToComposer: (List<String>) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBackClick = {},
                exitWithErrorMessage = {},
                exitWithNormMessage = {},
                showErrorMessage = {},
                onEditClick = {},
                navigateToComposer = {}
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ContactGroupDetailsContentPreview() {
    ContactGroupDetailsContent(
        state = ContactGroupDetailsState.Data(
            isSendEnabled = true,
            contactGroup = contactGroupDetailsSampleData,
            deleteDialogState = DeleteDialogState.Hidden
        ),
        onSendClick = {}
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EmptyContactGroupDetailsContentPreview() {
    ContactGroupDetailsContent(
        state = ContactGroupDetailsState.Data(
            isSendEnabled = true,
            contactGroup = contactGroupDetailsSampleData.copy(
                memberCount = 0,
                members = emptyList()
            ),
            deleteDialogState = DeleteDialogState.Hidden
        ),
        onSendClick = {}
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ContactDetailsTopBarPreview() {
    ContactGroupDetailsTopBar(
        state = ContactGroupDetailsState.Data(
            isSendEnabled = true,
            contactGroup = contactGroupDetailsSampleData,
            deleteDialogState = DeleteDialogState.Hidden
        ),
        actions = ContactGroupDetailsScreen.Actions.Empty,
        onDeleteClick = {}
    )
}
