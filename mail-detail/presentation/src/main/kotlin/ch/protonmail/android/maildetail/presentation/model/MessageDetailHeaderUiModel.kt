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

package ch.protonmail.android.maildetail.presentation.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Stable
import ch.protonmail.android.mailcommon.presentation.model.AvatarImageUiModel
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.presentation.model.LabelUiModel
import ch.protonmail.android.mailmessage.domain.model.MessageTheme
import kotlinx.collections.immutable.ImmutableList

@Stable
data class MessageDetailHeaderUiModel(
    val avatar: AvatarUiModel,
    val avatarImage: AvatarImageUiModel,
    val sender: ParticipantUiModel,
    val subject: String,
    val shouldShowTrackerProtectionIcon: Boolean,
    val shouldShowAttachmentIcon: Boolean,
    val shouldShowStar: Boolean,
    val location: MessageLocationUiModel,
    val category: MessageLocationUiModel?,
    val time: TextUiModel,
    val extendedTime: TextUiModel,
    val shouldShowUndisclosedRecipients: Boolean,
    val allRecipients: ImmutableList<ParticipantUiModel>,
    val toRecipients: ImmutableList<ParticipantUiModel>,
    val ccRecipients: ImmutableList<ParticipantUiModel>,
    val bccRecipients: ImmutableList<ParticipantUiModel>,
    val labels: ImmutableList<LabelUiModel>,
    val size: String,
    @DrawableRes val encryptionPadlock: Int,
    val encryptionInfo: String,
    val messageIdUiModel: MessageIdUiModel,
    val themeOverride: MessageTheme?,
    val shouldShowQuickReply: Boolean,
    val hasMoreActions: Boolean
)
