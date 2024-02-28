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

package ch.protonmail.android.mailcomposer.domain.usecase

import ch.protonmail.android.mailcomposer.domain.model.RecipientsBcc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsCc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsTo
import ch.protonmail.android.mailmessage.domain.model.Recipient
import me.proton.core.domain.entity.UserId
import me.proton.core.key.domain.repository.PublicAddressRepository
import me.proton.core.key.domain.repository.getPublicAddressOrNull
import javax.inject.Inject

class GetExternalRecipients @Inject constructor(
    private val publicAddressRepository: PublicAddressRepository
) {

    suspend operator fun invoke(
        userId: UserId,
        recipientsTo: RecipientsTo,
        recipientsCc: RecipientsCc,
        recipientsBcc: RecipientsBcc
    ): List<Recipient> = (recipientsTo.value + recipientsCc.value + recipientsBcc.value).filter { recipient ->
        publicAddressRepository.getPublicAddressOrNull(userId, recipient.address)?.let { publicAddress ->
            publicAddress.recipient == me.proton.core.key.domain.entity.key.Recipient.External
        } ?: false
    }
}
