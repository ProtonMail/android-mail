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

package ch.protonmail.android.maildetail.presentation.previewdata

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageDetailHeaderUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageLocationUiModel
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.maillabel.presentation.sample.LabelUiModelSample
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

object MessageDetailHeaderPreviewData {

    val WithoutLabels = MessageDetailHeaderUiModel(
        avatar = AvatarUiModel.ParticipantInitial(value = "P"),
        sender = ParticipantUiModel(
            participantName = "Proton Test Account",
            participantAddress = "proton.test@protonmail.com",
            participantPadlock = R.drawable.ic_proton_lock,
            shouldShowOfficialBadge = false
        ),
        shouldShowTrackerProtectionIcon = true,
        shouldShowAttachmentIcon = true,
        shouldShowStar = true,
        location = MessageLocationUiModel("Inbox", R.drawable.ic_proton_inbox),
        time = TextUiModel.Text("11:48"),
        extendedTime = TextUiModel.Text("19-10-2022 at 11:48AM"),
        shouldShowUndisclosedRecipients = false,
        allRecipients = TextUiModel.Text("Recipient One, Recipient Two, Recipient Three, Recipient Four"),
        toRecipients = listOf(
            ParticipantUiModel(
                participantName = "Recipient One",
                participantAddress = "recipient1@protonmail.com",
                participantPadlock = R.drawable.ic_proton_lock,
                shouldShowOfficialBadge = false
            ),
            ParticipantUiModel(
                participantName = "Recipient Two",
                participantAddress = "recipient2@protonmail.com",
                participantPadlock = R.drawable.ic_proton_lock,
                shouldShowOfficialBadge = false
            )
        ).toImmutableList(),
        ccRecipients = listOf(
            ParticipantUiModel(
                participantName = "Recipient Three",
                participantAddress = "recipient3@protonmail.com",
                participantPadlock = R.drawable.ic_proton_lock,
                shouldShowOfficialBadge = false
            )
        ).toImmutableList(),
        bccRecipients = listOf(
            ParticipantUiModel(
                participantName = "Recipient Four",
                participantAddress = "recipient4@protonmail.com",
                participantPadlock = R.drawable.ic_proton_lock,
                shouldShowOfficialBadge = false
            )
        ).toImmutableList(),
        labels = persistentListOf(),
        size = "6.35 KB",
        encryptionPadlock = R.drawable.ic_proton_lock,
        encryptionInfo = "End-to-end encrypted and signed message",
        messageIdUiModel = MessageIdUiModel("string")
    )

    val WithLabels = WithoutLabels.copy(
        labels = persistentListOf(
            LabelUiModelSample.Document,
            LabelUiModelSample.News
        )
    )
}

data class MessageDetailHeaderPreview(
    val initiallyExpanded: Boolean,
    val uiModel: MessageDetailHeaderUiModel
)

class MessageDetailHeaderPreviewProvider : PreviewParameterProvider<MessageDetailHeaderPreview> {

    override val values = sequenceOf(
        MessageDetailHeaderPreview(
            initiallyExpanded = false,
            uiModel = MessageDetailHeaderPreviewData.WithoutLabels
        ),
        MessageDetailHeaderPreview(
            initiallyExpanded = true,
            uiModel = MessageDetailHeaderPreviewData.WithoutLabels
        ),
        MessageDetailHeaderPreview(
            initiallyExpanded = false,
            uiModel = MessageDetailHeaderPreviewData.WithLabels
        ),
        MessageDetailHeaderPreview(
            initiallyExpanded = true,
            uiModel = MessageDetailHeaderPreviewData.WithLabels
        )
    )
}
