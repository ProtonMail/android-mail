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

package ch.protonmail.android.maildetail.presentation.sample

import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageDetailHeaderUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageLocationUiModel
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.maillabel.presentation.model.LabelUiModel
import kotlinx.collections.immutable.ImmutableList

object MessageDetailHeaderUiModelSample {

    @Suppress("LongParameterList")
    fun build(
        avatar: AvatarUiModel,
        sender: ParticipantUiModel,
        isStarred: Boolean,
        location: MessageLocationUiModel,
        time: TextUiModel,
        extendedTime: TextUiModel,
        allRecipients: TextUiModel,
        toRecipients: ImmutableList<ParticipantUiModel>,
        ccRecipients: ImmutableList<ParticipantUiModel>,
        bccRecipients: ImmutableList<ParticipantUiModel>,
        labels: ImmutableList<LabelUiModel>
    ): MessageDetailHeaderUiModel = MessageDetailHeaderUiModel(
        avatar = avatar,
        sender = sender,
        shouldShowTrackerProtectionIcon = false,
        shouldShowAttachmentIcon = false,
        shouldShowStar = isStarred,
        location = location,
        time = time,
        extendedTime = extendedTime,
        shouldShowUndisclosedRecipients = false,
        allRecipients = allRecipients,
        toRecipients = toRecipients,
        ccRecipients = ccRecipients,
        bccRecipients = bccRecipients,
        labels = labels,
        size = "6.35 KB",
        encryptionPadlock = 0,
        encryptionInfo = "",
        messageIdUiModel = MessageIdUiModel("id")
    )
}
