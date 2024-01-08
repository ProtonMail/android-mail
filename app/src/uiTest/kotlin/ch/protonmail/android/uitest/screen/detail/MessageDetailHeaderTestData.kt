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

package ch.protonmail.android.uitest.screen.detail

import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageMetadataState
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.maildetail.presentation.previewdata.MessageDetailsPreviewData
import kotlinx.collections.immutable.toImmutableList

internal object MessageDetailHeaderTestData {

    val MessageWithOneRecipient = BaseMessage.copy(
        messageMetadataState = BaseMetadataState.copy(messageDetailHeader = HeaderWithOneRecipient)
    )

    val MessageWithMultipleRecipients = BaseMessage.copy(
        messageMetadataState = BaseMetadataState.copy(messageDetailHeader = HeaderWithMultipleRecipients)
    )
}

private val BaseParticipantUiModel = ParticipantUiModel(
    "one",
    "address1@proton.me",
    participantPadlock = R.drawable.ic_proton_lock,
    shouldShowOfficialBadge = false
)

private val BaseMessage = MessageDetailsPreviewData.Message
private val BaseMetadataState = BaseMessage.messageMetadataState as MessageMetadataState.Data
private val BaseMessageDetailHeader = BaseMetadataState.messageDetailHeader

private val HeaderWithOneRecipient = BaseMessageDetailHeader.copy(
    toRecipients = listOf(BaseParticipantUiModel).toImmutableList(),
    ccRecipients = emptyList<ParticipantUiModel>().toImmutableList(),
    bccRecipients = emptyList<ParticipantUiModel>().toImmutableList()
)

private val HeaderWithMultipleRecipients = BaseMessageDetailHeader.copy(
    toRecipients = listOf(BaseParticipantUiModel).toImmutableList(),
    ccRecipients = listOf(BaseParticipantUiModel.copy(participantAddress = "address2@proton.me")).toImmutableList(),
    bccRecipients = emptyList<ParticipantUiModel>().toImmutableList()
)
