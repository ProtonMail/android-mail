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

package ch.protonmail.android.testdata.maildetail

import androidx.annotation.DrawableRes
import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.model.AvatarImageUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.sample.ParticipantAvatarSample
import ch.protonmail.android.maildetail.presentation.model.MessageDetailHeaderUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageLocationUiModel
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

object MessageDetailHeaderUiModelTestData {

    val recipientOne = ParticipantUiModel(
        "Recipient1",
        "recipient1@pm.com",
        R.drawable.ic_proton_lock,
        shouldShowOfficialBadge = false
    )
    val recipientTwo = ParticipantUiModel(
        "Recipient2",
        "recipient2@pm.com",
        R.drawable.ic_proton_lock,
        shouldShowOfficialBadge = false
    )
    val recipientThree = ParticipantUiModel(
        "Recipient3",
        "recipient3@pm.com",
        R.drawable.ic_proton_lock,
        shouldShowOfficialBadge = false
    )

    val messageDetailHeaderUiModel = MessageDetailHeaderUiModel(
        avatar = ParticipantAvatarSample.ebay,
        avatarImage = AvatarImageUiModel.NoImageAvailable,
        sender = ParticipantUiModel(
            "Sender",
            "sender@pm.com",
            R.drawable.ic_proton_lock,
            shouldShowOfficialBadge = false
        ),
        shouldShowTrackerProtectionIcon = true,
        shouldShowAttachmentIcon = true,
        shouldShowStar = true,
        location = MessageLocationUiModel(TextUiModel.Text("Archive"), R.drawable.ic_proton_archive_box),
        category = null,
        time = TextUiModel.Text("08/11/2022"),
        extendedTime = TextUiModel.Text("08/11/2022, 17:16"),
        shouldShowUndisclosedRecipients = false,
        allRecipients = listOf(
            recipientOne,
            recipientTwo,
            recipientThree
        ).toImmutableList(),
        toRecipients = listOf(
            recipientOne,
            recipientTwo
        ).toImmutableList(),
        ccRecipients = listOf(
            recipientThree
        ).toImmutableList(),
        bccRecipients = emptyList<ParticipantUiModel>().toImmutableList(),
        labels = persistentListOf(),
        size = "12 MB",
        encryptionPadlock = R.drawable.ic_proton_lock,
        encryptionInfo = "End-to-end encrypted and signed message",
        messageIdUiModel = MessageIdUiModel("id"),
        themeOverride = null,
        shouldShowQuickReply = true,
        hasMoreActions = true,
        subject = "A subject"
    )

    fun buildParticipant(
        name: String,
        address: String,
        @DrawableRes padlock: Int = R.drawable.ic_proton_lock,
        showOfficialBadge: Boolean = false
    ) = ParticipantUiModel(
        name,
        address,
        padlock,
        shouldShowOfficialBadge = showOfficialBadge
    )
}
