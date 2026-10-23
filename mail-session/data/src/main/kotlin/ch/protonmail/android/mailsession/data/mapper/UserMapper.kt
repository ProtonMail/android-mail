/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailsession.data.mapper

import ch.protonmail.android.mailcommon.data.mapper.LocalUser
import ch.protonmail.android.mailsession.domain.model.User
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.Role

fun LocalUser.toUser(userId: UserId): User = User(
    userId = userId,
    displayName = displayName,
    email = email,
    name = name,
    services = services.toInt(),
    subscribed = subscribed.toInt(),
    delinquent = delinquent.toInt(),
    createTimeUtc = createTime.toLong(),
    isOrganizationMember = role is Role.Member,
    usedSpace = usedSpace,
    maxSpace = maxSpace
)
