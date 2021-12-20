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
package ch.protonmail.android.uitests.robots.settings.account

import androidx.annotation.IdRes
import ch.protonmail.android.uitests.robots.settings.SettingsRobot
import ch.protonmail.android.uitests.robots.settings.account.labelsandfolders.LabelsAndFoldersRobot
import ch.protonmail.android.uitests.robots.settings.account.privacy.PrivacySettingsRobot
import ch.protonmail.android.uitests.robots.settings.account.swipinggestures.SwipingGesturesSettingsRobot

/**
 * [AccountSettingsRobot] class contains actions and verifications for
 * Account settings functionality.
 */
@Suppress("unused", "ExpressionBodySyntax")
class AccountSettingsRobot {

    fun subscription(): SubscriptionRobot {
        return SubscriptionRobot()
    }

    fun privacy(): PrivacySettingsRobot {
        return PrivacySettingsRobot()
    }

    fun defaultEmailAddress(): DefaultEmailAddressRobot {
        return DefaultEmailAddressRobot()
    }

    fun displayNameAndSignature(): DisplayNameAndSignatureRobot {
        return DisplayNameAndSignatureRobot()
    }

    fun foldersAndLabels(): LabelsAndFoldersRobot {
        return LabelsAndFoldersRobot()
    }

    fun navigateUpToSettings(): SettingsRobot {
        return SettingsRobot()
    }

    fun swipingGestures(): SwipingGesturesSettingsRobot {
        return SwipingGesturesSettingsRobot()
    }

    @SuppressWarnings("EmptyFunctionBlock")
    private fun clickOnSettingsItemWithHeader(@IdRes stringId: Int) {}

    /**
     * Contains all the validations that can be performed by [AccountSettingsRobot].
     */
    class Verify {

        @SuppressWarnings("EmptyFunctionBlock")
        fun accountSettingsOpened() {}
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
