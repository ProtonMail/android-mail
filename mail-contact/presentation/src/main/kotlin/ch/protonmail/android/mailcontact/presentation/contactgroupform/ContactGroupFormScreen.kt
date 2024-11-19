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

package ch.protonmail.android.mailcontact.presentation.contactgroupform

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupFormMember
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactGroupFormPreviewData
import ch.protonmail.android.mailcontact.presentation.ui.ColorPickerDialog
import ch.protonmail.android.mailcontact.presentation.ui.DeleteContactGroupDialog
import ch.protonmail.android.mailcontact.presentation.ui.FormInputField
import ch.protonmail.android.mailcontact.presentation.ui.IconContactAvatar
import ch.protonmail.android.maillabel.presentation.ui.FormDeleteButton
import ch.protonmail.android.uicomponents.dismissKeyboard
import ch.protonmail.android.uicomponents.snackbar.DismissableSnackbarHost
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonSecondaryButton
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultSmallNorm
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.contact.domain.entity.ContactEmailId

@Composable
fun ContactGroupFormScreen(
    actions: ContactGroupFormScreen.Actions,
    selectedContactEmailsIds: State<List<String>?>?,
    viewModel: ContactGroupFormViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostErrorState = remember { ProtonSnackbarHostState(defaultType = ProtonSnackbarType.ERROR) }
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val selectionSubmitted = remember { mutableStateOf(false) }

    if (!selectionSubmitted.value) {
        selectedContactEmailsIds?.value?.let {
            viewModel.submit(ContactGroupFormViewAction.OnUpdateMemberList(it))
            selectionSubmitted.value = true
        }
    }

    Scaffold(
        topBar = {
            ContactGroupFormTopBar(
                actions = ContactGroupFormTopBar.Actions(
                    onClose = {
                        viewModel.submit(ContactGroupFormViewAction.OnCloseClick)
                    },
                    onSave = {
                        viewModel.submit(ContactGroupFormViewAction.OnSaveClick)
                    }
                ),
                displaySaveLoader = state is ContactGroupFormState.Data && state.displaySaveLoader,
                isSaveEnabled = state is ContactGroupFormState.Data && state.isSaveEnabled
            )
        },
        content = { paddingValues ->
            when (state) {
                is ContactGroupFormState.Data -> {
                    ContactGroupFormContent(
                        modifier = Modifier.padding(paddingValues),
                        state = state,
                        actions = ContactGroupFormContent.Actions(
                            onAddMemberClick = {
                                actions.manageMembers(state.contactGroup.members.map { it.id })
                                selectionSubmitted.value = false
                            },
                            onRemoveMemberClick = {
                                viewModel.submit(ContactGroupFormViewAction.OnRemoveMemberClick(it))
                            },
                            onUpdateName = {
                                viewModel.submit(ContactGroupFormViewAction.OnUpdateName(it))
                            },
                            onUpdateColor = {
                                viewModel.submit(ContactGroupFormViewAction.OnUpdateColor(it))
                            },
                            onDeleteGroupClick = {
                                viewModel.submit(ContactGroupFormViewAction.OnDeleteClick)
                            }
                        )
                    )

                    ConsumableTextEffect(effect = state.closeWithSuccess) { message ->
                        actions.exitWithSuccessMessage(message)
                    }
                    ConsumableTextEffect(effect = state.showErrorSnackbar) { message ->
                        snackbarHostErrorState.showSnackbar(
                            message = message,
                            type = ProtonSnackbarType.ERROR
                        )
                    }
                    ConsumableTextEffect(effect = state.subscriptionNeededError) { message ->
                        snackbarHostErrorState.showSnackbar(
                            message = message,
                            type = ProtonSnackbarType.NORM
                        )
                    }
                    ConsumableTextEffect(effect = state.deletionSuccess) { message ->
                        actions.exitToContactsWithNormMessage(message)
                    }
                    ConsumableTextEffect(effect = state.deletionError) { message ->
                        actions.showErrorMessage(message)
                    }

                    DeleteContactGroupDialog(
                        state = state.deleteDialogState,
                        confirm = { viewModel.submit(ContactGroupFormViewAction.OnDeleteConfirmedClick) },
                        dismiss = { viewModel.submit(ContactGroupFormViewAction.OnDeleteDismissedClick) }
                    )
                }
                is ContactGroupFormState.Loading -> {
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
        actions.onClose()
    }
}

@Composable
fun ContactGroupFormContent(
    modifier: Modifier = Modifier,
    state: ContactGroupFormState.Data,
    actions: ContactGroupFormContent.Actions
) {

    Column {
        LazyColumn(
            modifier = modifier.weight(1f)
        ) {
            item {
                Column(modifier.fillMaxWidth()) {
                    IconContactAvatar(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(
                                top = ProtonDimens.DefaultSpacing,
                                bottom = ProtonDimens.ExtraSmallSpacing
                            ),
                        iconResId = R.drawable.ic_proton_users,
                        backgroundColor = state.contactGroup.color
                    )
                    val openDialog = remember { mutableStateOf(false) }
                    var selectedColor by remember { mutableStateOf(state.contactGroup.color) }
                    if (openDialog.value) {
                        ColorPickerDialog(
                            title = stringResource(R.string.contact_group_color),
                            selectedValue = selectedColor,
                            values = state.colors,
                            onDismissRequest = { openDialog.value = false },
                            onValueSelected = { selectedValue ->
                                openDialog.value = false
                                selectedColor = selectedValue
                                actions.onUpdateColor(selectedValue)
                            }
                        )
                    }
                    ProtonSecondaryButton(
                        modifier = modifier
                            .padding(
                                vertical = ProtonDimens.SmallSpacing,
                                horizontal = ProtonDimens.DefaultSpacing
                            )
                            .align(Alignment.CenterHorizontally),
                        onClick = { openDialog.value = true }
                    ) {
                        Text(
                            text = stringResource(R.string.change_color),
                            modifier = Modifier.padding(horizontal = ProtonDimens.SmallSpacing),
                            style = ProtonTheme.typography.defaultSmallNorm
                        )
                    }
                    FormInputField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(ProtonDimens.DefaultSpacing),
                        initialValue = state.contactGroup.name,
                        hint = stringResource(id = R.string.contact_group_form_name_hint),
                        onTextChange = actions.onUpdateName
                    )
                    Row(
                        modifier = Modifier.padding(
                            top = ProtonDimens.MediumSpacing,
                            start = ProtonDimens.DefaultSpacing,
                            end = ProtonDimens.DefaultSpacing
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            modifier = modifier
                                .size(ProtonDimens.SmallIconSize),
                            painter = painterResource(id = R.drawable.ic_proton_users),
                            tint = ProtonTheme.colors.iconWeak,
                            contentDescription = NO_CONTENT_DESCRIPTION
                        )
                        Text(
                            modifier = Modifier.padding(start = ProtonDimens.SmallSpacing),
                            style = ProtonTheme.typography.defaultSmallWeak,
                            text = pluralStringResource(
                                R.plurals.contact_group_form_member_count,
                                state.contactGroup.memberCount,
                                state.contactGroup.memberCount
                            )
                        )
                    }
                    Divider(
                        modifier = Modifier.padding(top = ProtonDimens.SmallSpacing),
                        color = ProtonTheme.colors.separatorNorm
                    )
                }
            }
            items(state.contactGroup.members) { member ->
                ContactGroupMemberItem(
                    contactGroupMember = member,
                    actions = actions
                )
            }
            item {
                ProtonSecondaryButton(
                    modifier = Modifier.padding(ProtonDimens.DefaultSpacing),
                    onClick = actions.onAddMemberClick
                ) {
                    Text(
                        text = stringResource(R.string.add_members),
                        modifier = Modifier.padding(horizontal = ProtonDimens.SmallSpacing),
                        style = ProtonTheme.typography.defaultSmallNorm
                    )
                }
            }
        }

        if (state.displayDeleteButton) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = ProtonDimens.ExtraSmallSpacing),
                contentAlignment = Alignment.Center
            ) {
                FormDeleteButton(
                    modifier = modifier,
                    text = stringResource(id = R.string.contact_group_delete_button_text),
                    onClick = { actions.onDeleteGroupClick() }
                )
            }
        }
    }
}

