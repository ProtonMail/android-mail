/*
 * Copyright (c) 2020 Proton Technologies AG
 * 
 * This file is part of ProtonMail.
 * 
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.uitest.robot.settings

import ch.protonmail.android.uitest.robot.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitest.robot.settings.account.AccountSettingsRobot
import ch.protonmail.android.uitest.robot.settings.autolock.AutoLockRobot

/**
 * [SettingsRobot] class contains actions and verifications for Settings view.
 */
@Suppress("unused", "ExpressionBodySyntax")
class SettingsRobot {

    fun navigateUpToInbox(): InboxRobot {
        return InboxRobot()
    }

    fun emptyCache(): SettingsRobot {
        return this
    }

    fun openUserAccountSettings(): AccountSettingsRobot {
        return AccountSettingsRobot()
    }

    fun selectAutoLock(): AutoLockRobot {
        return AutoLockRobot()
    }

    fun selectSettingsItemByValue(value: String): AccountSettingsRobot {
        return AccountSettingsRobot()
    }

    @SuppressWarnings("EmptyFunctionBlock")
    private fun selectItemByHeader(header: String) {}

    /**
     * Contains all the validations that can be performed by [SettingsRobot].
     */
    class Verify {

        @SuppressWarnings("EmptyFunctionBlock")
        fun settingsOpened() {}
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
