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

package ch.protonmail.android.mailcontact.presentation.contactdetails

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.Avatar
import ch.protonmail.android.mailcontact.presentation.model.ContactDetailsGroupsItem
import ch.protonmail.android.mailcontact.presentation.model.ContactDetailsItem
import ch.protonmail.android.mailcontact.presentation.model.ContactDetailsItem.ContactDetailType.Triggerable
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactDetailsPreviewData.contactDetailsSampleData
import ch.protonmail.android.mailcontact.presentation.ui.ImageContactAvatar
import ch.protonmail.android.mailcontact.presentation.ui.InitialsContactAvatar
import ch.protonmail.android.mailcontact.presentation.utils.ContactFeatureFlags.ContactDelete
import ch.protonmail.android.mailcontact.presentation.utils.ContactFeatureFlags.ContactEdit
import ch.protonmail.android.mailcontact.presentation.utils.ContactFeatureFlags.ContactExport
import ch.protonmail.android.uicomponents.snackbar.DismissableSnackbarHost
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionWeak
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultSmallStrongUnspecified
import me.proton.core.compose.theme.defaultWeak
import me.proton.core.compose.theme.headlineNorm
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.util.kotlin.takeIfNotBlank

@Composable
fun ContactDetailsScreen(actions: ContactDetailsScreen.Actions, viewModel: ContactDetailsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostErrorState = ProtonSnackbarHostState(defaultType = ProtonSnackbarType.ERROR)
    val state = rememberAsState(flow = viewModel.state, initial = ContactDetailsViewModel.initialState).value
    val deleteDialogShownState = remember { mutableStateOf(false) }

    val customActions = actions.copy(
        onDeleteClick = { viewModel.submit(ContactDetailsViewAction.DeleteRequested) },
        onCallClick = { phoneNumber -> viewModel.submit(ContactDetailsViewAction.OnCallClick(phoneNumber)) },
        onEmailClick = { email -> viewModel.submit(ContactDetailsViewAction.OnEmailClick(email)) },
        onLongClick = { value -> viewModel.submit(ContactDetailsViewAction.OnLongClick(value)) }
    )

    Scaffold(
        topBar = {
            ContactDetailsTopBar(
                state = state,
                actions = customActions
            )
        },
        content = { paddingValues ->
            when (state) {
                is ContactDetailsState.Data -> {
                    ContactDetailsContent(
                        state = state,
                        actions = customActions,
                        modifier = Modifier.padding(paddingValues)
                    )

                    ConsumableTextEffect(effect = state.closeWithSuccess) { message ->
                        actions.exitWithSuccessMessage(message)
                    }
                    ConsumableLaunchedEffect(effect = state.callPhoneNumber) {
                        val callIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$it")
                        }
                        context.startActivity(callIntent)
                    }
                    ConsumableLaunchedEffect(effect = state.copyToClipboard) { stringValue ->
                        clipboardManager?.let {
                            it.setPrimaryClip(ClipData.newPlainText("", stringValue))
                            Toast.makeText(
                                context,
                                context.getString(R.string.action_copied_to_clipboard),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    }
                    ConsumableLaunchedEffect(effect = state.openComposer) { actions.navigateToComposer(it) }
                    ConsumableLaunchedEffect(effect = state.showDeleteConfirmDialog) {
                        deleteDialogShownState.value = true
                    }
                }

                is ContactDetailsState.Loading -> {
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

    ContactDetailsDeleteConfirmationDialog(
        deleteDialogShownState = deleteDialogShownState,
        onDeleteConfirmed = { viewModel.submit(ContactDetailsViewAction.DeleteConfirmed) }
    )

    ConsumableLaunchedEffect(effect = state.close) {
        dismissKeyboard(context, view, keyboardController)
        customActions.onBackClick()
    }
}

@Composable
fun ContactDetailsContent(
    state: ContactDetailsState.Data,
    actions: ContactDetailsScreen.Actions,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
    ) {
        item {
            Column(modifier.fillMaxWidth()) {
                when (state.contact.avatar) {
                    is Avatar.Initials -> {
                        InitialsContactAvatar(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = ProtonDimens.DefaultSpacing),
                            initials = state.contact.avatar.value
                        )
                    }

                    is Avatar.Photo -> {
                        ImageContactAvatar(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = ProtonDimens.DefaultSpacing),
                            imageBitmap = state.contact.avatar.bitmap.asImageBitmap()
                        )
                    }
                }
                Text(
                    modifier = Modifier
                        .padding(
                            top = ProtonDimens.MediumSpacing,
                            start = ProtonDimens.MediumSpacing,
                            end = ProtonDimens.MediumSpacing
                        )
                        .align(Alignment.CenterHorizontally),
                    style = ProtonTheme.typography.headlineNorm,
                    text = state.contact.nameHeader,
                    textAlign = TextAlign.Center
                )
                if (state.contact.nameSubText.isNotBlank()) {
                    Text(
                        modifier = Modifier
                            .padding(
                                top = ProtonDimens.ExtraSmallSpacing,
                                start = ProtonDimens.MediumSpacing,
                                end = ProtonDimens.MediumSpacing
                            )
                            .align(Alignment.CenterHorizontally),
                        style = ProtonTheme.typography.defaultNorm,
                        color = ProtonTheme.colors.textWeak,
                        text = state.contact.nameSubText,
                        textAlign = TextAlign.Center
                    )
                }
                ActionItemsRow(
                    modifier = Modifier
                        .padding(vertical = ProtonDimens.MediumSpacing)
                        .align(Alignment.CenterHorizontally),
                    actions = actions,
                    state = state
                )
            }
        }
        items(state.contact.contactMainDetailsItemList) { contactDetailsItem ->
            ContactDetailsItem(contactDetailsItem = contactDetailsItem, actions = actions)
        }
        items(state.contact.contactOtherDetailsItemList) { contactDetailsItem ->
            ContactDetailsItem(contactDetailsItem = contactDetailsItem, actions = actions)
        }
        item {
            if (state.contact.contactGroups.displayGroupSection) {
                ContactGroupRow(
                    modifier = Modifier.padding(ProtonDimens.DefaultSpacing),
                    contactGroups = state.contact.contactGroups
                )
            }
        }
    }
}

@Composable
private fun ActionItemsRow(
    modifier: Modifier = Modifier,
    actions: ContactDetailsScreen.Actions,
    state: ContactDetailsState.Data
) {
    Row(
        modifier = modifier
    ) {
        ContactDetailsActionItem(
            iconResId = R.drawable.ic_proton_phone,
            onClick = { actions.onCallClick(state.contact.defaultPhoneNumber) },
            isEnabled = state.contact.isCallActionEnabled()
        )
        ContactDetailsActionItem(
            modifier = Modifier.padding(start = ProtonDimens.DefaultSpacing),
            iconResId = R.drawable.ic_proton_pen_square,
            onClick = { actions.onEmailClick(state.contact.defaultEmail) },
            isEnabled = state.contact.isEmailActionEnabled()
        )
        if (ContactExport.value) {
            ContactDetailsActionItem(
                modifier = Modifier.padding(start = ProtonDimens.DefaultSpacing),
                iconResId = R.drawable.ic_proton_arrow_up_from_square,
                onClick = actions.showFeatureMissingSnackbar,
                isEnabled = false
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContactGroupRow(modifier: Modifier = Modifier, contactGroups: ContactDetailsGroupsItem) {
    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            modifier = Modifier.align(Alignment.Top),
            painter = painterResource(id = contactGroups.iconResId),
            tint = ProtonTheme.colors.iconNorm,
            contentDescription = stringResource(id = R.string.contact_groups_content_description)
        )
        FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(start = ProtonDimens.DefaultSpacing)
        ) {
            for (groupLabel in contactGroups.groupLabelList) {
                ContactDetailsGroupLabel(
                    value = groupLabel.name,
                    color = groupLabel.color
                )
            }
        }
    }
}

@Composable
private fun ContactDetailsGroupLabel(
    modifier: Modifier = Modifier,
    value: String,
    color: Color
) {
    Box(
        modifier = modifier
            .padding(
                end = ProtonDimens.ExtraSmallSpacing,
                bottom = ProtonDimens.ExtraSmallSpacing
            )
            .background(
                color = color,
                shape = RoundedCornerShape(MailDimens.ContactGroupLabelCornerRadius)
            )
    ) {
        Text(
            modifier = Modifier
                .padding(
                    horizontal = MailDimens.ContactGroupLabelPaddingHorizontal,
                    vertical = MailDimens.ContactGroupLabelPaddingVertical
                ),
            text = value,
            style = ProtonTheme.typography.defaultSmallStrongUnspecified,
            color = Color.White
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactDetailsItem(
    modifier: Modifier = Modifier,
    contactDetailsItem: ContactDetailsItem,
    actions: ContactDetailsScreen.Actions
) {
    val textToCopyToClipboard = if (contactDetailsItem is ContactDetailsItem.Text) {
        contactDetailsItem.value.string().trim().takeIfNotBlank()
    } else null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = contactDetailsItem is ContactDetailsItem.Text,
                onClick = {
                    if (contactDetailsItem is ContactDetailsItem.Text) {
                        if (contactDetailsItem.type is Triggerable) {
                            when (contactDetailsItem.type) {
                                is Triggerable.Phone -> actions.onCallClick(contactDetailsItem.type.phoneNumber)
                                is Triggerable.Email -> actions.onEmailClick(contactDetailsItem.type.email)
                            }
                        }
                    }
                },
                onLongClick = {
                    textToCopyToClipboard?.let {
                        actions.onLongClick(it)
                    }
                }
            )
            .padding(ProtonDimens.DefaultSpacing)
    ) {
        if (contactDetailsItem.displayIcon) {
            Icon(
                modifier = Modifier
                    .align(Alignment.Top)
                    .padding(top = ProtonDimens.SmallSpacing),
                painter = painterResource(id = contactDetailsItem.iconResId),
                tint = ProtonTheme.colors.iconNorm,
                contentDescription = contactDetailsItem.header.string()
            )
        }

        val columnPadding =
            if (contactDetailsItem.displayIcon) ProtonDimens.DefaultSpacing
            else ProtonDimens.LargerSpacing
        Column(modifier.padding(start = columnPadding)) {
            Text(
                text = contactDetailsItem.header.string(),
                style = ProtonTheme.typography.captionWeak
            )
            when (contactDetailsItem) {
                is ContactDetailsItem.Image -> {
                    Image(
                        modifier = modifier
                            .padding(top = ProtonDimens.SmallSpacing)
                            .sizeIn(
                                maxWidth = MailDimens.ContactAvatarSize,
                                maxHeight = MailDimens.ContactAvatarSize
                            ),
                        bitmap = contactDetailsItem.value.asImageBitmap(),
                        contentScale = ContentScale.Inside,
                        contentDescription = contactDetailsItem.header.string()
                    )
                }

                is ContactDetailsItem.Text -> {
                    Text(
                        text = contactDetailsItem.value.string(),
                        style = ProtonTheme.typography.defaultNorm
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactDetailsActionItem(
    modifier: Modifier = Modifier,
    iconResId: Int,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .sizeIn(
                minWidth = MailDimens.ContactActionSize,
                minHeight = MailDimens.ContactActionSize
            )
            .background(
                color = if (isEnabled)
                    ProtonTheme.colors.interactionWeakNorm
                else
                    ProtonTheme.colors.interactionWeakDisabled,
                shape = RoundedCornerShape(MailDimens.ContactActionCornerRadius)
            )
            .clip(
                shape = RoundedCornerShape(MailDimens.ContactActionCornerRadius)
            )
            .clickable(
                enabled = isEnabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            tint = if (isEnabled) ProtonTheme.colors.iconNorm else ProtonTheme.colors.iconDisabled,
            contentDescription = NO_CONTENT_DESCRIPTION
        )
    }
}

@Composable
fun ContactDetailsTopBar(state: ContactDetailsState, actions: ContactDetailsScreen.Actions) {
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
            if (state is ContactDetailsState.Data) {
                if (ContactEdit.value) {
                    IconButton(onClick = { actions.onEditClick(state.contact.id) }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_proton_pen),
                            tint = ProtonTheme.colors.iconNorm,
                            contentDescription = stringResource(R.string.edit_contact_content_description)
                        )
                    }
                }
                if (ContactDelete.value) {
                    IconButton(onClick = actions.onDeleteClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_proton_trash),
                            tint = ProtonTheme.colors.iconNorm,
                            contentDescription = stringResource(R.string.delete_contact_content_description)
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun ContactDetailsDeleteConfirmationDialog(
    onDeleteConfirmed: () -> Unit,
    deleteDialogShownState: MutableState<Boolean>
) {
    if (deleteDialogShownState.value) {
        ProtonAlertDialog(
            modifier = Modifier.testTag(ContactDetailsTestTags.DeleteDialog),
            titleResId = R.string.contact_details_delete_dialog_title,
            text = {
                Text(
                    text = stringResource(id = R.string.contact_details_delete_dialog_text),
                    style = ProtonTheme.typography.defaultWeak
                )
            },
            dismissButton = {
                ProtonAlertDialogButton(
                    titleResId = R.string.contact_details_delete_dialog_cancel_button,
                    modifier = Modifier.testTag(ContactDetailsTestTags.DeleteDialogCancelButton)
                ) {
                    deleteDialogShownState.value = false
                }
            },
            confirmButton = {
                ProtonAlertDialogButton(
                    titleResId = R.string.contact_details_delete_dialog_confirm_button,
                    modifier = Modifier.testTag(ContactDetailsTestTags.DeleteDialogConfirmButton)
                ) {
                    deleteDialogShownState.value = false
                    onDeleteConfirmed()
                }
            },
            onDismissRequest = { deleteDialogShownState.value = false }
        )
    }
}

object ContactDetailsScreen {

    const val ContactDetailsContactIdKey = "contact_details_contact_id"

    data class Actions(
        val onBackClick: () -> Unit,
        val exitWithSuccessMessage: (String) -> Unit,
        val exitWithErrorMessage: (String) -> Unit,
        val onEditClick: (ContactId) -> Unit,
        val onDeleteClick: () -> Unit,
        val onCallClick: (String) -> Unit,
        val onEmailClick: (String) -> Unit,
        val onLongClick: (String) -> Unit,
        val showFeatureMissingSnackbar: () -> Unit,
        val navigateToComposer: (String) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBackClick = {},
                exitWithSuccessMessage = {},
                exitWithErrorMessage = {},
                onEditClick = {},
                onDeleteClick = {},
                onCallClick = {},
                onEmailClick = {},
                onLongClick = {},
                showFeatureMissingSnackbar = {},
                navigateToComposer = {}
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ContactDetailsScreenPreview() {
    ContactDetailsContent(
        state = ContactDetailsState.Data(contact = contactDetailsSampleData),
        actions = ContactDetailsScreen.Actions.Empty
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ContactDetailsTopBarPreview() {
    ContactDetailsTopBar(
        state = ContactDetailsState.Data(contact = contactDetailsSampleData),
        actions = ContactDetailsScreen.Actions.Empty
    )
}

object ContactDetailsTestTags {

    const val DeleteDialog = "DeleteContactDialog"
    const val DeleteDialogCancelButton = "DeleteContactDialogCancelButton"
    const val DeleteDialogConfirmButton = "DeleteContactDialogConfirmButton"
}
