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

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.usecase.IsPaidUser
import ch.protonmail.android.mailcommon.domain.usecase.ObserveUserAddresses
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.UserAddress
import javax.inject.Inject

class ValidateSenderAddress @Inject constructor(
    private val observeUserAddresses: ObserveUserAddresses,
    private val isPaidUser: IsPaidUser
) {

    suspend operator fun invoke(userId: UserId, senderEmail: SenderEmail): Either<ValidationFailure, ValidationResult> {
        val userAddresses = observeUserAddresses(userId).firstOrNull()
            ?: return ValidationFailure.CouldNotValidate.left()

        val addressToValidate = userAddresses
            .firstOrNull { it.email == senderEmail.value }
            ?: return ValidationFailure.CouldNotValidate.left()

        val validAddress = userAddresses.filter { it.enabled }.minByOrNull { it.order }
            ?: return ValidationFailure.AllAddressesDisabled.left()

        return when {
            !addressToValidate.enabled -> ValidationResult.Invalid(
                SenderEmail(validAddress.email), SenderEmail(addressToValidate.email), ValidationError.DisabledAddress
            )

            isFreeUserUsingPmMeAddress(userId, addressToValidate) -> ValidationResult.Invalid(
                SenderEmail(validAddress.email), SenderEmail(addressToValidate.email), ValidationError.PaidAddress
            )

            else -> ValidationResult.Valid(SenderEmail(addressToValidate.email))
        }.right()
    }

    private suspend fun isFreeUserUsingPmMeAddress(userId: UserId, address: UserAddress) =
        !isPaidUser(userId).getOrElse { false } && address.email.endsWith(PmMeDomain, true)

    sealed interface ValidationFailure {
        object CouldNotValidate : ValidationFailure
        object AllAddressesDisabled : ValidationFailure
    }

    sealed interface ValidationResult {
        val validAddress: SenderEmail

        data class Valid(override val validAddress: SenderEmail) : ValidationResult

        data class Invalid(
            override val validAddress: SenderEmail,
            val invalid: SenderEmail,
            val reason: ValidationError
        ) : ValidationResult

    }

    enum class ValidationError {
        DisabledAddress,
        PaidAddress,
        GenericError
    }

    companion object {
        private const val PmMeDomain = "@pm.me"
    }
}

fun ValidateSenderAddress.ValidationResult.isInvalidDueToPaidAddress() = when (this) {
    is ValidateSenderAddress.ValidationResult.Invalid ->
        this.reason == ValidateSenderAddress.ValidationError.PaidAddress
    is ValidateSenderAddress.ValidationResult.Valid -> false
}

fun ValidateSenderAddress.ValidationResult.isInvalidDueToDisabledAddress() = when (this) {
    is ValidateSenderAddress.ValidationResult.Invalid ->
        this.reason == ValidateSenderAddress.ValidationError.DisabledAddress
    is ValidateSenderAddress.ValidationResult.Valid -> false
}