@Composable
fun ContactGroupMemberItem(
    modifier: Modifier = Modifier,
    contactGroupMember: ContactGroupFormMember,
    actions: ContactGroupFormContent.Actions
) {
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
            modifier = Modifier
                .weight(1f)
                .padding(
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
        IconButton(
            onClick = { actions.onRemoveMemberClick(contactGroupMember.id) }
        ) {
            Icon(
                tint = ProtonTheme.colors.iconWeak,
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(id = R.string.remove_member_content_description)
            )
        }
    }
}

@Composable
fun ContactGroupFormTopBar(
    actions: ContactGroupFormTopBar.Actions,
    displaySaveLoader: Boolean,
    isSaveEnabled: Boolean
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
            if (displaySaveLoader) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = ProtonDimens.DefaultSpacing)
                        .size(MailDimens.ProgressDefaultSize),
                    strokeWidth = MailDimens.ProgressStrokeWidth
                )
            } else {
                ProtonTextButton(
                    onClick = actions.onSave,
                    enabled = isSaveEnabled
                ) {
                    val textColor = if (isSaveEnabled) ProtonTheme.colors.textAccent
                    else ProtonTheme.colors.interactionDisabled
                    Text(
                        text = stringResource(id = R.string.contact_group_form_save),
                        color = textColor,
                        style = ProtonTheme.typography.defaultStrongNorm
                    )
                }
            }
        }
    )
}

