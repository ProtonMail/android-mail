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

package ch.protonmail.android.mailcommon.data.sample

import ch.protonmail.android.mailcommon.domain.sample.UserSample
import me.proton.core.user.data.entity.UserEntity
import me.proton.core.user.domain.entity.User

object UserEntitySample {

    val Primary = build(UserSample.Primary)

    fun build(user: User = UserSample.build()) = UserEntity(
        type = 0,
        credit = user.credit,
        createdAtUtc = user.createdAtUtc,
        currency = user.currency,
        delinquent = user.delinquent?.value,
        displayName = user.displayName,
        email = user.email,
        isPrivate = user.private,
        maxSpace = user.maxSpace,
        maxUpload = user.maxUpload,
        name = user.name,
        passphrase = null,
        role = user.role?.value,
        services = user.services,
        subscribed = user.subscribed,
        usedSpace = user.usedSpace,
        userId = user.userId,
        recovery = null,
        maxBaseSpace = null,
        maxDriveSpace = null,
        usedBaseSpace = null,
        usedDriveSpace = null,
        flags = null
    )
}
