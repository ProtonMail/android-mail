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

package ch.protonmail.android.mailcomposer.presentation.model

import androidx.compose.runtime.Stable
import ch.protonmail.android.mailmessage.domain.model.Participant
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Stable
data class RecipientsState(
    val toRecipients: ImmutableList<RecipientUiModel>,
    val ccRecipients: ImmutableList<RecipientUiModel>,
    val bccRecipients: ImmutableList<RecipientUiModel>
) {

    companion object {

        val Empty = RecipientsState(
            emptyList<RecipientUiModel>().toImmutableList(),
            emptyList<RecipientUiModel>().toImmutableList(),
            emptyList<RecipientUiModel>().toImmutableList()
        )
    }
}

suspend fun RecipientsState.toParticipantFields(
    action: suspend (recipient: RecipientUiModel.Valid) -> Participant
): Triple<List<Participant>, List<Participant>, List<Participant>> {
    return coroutineScope {
        val toParticipants = async {
            toRecipients.mapNotNull { it as? RecipientUiModel.Valid }
                .map { action(it) }
        }

        val ccParticipants = async {
            ccRecipients.mapNotNull { it as? RecipientUiModel.Valid }
                .map { action(it) }
        }

        val bccParticipants = async {
            bccRecipients.mapNotNull { it as? RecipientUiModel.Valid }
                .map { action(it) }
        }

        Triple(
            toParticipants.await(),
            ccParticipants.await(),
            bccParticipants.await()
        )
    }
}
