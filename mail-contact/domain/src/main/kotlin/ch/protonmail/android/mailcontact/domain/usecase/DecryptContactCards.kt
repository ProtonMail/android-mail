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

package ch.protonmail.android.mailcontact.domain.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import ch.protonmail.android.mailcontact.domain.decryptContactCardTrailingSpacesFallback
import ch.protonmail.android.mailcontact.domain.model.GetContactError
import me.proton.core.contact.domain.entity.ContactWithCards
import me.proton.core.contact.domain.entity.DecryptedVCard
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.domain.entity.UserId
import me.proton.core.key.domain.useKeys
import me.proton.core.user.domain.UserManager
import timber.log.Timber
import javax.inject.Inject

/**
 * Decrypts and verifies ContactVCards.
 */
class DecryptContactCards @Inject constructor(
    private val userManager: UserManager,
    private val cryptoContext: CryptoContext
) {

    suspend operator fun invoke(
        userId: UserId,
        contactWithCards: ContactWithCards
    ): Either<GetContactError, List<DecryptedVCard>> = either {
        val user = userManager.getUser(userId)
        return user.useKeys(cryptoContext) {
            contactWithCards.contactCards.map { card ->
                runCatching {
                    decryptContactCardTrailingSpacesFallback(card)
                }.getOrElse {
                    Timber.e("Exception decrypting Contact VCard", it)
                    raise(GetContactError)
                }
            }
        }.right()
    }
}
