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

import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.Delinquent
import me.proton.core.user.domain.entity.Role
import me.proton.core.user.domain.entity.User

object UserSample {

    val Primary = build()

    fun build(
        name: String = AccountSample.build().username,
        displayName: String = name,
        email: String = AccountSample.build().email ?: "email",
        role: Role = Role.NoOrganization,
        userId: UserId = UserIdSample.Primary
    ) = User(
        credit = 1,
        currency = "CHF",
        delinquent = Delinquent.None,
        displayName = displayName,
        email = email,
        keys = emptyList(),
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
