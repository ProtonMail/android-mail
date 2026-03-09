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

package ch.protonmail.android.mailnotifications.data.usecase

import arrow.core.raise.either
import ch.protonmail.android.mailnotifications.data.model.DecryptionError
import ch.protonmail.android.mailnotifications.data.wrapper.DecryptedPushNotificationWrapper
import uniffi.mail_uniffi.DecryptPushNotificationResult
import uniffi.mail_uniffi.EncryptedPushNotification
import uniffi.mail_uniffi.OsKeyChain
import uniffi.mail_uniffi.decryptPushNotification
import javax.inject.Inject

internal class DecryptPushNotification @Inject constructor(
    private val keyChain: OsKeyChain
) {

    suspend operator fun invoke(encryptedPushNotification: EncryptedPushNotification) =
        either<DecryptionError, DecryptedPushNotificationWrapper> {
            when (val result = decryptPushNotification(keyChain, encryptedPushNotification)) {
                is DecryptPushNotificationResult.Error -> raise(DecryptionError.FailedToDecrypt)
                is DecryptPushNotificationResult.Ok -> DecryptedPushNotificationWrapper(result.v1)
            }
        }
}
