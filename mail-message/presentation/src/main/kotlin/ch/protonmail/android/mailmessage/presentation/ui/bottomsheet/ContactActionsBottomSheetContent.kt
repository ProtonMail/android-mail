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

package ch.protonmail.android.mailmessage.presentation.ui.bottomsheet

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcommon.presentation.compose.Avatar
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailmessage.domain.model.Participant
import ch.protonmail.android.mailmessage.presentation.R
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.ContactActionsBottomSheetState
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonRawListItem
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.headlineNorm
import me.proton.core.contact.domain.entity.ContactId

@Composable
fun ContactActionsBottomSheetContent(
    state: ContactActionsBottomSheetState,
    actions: ContactActionsBottomSheetContent.Actions
) {
    when (state) {
        is ContactActionsBottomSheetState.Data -> ContactActionsBottomSheetContent(
            dataState = state, actions = actions
        )

        else -> ProtonCenteredProgress()
    }
}

@Composable
fun ContactActionsBottomSheetContent(
    dataState: ContactActionsBottomSheetState.Data,
    actions: ContactActionsBottomSheetContent.Actions
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        ContactActionsBottomSheetHeader(
            dataState.participant, dataState.avatarUiModel
        )
        ContactActionItem(
            iconRes = R.drawable.ic_proton_squares,
            textRes = R.string.contact_actions_copy_address,
            contentDescriptionRes = R.string.contact_actions_copy_address_description,
            onClick = { actions.onCopyAddressClicked(dataState.participant) }
        )
        ContactActionItem(
            iconRes = R.drawable.ic_proton_squares,
            textRes = R.string.contact_actions_copy_name,
            contentDescriptionRes = R.string.contact_actions_copy_name_description,
            onClick = { actions.onCopyNameClicked(dataState.participant) }
        )
        ContactActionItem(
            iconRes = R.drawable.ic_proton_pen_square,
            textRes = R.string.contact_actions_new_message,
            contentDescriptionRes = R.string.contact_actions_new_message_description,
            onClick = { actions.onNewMessageClicked(dataState.participant) }
        )
        dataState.contactId?.let {
            ContactActionItem(
                iconRes = R.drawable.ic_proton_user,
                textRes = R.string.contact_actions_view_contact_details,
                contentDescriptionRes = R.string.contact_actions_view_contact_details_description,
                onClick = { actions.onViewContactDetailsClicked(dataState.contactId) }
            )
        } ?: ContactActionItem(
            iconRes = R.drawable.ic_proton_user_plus,
            textRes = R.string.contact_actions_add_contact,
            contentDescriptionRes = R.string.contact_actions_add_contact_description,
            onClick = { actions.onAddContactClicked(dataState.participant) }
        )
    }
}

@Composable
fun ContactActionsBottomSheetHeader(participant: Participant, avatarUiModel: AvatarUiModel) {

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Avatar(
            modifier = Modifier
                .testTag(ContactActionsBottomSheetTestTags.Avatar)
                .padding(ProtonDimens.DefaultSpacing)
                .align(Alignment.CenterHorizontally),
            avatarUiModel = avatarUiModel,
            onClick = { },
            clickable = false,
            outerContainerSize = MailDimens.ContactActions.AvatarSize,
            avatarSize = MailDimens.ContactActions.AvatarSize,
            backgroundShape = CircleShape
        )
        if (participant.name.isNotBlank()) {
            Text(
                modifier = Modifier
                    .padding(
                        start = ProtonDimens.MediumSpacing, end = ProtonDimens.MediumSpacing
                    )
                    .align(Alignment.CenterHorizontally),
                style = ProtonTheme.typography.headlineNorm,
                text = participant.name,
                textAlign = TextAlign.Center
            )
        }
        Text(
            modifier = Modifier
                .padding(
                    top = ProtonDimens.ExtraSmallSpacing,
                    bottom = ProtonDimens.MediumSpacing,
                    start = ProtonDimens.MediumSpacing,
                    end = ProtonDimens.MediumSpacing
                )
                .align(Alignment.CenterHorizontally),
            style = ProtonTheme.typography.defaultNorm,
            color = ProtonTheme.colors.textWeak,
            text = participant.address,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ContactActionItem(
    @DrawableRes iconRes: Int,
    @StringRes textRes: Int,
    @StringRes contentDescriptionRes: Int,
    onClick: () -> Unit = {}
) {
    ProtonRawListItem(
        modifier = Modifier
            .testTag(ContactActionsBottomSheetTestTags.ContactActionsItem)
            .clickable { onClick() }
            .padding(vertical = ProtonDimens.DefaultSpacing)
    ) {
        Icon(
            painter = painterResource(iconRes),
            modifier = Modifier
                .testTag(ContactActionsBottomSheetTestTags.ActionIcon)
                .padding(horizontal = ProtonDimens.DefaultSpacing),
            contentDescription = stringResource(id = contentDescriptionRes)
        )
        Text(
            modifier = Modifier
                .testTag(ContactActionsBottomSheetTestTags.ActionText)
                .weight(1f),
            text = stringResource(id = textRes),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

object ContactActionsBottomSheetContent {

    data class Actions(
        val onCopyAddressClicked: (participant: Participant) -> Unit,
        val onCopyNameClicked: (participant: Participant) -> Unit,
        val onNewMessageClicked: (participant: Participant) -> Unit,
        val onAddContactClicked: (participant: Participant) -> Unit,
        val onViewContactDetailsClicked: (contactId: ContactId) -> Unit
    )
}

@Preview(showBackground = true)
@Composable
fun ContactActionsBottomSheetContentPreview() {
    ProtonTheme {
        ContactActionsBottomSheetContent(
            state = ContactActionsBottomSheetState.Data(
                participant = Participant(address = "test@protonmail.com", name = "Test User"),
                avatarUiModel = AvatarUiModel.ParticipantInitial("TU"),
                contactId = ContactId(id = "1")
            ),
            actions = ContactActionsBottomSheetContent.Actions(
                onCopyAddressClicked = {},
                onCopyNameClicked = {},
                onNewMessageClicked = {},
                onAddContactClicked = {},
                onViewContactDetailsClicked = {}
            )
        )
    }
}

object ContactActionsBottomSheetTestTags {

    const val Avatar = "Avatar"
    const val ContactActionsItem = "ContactActionsItem"
    const val ActionIcon = "ActionIcon"
    const val ActionText = "ActionText"
}