object ContactGroupFormScreen {

    const val ContactGroupFormLabelIdKey = "contact_group_form_label_id"

    data class Actions(
        val onClose: () -> Unit,
        val exitWithErrorMessage: (String) -> Unit,
        val exitWithSuccessMessage: (String) -> Unit,
        val exitToContactsWithNormMessage: (String) -> Unit,
        val showErrorMessage: (String) -> Unit,
        val manageMembers: (List<ContactEmailId>) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onClose = {},
                exitWithErrorMessage = {},
                exitWithSuccessMessage = {},
                manageMembers = {},
                exitToContactsWithNormMessage = {},
                showErrorMessage = {}
            )
        }
    }
}

object ContactGroupFormContent {

    data class Actions(
        val onAddMemberClick: () -> Unit,
        val onRemoveMemberClick: (ContactEmailId) -> Unit,
        val onUpdateName: (String) -> Unit,
        val onUpdateColor: (Color) -> Unit,
        val onDeleteGroupClick: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onAddMemberClick = {},
                onRemoveMemberClick = {},
                onUpdateName = {},
                onUpdateColor = {},
                onDeleteGroupClick = {}
            )
        }
    }
}

object ContactGroupFormTopBar {
    data class Actions(
        val onClose: () -> Unit,
        val onSave: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onClose = {},
                onSave = {}
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ContactGroupFormContentPreview() {
    ContactGroupFormContent(
        state = ContactGroupFormState.Data(
            contactGroup = ContactGroupFormPreviewData.contactGroupFormSampleData,
            colors = emptyList()
        ),
        actions = ContactGroupFormContent.Actions.Empty
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EmptyContactGroupFormContentPreview() {
    ContactGroupFormContent(
        state = ContactGroupFormState.Data(
            contactGroup = ContactGroupFormPreviewData.contactGroupFormSampleData.copy(
                memberCount = 0,
                members = emptyList()
            ),
            colors = emptyList()
        ),
        actions = ContactGroupFormContent.Actions.Empty
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ContactFormTopBarPreview() {
    ContactGroupFormTopBar(
        actions = ContactGroupFormTopBar.Actions.Empty,
        displaySaveLoader = false,
        isSaveEnabled = true
    )
}
