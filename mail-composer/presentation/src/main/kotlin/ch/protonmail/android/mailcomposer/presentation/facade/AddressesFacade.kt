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

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.usecase.GetPrimaryAddress
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.usecase.GetComposerSenderAddresses
import ch.protonmail.android.mailcomposer.domain.usecase.ValidateSenderAddress
import ch.protonmail.android.mailcomposer.domain.usecase.ValidateSenderAddress.ValidationFailure
import ch.protonmail.android.mailcomposer.domain.usecase.ValidateSenderAddress.ValidationFailure.CouldNotValidate
import ch.protonmail.android.mailcomposer.domain.usecase.ValidateSenderAddress.ValidationResult
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.UserAddress
import javax.inject.Inject

class AddressesFacade @Inject constructor(
    private val getPrimaryAddress: GetPrimaryAddress,
    private val getComposerSenderAddresses: GetComposerSenderAddresses,
    private val validateSenderAddress: ValidateSenderAddress
) {

    suspend fun getPrimarySenderEmail(userId: UserId): Either<DataError, SenderEmail> = either {
        val address = getPrimaryAddress.invoke(userId).getOrElse {
            raise(DataError.Local.NoDataCached)
        }

        SenderEmail(address.email)
    }

    suspend fun getSenderAddresses(): Either<GetComposerSenderAddresses.Error, List<UserAddress>> =
        getComposerSenderAddresses.invoke()

    suspend fun validateSenderAddress(
        userId: UserId,
        senderEmail: SenderEmail
    ): Either<ValidationFailure, ValidationResult> {
        val validationResult = validateSenderAddress.invoke(userId, senderEmail).getOrNull()
        if (validationResult != null) return validationResult.right()

        val fallbackAddress = getPrimaryAddress.invoke(userId)
            .getOrNull()
            ?.let { SenderEmail(it.email) }

        return if (fallbackAddress == null) {
            CouldNotValidate.left()
        } else {
            ValidationResult.Invalid(
                validAddress = fallbackAddress,
                invalid = senderEmail,
                reason = ValidateSenderAddress.ValidationError.GenericError
            ).right()
        }
    }
}
