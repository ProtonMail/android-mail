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

package ch.protonmail.android.testdata.user

import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.Delinquent
import me.proton.core.user.domain.entity.Role
import me.proton.core.user.domain.entity.Type
import me.proton.core.user.domain.entity.User

object UserTestData {

    const val MAX_SPACE_RAW = 40_000L
    const val USED_SPACE_RAW = 10_000L
    const val MAX_BASE_SPACE_RAW = 20_000L
    const val USED_BASE_SPACE_RAW = 5000L
    const val USER_EMAIL_RAW = "userEmail"
    const val USER_DISPLAY_NAME_RAW = "userDisplayName"
    const val USER_NAME_RAW = "username"

    val Primary = build()

    val emptyDisplayNameUser = build(
        displayName = "",
        userId = UserIdTestData.userId
    )

    val adminUser = build(
        role = Role.OrganizationAdmin,
        userId = UserIdTestData.adminUserId
    )

    val orgMemberUser = build(
        role = Role.OrganizationMember,
        userId = UserIdTestData.adminUserId
    )

    val freeUser = build(subscribed = 0)

    val paidMailUser = build(subscribed = 1)

    val paidUser = build(subscribed = 2)

    fun build(
        displayName: String = USER_DISPLAY_NAME_RAW,
        name: String = USER_NAME_RAW,
        role: Role = Role.NoOrganization,
        userId: UserId = UserIdTestData.Primary,
        subscribed: Int = 1
    ) = User(
        createdAtUtc = 0,
        type = Type.Proton,
        credit = 1,
        currency = "CHF",
        delinquent = Delinquent.None,
        displayName = displayName,
        email = USER_EMAIL_RAW,
        keys = emptyList(),
        flags = emptyMap(),
        maxSpace = MAX_SPACE_RAW,
        maxBaseSpace = MAX_BASE_SPACE_RAW,
        maxUpload = 1,
        name = name,
        private = true,
        role = role,
        services = 1,
        subscribed = subscribed,
        usedSpace = USED_SPACE_RAW,
        usedBaseSpace = USED_BASE_SPACE_RAW,
        userId = userId,
        recovery = null
    )
}
