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

import androidx.core.text.HtmlCompat
import arrow.core.Either
import arrow.core.continuations.either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.AddressSignature
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class GetAddressSignature @Inject constructor(
    private val resolveUserAddress: ResolveUserAddress
) {

    suspend operator fun invoke(userId: UserId, email: SenderEmail): Either<DataError, AddressSignature> = either {

        val address = resolveUserAddress(userId, email)
            .mapLeft { DataError.AddressNotFound }
            .bind()

        val htmlSignature = address.signature ?: "" // backend falls back to empty string in case of a null

        val plaintextSignature = HtmlCompat.fromHtml(htmlSignature, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()

        return@either AddressSignature(htmlSignature, plaintextSignature.trimEnd())
    }

}
