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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.design.compose.component.ProtonCenteredProgress
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyLargeNorm
import ch.protonmail.android.design.compose.theme.titleLargeNorm
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.AvatarImageUiModel
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcontact.domain.model.ContactId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.Participant
import ch.protonmail.android.mailmessage.presentation.model.ContactActionUiModel
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.ContactActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.ui.ParticipantAvatar
import ch.protonmail.android.uicomponents.BottomNavigationBarSpacer
import kotlinx.collections.immutable.toImmutableList

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

    BottomNavigationBarSpacer()
}

@Composable
fun ContactActionsBottomSheetContent(
    dataState: ContactActionsBottomSheetState.Data,
    actions: ContactActionsBottomSheetContent.Actions
) {
    Column(
        modifier = Modifier
            .padding(ProtonDimens.Spacing.Large)
            .verticalScroll(rememberScrollState())
    ) {
        ContactActionsBottomSheetHeader(
            dataState.participant, dataState.avatarUiModel, dataState.avatarImageUiModel
        )

        ActionGroup(
            items = dataState.actions.firstGroup,
            onItemClicked = { action: ContactActionUiModel -> callbackForActions(action, actions, dataState.origin) }
        ) { item, onClick ->
            ActionGroupItem(
                icon = item.iconRes,
                description = stringResource(item.textRes),
                contentDescription = stringResource(item.descriptionRes),
                onClick = onClick
            )
        }

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))
        ActionGroup(
            items = dataState.actions.secondGroup,
            onItemClicked = { action: ContactActionUiModel -> callbackForActions(action, actions, dataState.origin) }
        ) { item, onClick ->
            ActionGroupItem(
                icon = item.iconRes,
                description = stringResource(item.textRes),
                contentDescription = stringResource(item.descriptionRes),
                onClick = onClick
            )
        }

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))
        ActionGroup(
            items = dataState.actions.thirdGroup,
            onItemClicked = { action: ContactActionUiModel -> callbackForActions(action, actions, dataState.origin) }
        ) { item, onClick ->
            ActionGroupItem(
                icon = item.iconRes,
                description = stringResource(item.textRes),
                contentDescription = stringResource(item.descriptionRes),
                onClick = onClick
            )
        }
    }
}

private fun ContactActionsBottomSheetState.Origin.getMessageId(): MessageId? = when (this) {
    is ContactActionsBottomSheetState.Origin.MessageDetails -> this.messageId
    is ContactActionsBottomSheetState.Origin.Unknown -> null
}

private fun callbackForActions(
    action: ContactActionUiModel,
    actions: ContactActionsBottomSheetContent.Actions,
    sheetOrigin: ContactActionsBottomSheetState.Origin
) = when (action) {
    is ContactActionUiModel.AddContactUiModel -> actions.onAddContactClicked(action.participant)
    is ContactActionUiModel.CopyAddress -> actions.onCopyAddressClicked(action.address)
    is ContactActionUiModel.CopyName -> actions.onCopyNameClicked(action.name)
    is ContactActionUiModel.NewMessage -> actions.onNewMessageClicked(action.participant)
    is ContactActionUiModel.BlockContact -> actions.onBlockClicked(
        action.participant,
        sheetOrigin.getMessageId(), action.contactId
    )

    is ContactActionUiModel.UnblockContact -> actions.onUnblockClicked(action.participant, sheetOrigin.getMessageId())
    is ContactActionUiModel.BlockAddress -> actions.onBlockClicked(action.participant, sheetOrigin.getMessageId(), null)
    is ContactActionUiModel.UnblockAddress -> actions.onUnblockClicked(action.participant, sheetOrigin.getMessageId())
}

@Composable
fun ContactActionsBottomSheetHeader(
    participant: Participant,
    avatarUiModel: AvatarUiModel?,
    avatarImageUiModel: AvatarImageUiModel = AvatarImageUiModel.NoImageAvailable
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (avatarUiModel != null) {
            ParticipantAvatar(
                modifier = Modifier
                    .testTag(ContactActionsBottomSheetTestTags.Avatar)
                    .padding(ProtonDimens.Spacing.Large)
                    .align(Alignment.CenterHorizontally),
                avatarUiModel = avatarUiModel,
                avatarImageUiModel = avatarImageUiModel,
                clickable = false,
                outerContainerSize = MailDimens.Contacts.AvatarSize,
                avatarSize = MailDimens.Contacts.AvatarSize,
                backgroundShape = CircleShape
            )
        }
        if (participant.name.isNotBlank()) {
            Text(
                modifier = Modifier
                    .padding(
                        start = ProtonDimens.Spacing.ExtraLarge, end = ProtonDimens.Spacing.ExtraLarge
                    )
                    .align(Alignment.CenterHorizontally),
                style = ProtonTheme.typography.titleLargeNorm,
                text = participant.name,
                textAlign = TextAlign.Center
            )
        }
        Text(
            modifier = Modifier
                .padding(
                    top = ProtonDimens.Spacing.Small,
                    bottom = ProtonDimens.Spacing.ExtraLarge,
                    start = ProtonDimens.Spacing.ExtraLarge,
                    end = ProtonDimens.Spacing.ExtraLarge
                )
                .align(Alignment.CenterHorizontally),
            style = ProtonTheme.typography.bodyLargeNorm,
            color = ProtonTheme.colors.textWeak,
            text = participant.address,
            textAlign = TextAlign.Center
        )
    }
}

object ContactActionsBottomSheetContent {

    data class Actions(
        val onCopyAddressClicked: (address: String) -> Unit,
        val onCopyNameClicked: (name: String) -> Unit,
        val onNewMessageClicked: (participant: Participant) -> Unit,
        val onAddContactClicked: (participant: Participant) -> Unit,
        val onBlockClicked: (participant: Participant, messageId: MessageId?, contactId: ContactId?) -> Unit,
        val onUnblockClicked: (participant: Participant, messageId: MessageId?) -> Unit
    )
}

@Preview(showBackground = true)
@Composable
fun ContactActionsBottomSheetContentPreview() {
    val participant = Participant(address = "participant@example.com", name = "Participant")
    ProtonTheme {
        ContactActionsBottomSheetContent(
            state = ContactActionsBottomSheetState.Data(
                participant = Participant(address = "test@protonmail.com", name = "Test User"),
                avatarUiModel = AvatarUiModel.ParticipantAvatar("TU", "test@protonmail.com", null),
                actions = ContactActionsBottomSheetState.ContactActionsGroups(
                    firstGroup = listOf(
                        ContactActionUiModel.CopyAddress(participant.address),
                        ContactActionUiModel.CopyName(participant.name)
                    ).toImmutableList(),
                    secondGroup = listOf(
                        ContactActionUiModel.AddContactUiModel(participant),
                        ContactActionUiModel.NewMessage(participant)
                    ).toImmutableList(),
                    thirdGroup = listOf(
                        ContactActionUiModel.BlockAddress(participant)
                    ).toImmutableList()
                ),
                origin = ContactActionsBottomSheetState.Origin.Unknown
            ),
            actions = ContactActionsBottomSheetContent.Actions(
                onCopyAddressClicked = {},
                onCopyNameClicked = {},
                onNewMessageClicked = {},
                onAddContactClicked = {},
                onBlockClicked = { _, _, _ -> },
                onUnblockClicked = { _, _ -> }
            )
        )
    }
}

object ContactActionsBottomSheetTestTags {

    const val Avatar = "Avatar"
}
