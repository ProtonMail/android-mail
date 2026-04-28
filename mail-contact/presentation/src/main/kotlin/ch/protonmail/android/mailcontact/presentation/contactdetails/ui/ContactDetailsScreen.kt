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

package ch.protonmail.android.mailcontact.presentation.contactdetails.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.component.ProtonCenteredProgress
import ch.protonmail.android.design.compose.component.ProtonModalBottomSheetLayout
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyLargeNorm
import ch.protonmail.android.design.compose.theme.bodyMediumWeak
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.extension.copyTextToClipboard
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.contactdetails.ContactDetailsViewModel
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.AvatarUiModel
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.ContactDetailsItemGroupUiModel
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.ContactDetailsItemType
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.ContactDetailsState
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.ContactDetailsUiModel
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.QuickActionType
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.QuickActionUiModel
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactDetailsPreviewData
import ch.protonmail.android.mailcontact.presentation.ui.ContactDetailsError
import ch.protonmail.android.mailcontact.presentation.ui.ContactDetailsTopBar
import ch.protonmail.android.mailcontact.presentation.ui.RedirectToWebBottomSheetContent
import ch.protonmail.android.uicomponents.BottomNavigationBarSpacer

@Composable
fun ContactDetailsScreen(
    actions: ContactDetailsScreen.Actions,
    viewModel: ContactDetailsViewModel = hiltViewModel<ContactDetailsViewModel>()
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

    ContactDetailsScreen(
        state = state,
        actions = actions
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactDetailsScreen(
    state: ContactDetailsState,
    actions: ContactDetailsScreen.Actions,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    var bottomSheetType by remember { mutableStateOf(BottomSheetType.PhoneNumbers) }

    ProtonModalBottomSheetLayout(
        showBottomSheet = showBottomSheet,
        sheetState = bottomSheetState,
        onDismissed = { showBottomSheet = false },
        dismissOnBack = true,
        sheetContent = {
            if (state is ContactDetailsState.Data) {
                when (bottomSheetType) {
                    BottomSheetType.PhoneNumbers -> {
                        PhoneNumbersBottomSheetContent(state)
                    }

                    BottomSheetType.RedirectToWeb -> {
                        RedirectToWebBottomSheetContent(
                            description = R.string.edit_contact_bottom_sheet_redirect_to_web_description,
                            buttonText = R.string.contact_bottom_sheet_redirect_to_web_button,
                            onConfirm = { launchBrowser(context, state.uiModel.remoteId) },
                            onDismiss = { showBottomSheet = false }
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier,
            containerColor = ProtonTheme.colors.backgroundInvertedNorm,
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
            topBar = {
                ContactDetailsTopBar(
                    shouldShowActions = state is ContactDetailsState.Data,
                    actions = ContactDetailsTopBar.Actions(
                        onBack = actions.onBack,
                        onEdit = {
                            bottomSheetType = BottomSheetType.RedirectToWeb
                            showBottomSheet = true
                        },
                        onDelete = actions.showFeatureMissingSnackbar
                    )
                )
            }
        ) {
            when (state) {
                is ContactDetailsState.Data -> ContactDetails(
                    uiModel = state.uiModel,
                    actions = actions,
                    onCallQuickAction = {
                        bottomSheetType = BottomSheetType.PhoneNumbers
                        showBottomSheet = true
                    },
                    modifier = Modifier.padding(it)
                )

                is ContactDetailsState.Error -> ContactDetailsError(
                    onBack = actions.onBack,
                    onShowErrorSnackbar = actions.onShowErrorSnackbar
                )

                is ContactDetailsState.Loading -> ProtonCenteredProgress(
                    modifier = Modifier.padding(it)
                )
            }
        }
    }
}

@Composable
private fun ContactDetails(
    uiModel: ContactDetailsUiModel,
    actions: ContactDetailsScreen.Actions,
    onCallQuickAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ProtonDimens.Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        when (val avatarUiModel = uiModel.avatarUiModel) {
            is AvatarUiModel.Initials -> InitialsContactAvatar(
                initials = avatarUiModel.value,
                color = avatarUiModel.color
            )

            is AvatarUiModel.Photo -> ImageContactAvatar(
                imageBitmap = avatarUiModel.bitmap
            )
        }

        Spacer(modifier = Modifier.size(ProtonDimens.Spacing.Large))
        Text(
            text = uiModel.headerUiModel.displayName,
            style = ProtonTheme.typography.titleLargeMedium
        )
        uiModel.headerUiModel.displayEmailAddress?.let {
            Spacer(modifier = Modifier.size(ProtonDimens.Spacing.Compact))
            Text(
                text = it,
                style = ProtonTheme.typography.bodyMediumWeak
            )
        }
        Spacer(modifier = Modifier.size(ProtonDimens.Spacing.Standard))

        ContactDetailsQuickActions(
            quickActionUiModels = uiModel.quickActionUiModels,
            actions = ContactDetailsScreen.QuickActions(
                onMessageContact = { uiModel.headerUiModel.displayEmailAddress?.let { actions.onMessageContact(it) } },
                onCallQuickAction = onCallQuickAction,
                showFeatureMissingSnackbar = actions.showFeatureMissingSnackbar
            )
        )

        uiModel.contactDetailsItemGroupUiModels.forEachIndexed { index, groupUiModel ->
            ContactDetailsItemGroup(itemGroupUiModel = groupUiModel, onMessageContact = actions.onMessageContact)
            if (index != uiModel.contactDetailsItemGroupUiModels.size - 1) {
                Spacer(modifier = Modifier.size(ProtonDimens.Spacing.Large))
            }
        }

        BottomNavigationBarSpacer()
    }
}

@Composable
private fun ContactDetailsQuickActions(
    quickActionUiModels: List<QuickActionUiModel>,
    actions: ContactDetailsScreen.QuickActions
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ProtonDimens.Spacing.Large)
            .background(color = ProtonTheme.colors.backgroundInvertedNorm)
    ) {
        quickActionUiModels.forEachIndexed { index, uiModel ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = ProtonTheme.colors.backgroundInvertedSecondary,
                        shape = ProtonTheme.shapes.extraLarge
                    )
                    .clip(ProtonTheme.shapes.extraLarge)
                    .clickable(
                        enabled = uiModel.isEnabled,
                        role = Role.Button,
                        onClick = {
                            when (uiModel.quickActionType) {
                                QuickActionType.Message -> actions.onMessageContact()
                                QuickActionType.Call -> actions.onCallQuickAction()
                                QuickActionType.Share -> actions.showFeatureMissingSnackbar()
                            }
                        }
                    )
                    .padding(ProtonDimens.Spacing.Large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = uiModel.icon),
                    tint = if (uiModel.isEnabled) ProtonTheme.colors.iconNorm else ProtonTheme.colors.iconDisabled,
                    contentDescription = NO_CONTENT_DESCRIPTION
                )
                Spacer(modifier = Modifier.size(ProtonDimens.Spacing.Standard))
                Text(
                    text = stringResource(id = uiModel.label),
                    style = ProtonTheme.typography.bodyMedium.copy(
                        color = if (uiModel.isEnabled) ProtonTheme.colors.textWeak else ProtonTheme.colors.textDisabled
                    )
                )
            }
            if (index != quickActionUiModels.size - 1) {
                Spacer(modifier = Modifier.size(ProtonDimens.Spacing.Standard))
            }
        }
    }
}

