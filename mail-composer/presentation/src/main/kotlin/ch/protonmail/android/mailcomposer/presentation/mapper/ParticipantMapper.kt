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

import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailmessage.domain.model.Participant
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.util.kotlin.equalsNoCase
import me.proton.core.util.kotlin.takeIfNotBlank
import javax.inject.Inject

@Deprecated("Part of Composer V1, to be replaced with ComposerParticipantMapper")
class ParticipantMapper @Inject constructor() {

    fun recipientUiModelToParticipant(recipient: RecipientUiModel.Valid, contacts: List<Contact>): Participant {
        val contactEmail = contacts.firstNotNullOfOrNull { contact ->
            contact.contactEmails.find {
                // match by email and fallback to canonical version (fallback only makes sense if we actually
                // get canonical version of inputted address from API, it doesn't happen yet)
                recipient.address.equalsNoCase(it.email) || recipient.address.equalsNoCase(it.canonicalEmail)
            }
        }

        return Participant(
            recipient.address,
            contactEmail?.name?.takeIfNotBlank() ?: recipient.address,
            contactEmail?.isProton ?: false,
            null
        )
    }
}
