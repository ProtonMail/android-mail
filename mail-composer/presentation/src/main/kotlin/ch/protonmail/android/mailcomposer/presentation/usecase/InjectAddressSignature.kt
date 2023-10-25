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

package ch.protonmail.android.mailcomposer.presentation.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.AddressSignature
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.usecase.GetAddressSignature
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class InjectAddressSignature @Inject constructor(
    private val getAddressSignature: GetAddressSignature
) {

    suspend operator fun invoke(
        userId: UserId,
        draftBody: DraftBody,
        senderEmail: SenderEmail,
        previousSenderEmail: SenderEmail? = null
    ): Either<DataError, DraftBody> = either {

        val addressSignature = getAddressSignature(userId, senderEmail).getOrElse {
            Timber.e("InjectAddressSignature: error getting address signature: $it")
            AddressSignature.BlankSignature
        }

        previousSenderEmail?.let { senderEmail ->
            getAddressSignature(userId, senderEmail).fold(
                ifLeft = { Timber.e("Error getting previous address signature: $senderEmail") },
                ifRight = { previousAddressSignature ->
                    getBodyWithReplacedSignature(draftBody, previousAddressSignature, addressSignature)?.let {
                        return@either it
                    }
                }
            )
        }

        val draftBodyWithAddressSignature = if (addressSignature.plaintext.isNotBlank()) {
            DraftBody("${draftBody.value}${AddressSignature.SeparatorPlaintext}${addressSignature.plaintext}")
        } else draftBody

        return@either draftBodyWithAddressSignature
    }

    private fun getBodyWithReplacedSignature(
        draftBody: DraftBody,
        previousAddressSignature: AddressSignature,
        addressSignature: AddressSignature
    ): DraftBody? {
        return draftBody.value.lastIndexOf(previousAddressSignature.plaintext).takeIf { it != -1 }?.let { lastIndex ->
            val bodyStringBuilder: StringBuilder = StringBuilder(draftBody.value)
            bodyStringBuilder.replace(
                lastIndex,
                previousAddressSignature.plaintext.length + lastIndex,
                addressSignature.plaintext
            )
            DraftBody(bodyStringBuilder.toString())
        }
    }


}