@Composable
private fun ContactDetailsItemGroup(
    itemGroupUiModel: ContactDetailsItemGroupUiModel,
    onMessageContact: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = ProtonTheme.colors.backgroundInvertedSecondary,
                shape = ProtonTheme.shapes.extraLarge
            )
            .clip(ProtonTheme.shapes.extraLarge)
    ) {
        itemGroupUiModel.contactDetailsItemUiModels.forEachIndexed { index, uiModel ->
            val context = LocalContext.current
            val contactItemLabel = uiModel.label.string()
            val contactItemValue = uiModel.value.string()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onLongClickLabel = stringResource(id = R.string.contact_details_action_copy_label),
                        onLongClick = { context.copyTextToClipboard(contactItemLabel, contactItemValue) },
                        onClickLabel = when (uiModel.contactDetailsItemType) {
                            ContactDetailsItemType.Email -> stringResource(
                                id = R.string.contact_details_action_message_label
                            )

                            ContactDetailsItemType.Phone -> stringResource(
                                id = R.string.contact_details_action_call_label
                            )

                            ContactDetailsItemType.Url -> stringResource(
                                id = R.string.contact_details_action_open_url_label
                            )

                            ContactDetailsItemType.Other -> null
                        },
                        onClick = {
                            when (uiModel.contactDetailsItemType) {
                                ContactDetailsItemType.Email -> onMessageContact(contactItemValue)
                                ContactDetailsItemType.Phone -> launchPhoneApp(context, contactItemValue)
                                ContactDetailsItemType.Url -> openUrl(context, contactItemValue)
                                ContactDetailsItemType.Other -> Unit
                            }
                        }
                    )
                    .padding(ProtonDimens.Spacing.Large)
            ) {
                Text(
                    text = uiModel.label.string(),
                    style = ProtonTheme.typography.bodyMedium.copy(
                        color = ProtonTheme.colors.textHint
                    )
                )
                Spacer(modifier = Modifier.size(ProtonDimens.Spacing.Small))
                Text(
                    text = uiModel.value.string(),
                    style = ProtonTheme.typography.bodyLarge.copy(
                        color = if (uiModel.contactDetailsItemType == ContactDetailsItemType.Other) {
                            ProtonTheme.colors.textNorm
                        } else {
                            ProtonTheme.colors.textAccent
                        }
                    )
                )
                if (uiModel.badges.isNotEmpty()) {
                    Spacer(modifier = Modifier.size(ProtonDimens.Spacing.Standard))
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.Top
                    ) {
                        ContactDetailsBadges(uiModel.badges)
                    }
                }
            }
            if (index != itemGroupUiModel.contactDetailsItemUiModels.size - 1) {
                MailDivider()
            }
        }
    }
}

