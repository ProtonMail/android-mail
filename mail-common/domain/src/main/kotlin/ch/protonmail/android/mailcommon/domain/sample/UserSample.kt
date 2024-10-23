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

package ch.protonmail.android.mailcommon.domain.sample

import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.domain.entity.UserId
import me.proton.core.key.domain.entity.key.KeyId
import me.proton.core.key.domain.entity.key.PrivateKey
import me.proton.core.user.domain.entity.Delinquent
import me.proton.core.user.domain.entity.Role
import me.proton.core.user.domain.entity.Type
import me.proton.core.user.domain.entity.User
import me.proton.core.user.domain.entity.UserKey

object UserSample {

    val Primary = build()

    val UserWithKeys = build().copy(
        keys = listOf(
            UserKey(
                userId = UserIdSample.Primary,
                version = 1,
                keyId = KeyId("userKey"),
                privateKey = PrivateKey(
                    key = "private key armored",
                    isPrimary = true,
                    passphrase = EncryptedByteArray("private key passphrase".toByteArray())
                )
            )
        )
    )

    fun build(
        name: String = AccountSample.build().username ?: "name",
        displayName: String = name,
        email: String = AccountSample.build().email ?: "email",
        role: Role = Role.NoOrganization,
        userId: UserId = UserIdSample.Primary
    ) = User(
        type = Type.Proton,
        credit = 1,
        createdAtUtc = 0,
        currency = "CHF",
        delinquent = Delinquent.None,
        displayName = displayName,
        email = email,
        keys = emptyList(),
        flags = emptyMap(),
        maxSpace = 1_024,
        maxUpload = 1,
        name = name,
        private = true,
        role = role,
        services = 1,
        subscribed = 1,
        usedSpace = 512,
        userId = userId,
        recovery = null
    )
}
