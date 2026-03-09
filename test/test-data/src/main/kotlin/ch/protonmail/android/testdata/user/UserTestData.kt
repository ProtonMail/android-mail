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

import ch.protonmail.android.mailcommon.data.mapper.LocalUser
import ch.protonmail.android.mailsession.domain.model.User
import ch.protonmail.android.testdata.user.UserTestData.USER_DISPLAY_NAME_RAW
import ch.protonmail.android.testdata.user.UserTestData.USER_EMAIL_RAW
import ch.protonmail.android.testdata.user.UserTestData.USER_NAME_RAW
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.Flags
import uniffi.mail_uniffi.ProductUsedSpace
import uniffi.mail_uniffi.Role
import uniffi.mail_uniffi.UnixTimestamp
import uniffi.mail_uniffi.UserMnemonicStatus
import uniffi.mail_uniffi.UserType

object UserTestData {

    const val USER_EMAIL_RAW = "userEmail"
    const val USER_DISPLAY_NAME_RAW = "userDisplayName"
    const val USER_NAME_RAW = "username"

    val Primary = build()

    val freeUser = build(userId = UserIdTestData.freeUserId, subscribed = 0)

    val paidMailUser = build(userId = UserIdTestData.paidUserid, subscribed = 1)

    val paidUser = build(subscribed = 2)

    fun build(
        displayName: String = USER_DISPLAY_NAME_RAW,
        name: String = USER_NAME_RAW,
        userId: UserId = UserIdTestData.Primary,
        subscribed: Int = 1,
        delinquent: Int = 0,
        createdTimeUtc: Long = 1L,
        usedSpace: Long = 1_000_000_000L,
        maxSpace: Long = 15_000_000_000L
    ) = User(
        userId = userId,
        displayName = displayName,
        email = USER_EMAIL_RAW,
        name = name,
        services = 1,
        subscribed = subscribed,
        delinquent = delinquent,
        createTimeUtc = createdTimeUtc,
        usedSpace = usedSpace,
        maxSpace = maxSpace
    )
}

object LocalUserTestData {

    fun build(
        displayName: String = USER_DISPLAY_NAME_RAW,
        name: String = USER_NAME_RAW,
        services: Int = 0,
        subscribed: Int = 0
    ) = LocalUser(
        createTime = UnixTimestamp.MIN_VALUE,
        credit = 0L,
        currency = "USD",
        delinquent = 0U,
        displayName = displayName,
        email = USER_EMAIL_RAW,
        maxSpace = 0L,
        maxUpload = 0L,
        mnemonicStatus = UserMnemonicStatus.ENABLED_AND_SET,
        private = false,
        productUsedSpace = ProductUsedSpace(0L, 0L, 0L, 0L, 0L),
        role = Role.Member,
        name = name,
        services = services.toUInt(),
        toMigrate = false,
        subscribed = subscribed.toUInt(),
        usedSpace = 0L,
        userType = UserType.Proton,
        flags = getDefaultFlags()
    )

    private fun getDefaultFlags() = Flags(
        hasTemporaryPassword = false,
        noLogin = false,
        noProtonAddress = false,
        onboardChecklistStorageGranted = false,
        protected = false,
        recoveryAttempt = false,
        sso = false,
        testAccount = false
    )
}
