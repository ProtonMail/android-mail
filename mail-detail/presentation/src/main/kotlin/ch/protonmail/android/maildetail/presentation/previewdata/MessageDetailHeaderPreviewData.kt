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

import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageDetailHeaderUiModel
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel

object MessageDetailHeaderPreviewData {

    val MessageHeader = MessageDetailHeaderUiModel(
        avatar = AvatarUiModel.ParticipantInitial(value = "P"),
        sender = ParticipantUiModel(
            participantName = "Proton Test Account",
            participantAddress = "proton.test@protonmail.com",
            participantPadlock = R.drawable.ic_proton_lock
        ),
        shouldShowTrackerProtectionIcon = true,
        shouldShowAttachmentIcon = true,
        shouldShowStar = true,
        locationIcon = R.drawable.ic_proton_inbox,
        location = "Inbox",
        time = TextUiModel.Text("11:48"),
        extendedTime = "19-10-2022 at 11:48AM",
        allRecipients = "Recipient One, Recipient Two, Recipient Three, Recipient Four",
        toRecipients = listOf(
            ParticipantUiModel(
                participantName = "Recipient One",
                participantAddress = "recipient1@protonmail.com",
                participantPadlock = R.drawable.ic_proton_lock
            ),
            ParticipantUiModel(
                participantName = "Recipient Two",
                participantAddress = "recipient2@protonmail.com",
                participantPadlock = R.drawable.ic_proton_lock
            )
        ),
        ccRecipients = listOf(
            ParticipantUiModel(
                participantName = "Recipient Three",
                participantAddress = "recipient3@protonmail.com",
                participantPadlock = R.drawable.ic_proton_lock
            )
        ),
        bccRecipients = listOf(
            ParticipantUiModel(
                participantName = "Recipient Four",
                participantAddress = "recipient4@protonmail.com",
                participantPadlock = R.drawable.ic_proton_lock
            )
        ),
        labels = emptyList(),
        size = "6.35 KB",
        encryptionPadlock = R.drawable.ic_proton_lock,
        encryptionInfo = "End-to-end encrypted and signed message"
    )
}