@Composable
private fun PhoneNumbersBottomSheetContent(state: ContactDetailsState.Data) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        state.uiModel.contactDetailsItemGroupUiModels.findPhoneItems()?.forEach { uiModel ->
            val actionLabel = stringResource(id = R.string.contact_details_action_call_label)
            val phoneNumberType = uiModel.label.string()
            val phoneNumber = uiModel.value.string()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ProtonDimens.ListItemHeight)
                    .clickable(
                        onClick = { launchPhoneApp(context, phoneNumber) }
                    )
                    .padding(horizontal = ProtonDimens.Spacing.Large),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatPhoneText(
                        actionLabel = actionLabel,
                        phoneNumberType = phoneNumberType,
                        phoneNumber = phoneNumber
                    ),
                    style = ProtonTheme.typography.bodyLargeNorm
                )
            }
        }
    }
}

private fun launchBrowser(context: Context, remoteId: String?) {
    remoteId?.let {
        val uri = "https://mail.proton.me/inbox#edit-contact=$remoteId".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }
}

private fun launchPhoneApp(context: Context, phoneNumber: String) {
    val uri = "tel:$phoneNumber".toUri()
    val intent = Intent(Intent.ACTION_DIAL, uri)
    context.startActivity(intent)
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    context.startActivity(intent)
}

private fun List<ContactDetailsItemGroupUiModel>.findPhoneItems() = find { itemGroupUiModel ->
    itemGroupUiModel.contactDetailsItemUiModels.any {
        it.contactDetailsItemType == ContactDetailsItemType.Phone
    }
}?.contactDetailsItemUiModels

private fun formatPhoneText(
    actionLabel: String,
    phoneNumberType: String,
    phoneNumber: String
) = "$actionLabel \"$phoneNumberType\" $phoneNumber"

enum class BottomSheetType { PhoneNumbers, RedirectToWeb }

@Preview
@Composable
private fun ContactDetailsScreenPreview() {
    ContactDetailsScreen(
        state = ContactDetailsPreviewData.contactDetailsState,
        actions = ContactDetailsScreen.Actions(
            onBack = {},
            onShowErrorSnackbar = {},
            onMessageContact = {},
            showFeatureMissingSnackbar = {}
        )
    )
}

object ContactDetailsScreen {

    const val CONTACT_DETAILS_ID_KEY = "ContactDetailsIdKey"

    data class Actions(
        val onBack: () -> Unit,
        val onShowErrorSnackbar: (String) -> Unit,
        val onMessageContact: (String) -> Unit,
        val showFeatureMissingSnackbar: () -> Unit
    )

    data class QuickActions(
        val onMessageContact: () -> Unit,
        val onCallQuickAction: () -> Unit,
        val showFeatureMissingSnackbar: () -> Unit
    )
}
