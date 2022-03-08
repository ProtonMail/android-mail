/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.testdata.usersettings

import ch.protonmail.android.testdata.user.UserIdTestData
import me.proton.core.usersettings.domain.entity.PasswordSetting
import me.proton.core.usersettings.domain.entity.RecoverySetting
import me.proton.core.usersettings.domain.entity.UserSettings

object UserSettingsTestData {
    const val RECOVERY_EMAIL_RAW = "recoveryemail@proton.ch"
    private const val RECOVERY_PHONE_RAW = "+346527847362"

    val userSettings = UserSettings(
        UserIdTestData.userId,
        email = RecoverySetting(
            value = RECOVERY_EMAIL_RAW,
            status = null,
            notify = false,
            reset = false
        ),
        phone = RecoverySetting(
            value = RECOVERY_PHONE_RAW,
            status = null,
            notify = false,
            reset = false
        ),
        PasswordSetting(
            mode = 0,
            expirationTime = 0
        ),
        twoFA = null,
        news = null,
        locale = null,
        logAuth = null,
        invoiceText = null,
        density = null,
        theme = null,
        themeType = null,
        weekStart = null,
        dateFormat = null,
        timeFormat = null,
        welcome = null,
        earlyAccess = null,
        flags = null
    )

    val emptyUserSettings = UserSettings(
        UserIdTestData.userId,
        email = RecoverySetting(
            value = "",
            status = null,
            notify = false,
            reset = false
        ),
        phone = RecoverySetting(
            value = "",
            status = null,
            notify = false,
            reset = false
        ),
        PasswordSetting(
            mode = null,
            expirationTime = null
        ),
        twoFA = null,
        news = null,
        locale = null,
        logAuth = null,
        invoiceText = null,
        density = null,
        theme = null,
        themeType = null,
        weekStart = null,
        dateFormat = null,
        timeFormat = null,
        welcome = null,
        earlyAccess = null,
        flags = null
    )
}
