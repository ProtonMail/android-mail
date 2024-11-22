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

package ch.protonmail.android.mailnotifications.domain.usecase.content

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailnotifications.data.remote.resource.PushNotification
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.domain.entity.UserId
import me.proton.core.key.domain.decryptText
import me.proton.core.key.domain.entity.keyholder.KeyHolderContext
import me.proton.core.key.domain.useKeys
import me.proton.core.user.domain.UserManager
import me.proton.core.util.kotlin.deserialize
import timber.log.Timber
import javax.inject.Inject

internal class DecryptNotificationContent @Inject constructor(
    private val cryptoContext: CryptoContext,
    private val userManager: UserManager
) {

    suspend operator fun invoke(
        userId: UserId,
        notificationContent: String
    ): Either<DecryptionError, DecryptedNotification> = userManager.getUser(userId).useKeys(cryptoContext) {
        val result = tryDecrypt(notificationContent, userId)
        result
    }
        ?.right()
        ?: DecryptionError(notificationContent).left()

    private fun KeyHolderContext.tryDecrypt(notification: String, userId: UserId): DecryptedNotification? = try {
        decryptText(notification).run { DecryptedNotification(this.deserialize<PushNotification>()) }
    } catch (e: CryptoException) {
        Timber.e("Failed to decrypt notification for user id: %s - %s", e, userId.id)
        null
    }

    data class DecryptionError(val encryptedMessageBody: String)
    data class DecryptedNotification(val value: PushNotification)
}
