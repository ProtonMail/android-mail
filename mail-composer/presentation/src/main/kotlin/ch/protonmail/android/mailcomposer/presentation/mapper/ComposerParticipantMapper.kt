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

package ch.protonmail.android.mailcomposer.presentation.mapper

import java.util.concurrent.ConcurrentHashMap
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailcontact.domain.usecase.SearchContacts
import ch.protonmail.android.mailcontact.domain.usecase.SearchDeviceContacts
import ch.protonmail.android.mailmessage.domain.model.Participant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.equalsNoCase
import me.proton.core.util.kotlin.takeIfNotBlank
import timber.log.Timber
import javax.inject.Inject

class ComposerParticipantMapper @Inject constructor(
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val searchContacts: SearchContacts,
    private val searchDeviceContacts: SearchDeviceContacts
) {

    private val cache = ConcurrentHashMap<String, Participant>()

    suspend fun recipientUiModelToParticipant(recipient: RecipientUiModel.Valid): Participant {
        val userId = observePrimaryUserId().firstOrNull() ?: return Participant(
            recipient.address,
            recipient.address,
            false,
            null
        )

        return cache.getOrPut(recipient.address) {
            resolveRecipient(userId, recipient)
        }
    }

    private suspend fun resolveRecipient(userId: UserId, recipient: RecipientUiModel.Valid): Participant {
        Timber.tag("ParticipantMapper").d("Resolving recipient ${recipient.hashCode()}")
        val protonContactQueryResult = searchContacts.invoke(userId, recipient.address).first().getOrNull()

        val contactEmail = protonContactQueryResult?.firstNotNullOfOrNull { contact ->
            contact.contactEmails.find {
                // match by email and fallback to canonical version (fallback only makes sense if we actually
                // get canonical version of inputted address from API, it doesn't happen yet)
                recipient.address.equalsNoCase(it.email) || recipient.address.equalsNoCase(it.canonicalEmail)
            }
        }

        if (contactEmail != null) {
            return Participant(
                recipient.address,
                contactEmail.name.takeIfNotBlank() ?: recipient.address,
                contactEmail.isProton == true,
                null
            )
        }

        val deviceContact = searchDeviceContacts.invoke(recipient.address).getOrNull()?.find { contact ->
            contact.email == recipient.address
        }

        return Participant(
            recipient.address,
            deviceContact?.name?.takeIfNotBlank() ?: recipient.address,
            false,
            null
        )
    }
}
