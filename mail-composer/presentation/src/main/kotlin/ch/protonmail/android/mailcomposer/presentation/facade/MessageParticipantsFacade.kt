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

package ch.protonmail.android.mailcomposer.presentation.facade

import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcomposer.domain.model.RecipientsBcc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsCc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsTo
import ch.protonmail.android.mailcomposer.domain.usecase.GetExternalRecipients
import ch.protonmail.android.mailcomposer.presentation.mapper.ComposerParticipantMapper
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailmessage.domain.model.Participant
import kotlinx.coroutines.flow.filterNotNull
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class MessageParticipantsFacade @Inject constructor(
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val participantMapper: ComposerParticipantMapper,
    private val getExternalRecipients: GetExternalRecipients
) {

    fun observePrimaryUserId() = observePrimaryUserId.invoke().filterNotNull()

    suspend fun mapToParticipant(recipient: RecipientUiModel.Valid): Participant =
        participantMapper.recipientUiModelToParticipant(recipient)

    suspend fun getExternalRecipients(
        userId: UserId,
        recipientsTo: RecipientsTo,
        recipientsCc: RecipientsCc,
        recipientsBcc: RecipientsBcc
    ) = getExternalRecipients.invoke(userId, recipientsTo, recipientsCc, recipientsBcc)
}
