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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.component.ProtonCenteredProgress
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyLargeNorm
import ch.protonmail.android.design.compose.theme.bodyLargeWeak
import ch.protonmail.android.design.compose.theme.bodyMediumWeak
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailcontact.domain.model.ContactId
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.AvatarUiModel
import ch.protonmail.android.mailcontact.presentation.contactdetails.ui.IconContactAvatar
import ch.protonmail.android.mailcontact.presentation.contactdetails.ui.ImageContactAvatar
import ch.protonmail.android.mailcontact.presentation.contactdetails.ui.InitialsContactAvatar
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactGroupDetailsPreviewData
import ch.protonmail.android.mailcontact.presentation.ui.ContactDetailsError
import ch.protonmail.android.mailcontact.presentation.ui.ContactDetailsTopBar
import ch.protonmail.android.uicomponents.BottomNavigationBarSpacer

@Composable
fun ContactGroupDetailsScreen(
    actions: ContactGroupDetailsScreen.Actions,
    viewModel: ContactGroupDetailsViewModel = hiltViewModel<ContactGroupDetailsViewModel>()
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

    ContactGroupDetailsScreen(
        state = state,
        actions = actions
    )
}

@Composable
private fun ContactGroupDetailsScreen(
    state: ContactGroupDetailsState,
    actions: ContactGroupDetailsScreen.Actions,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        containerColor = ProtonTheme.colors.backgroundInvertedNorm,
        topBar = {
            ContactDetailsTopBar(
                shouldShowActions = state is ContactGroupDetailsState.Data,
                actions = ContactDetailsTopBar.Actions(
                    onBack = actions.onBack,
                    onEdit = actions.showFeatureMissingSnackbar,
                    onDelete = actions.showFeatureMissingSnackbar
                )
            )
        }
    ) {
        when (state) {
            is ContactGroupDetailsState.Data -> ContactGroupDetails(
                uiModel = state.uiModel,
                actions = actions,
                modifier = Modifier.padding(it)
            )
            is ContactGroupDetailsState.Error -> ContactDetailsError(
                onBack = actions.onBack,
                onShowErrorSnackbar = actions.onShowErrorSnackbar
            )
            is ContactGroupDetailsState.Loading -> ProtonCenteredProgress(
                modifier = Modifier.padding(it)
            )
        }
    }
}

@Composable
private fun ContactGroupDetails(
    uiModel: ContactGroupDetailsUiModel,
    actions: ContactGroupDetailsScreen.Actions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ProtonDimens.Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconContactAvatar(
            iconResId = R.drawable.ic_proton_users,
            backgroundColor = uiModel.color
        )
        Spacer(modifier = Modifier.size(ProtonDimens.Spacing.Large))
        Text(
            text = uiModel.name,
            style = ProtonTheme.typography.titleLargeMedium
        )
        Spacer(modifier = Modifier.size(ProtonDimens.Spacing.Standard))

        SendGroupMessageAction(
            memberCount = uiModel.memberCount,
            onClick = {
                actions.onSendGroupMessage(
                    uiModel.name,
                    uiModel.members.map { it.emailAddress }
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = ProtonTheme.colors.backgroundInvertedSecondary,
                    shape = ProtonTheme.shapes.extraLarge
                )
                .clip(ProtonTheme.shapes.extraLarge)
        ) {
            uiModel.members.forEachIndexed { index, memberUiModel ->
                ContactGroupMember(
                    uiModel = memberUiModel,
                    onOpenContact = actions.onOpenContact
                )
                if (index != uiModel.members.size - 1) {
                    MailDivider()
                }
            }
        }

        BottomNavigationBarSpacer()
    }
}

@Composable
private fun SendGroupMessageAction(
    memberCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = ProtonDimens.Spacing.Large)
            .background(
                color = ProtonTheme.colors.backgroundInvertedSecondary,
                shape = ProtonTheme.shapes.extraLarge
            )
            .clip(ProtonTheme.shapes.extraLarge)
            .clickable(
                enabled = memberCount > 0,
                role = Role.Button,
                onClick = onClick
            )
            .padding(
                horizontal = ProtonDimens.Spacing.Large,
                vertical = ProtonDimens.Spacing.Medium
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.padding(ProtonDimens.Spacing.Standard),
            painter = painterResource(id = R.drawable.ic_proton_pen_square),
            tint = ProtonTheme.colors.iconNorm,
            contentDescription = NO_CONTENT_DESCRIPTION
        )
        Spacer(modifier = Modifier.size(ProtonDimens.Spacing.Large))
        Column {
            Text(
                text = stringResource(id = R.string.contact_group_details_send_message_action),
                style = ProtonTheme.typography.bodyLargeWeak
            )
            Text(
                text = pluralStringResource(
                    id = R.plurals.contact_group_details_number_of_contacts,
                    count = memberCount,
                    memberCount
                ),
                style = ProtonTheme.typography.bodyMedium.copy(color = ProtonTheme.colors.textHint)
            )
        }
    }
}

@Composable
private fun ContactGroupMember(
    uiModel: ContactGroupMemberUiModel,
    onOpenContact: (ContactId) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClick = { onOpenContact(uiModel.id) }
            )
            .padding(
                horizontal = ProtonDimens.Spacing.Large,
                vertical = ProtonDimens.Spacing.Medium
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (val avatarUiModel = uiModel.avatarUiModel) {
            is AvatarUiModel.Initials -> InitialsContactAvatar(
                initials = avatarUiModel.value,
                color = avatarUiModel.color,
                isSmall = true
            )
            is AvatarUiModel.Photo -> ImageContactAvatar(
                imageBitmap = avatarUiModel.bitmap,
                isSmall = true
            )
        }

        Spacer(modifier = Modifier.size(ProtonDimens.Spacing.Large))

        Column {
            Text(
                text = uiModel.name,
                style = ProtonTheme.typography.bodyLargeNorm
            )
            Text(
                text = uiModel.emailAddress,
                style = ProtonTheme.typography.bodyMediumWeak
            )
        }
    }
}

@Preview
@Composable
fun ContactGroupDetailsScreenPreview() {
    ContactGroupDetailsScreen(
        state = ContactGroupDetailsPreviewData.contactGroupDetailsState,
        actions = ContactGroupDetailsScreen.Actions(
            onBack = {},
            onShowErrorSnackbar = {},
            onSendGroupMessage = { _, _ -> },
            onOpenContact = {},
            showFeatureMissingSnackbar = {}
        )
    )
}

object ContactGroupDetailsScreen {
    const val CONTACT_GROUP_DETAILS_ID_KEY = "ContactGroupDetailsIdKey"

    data class Actions(
        val onBack: () -> Unit,
        val onShowErrorSnackbar: (String) -> Unit,
        val onSendGroupMessage: (groupName: String, members: List<String>) -> Unit,
        val onOpenContact: (ContactId) -> Unit,
        val showFeatureMissingSnackbar: () -> Unit
    )
}
