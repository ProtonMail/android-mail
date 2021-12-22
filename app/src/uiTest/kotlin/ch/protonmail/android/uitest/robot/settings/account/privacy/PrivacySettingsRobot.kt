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

package ch.protonmail.android.uitest.robot.settings.account.privacy

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import ch.protonmail.android.uitest.robot.settings.account.AccountSettingsRobot

@Suppress("unused", "TooManyFunctions", "ExpressionBodySyntax")
class PrivacySettingsRobot {

    fun navigateUpToAccountSettings(): AccountSettingsRobot {
        return AccountSettingsRobot()
    }

    fun autoDownloadMessages(): AutoDownloadMessagesRobot {
        return AutoDownloadMessagesRobot()
    }

    fun backgroundSync(): BackgroundSyncRobot {
        return BackgroundSyncRobot()
    }

    fun enableAutoShowRemoteImages(): PrivacySettingsRobot {
        return this
    }

    fun disableAutoShowRemoteImages(): PrivacySettingsRobot {
        return this
    }

    fun enableAutoShowEmbeddedImages(): PrivacySettingsRobot {
        return this
    }

    fun disableAutoShowEmbeddedImages(): PrivacySettingsRobot {
        return this
    }

    fun enablePreventTakingScreenshots(): PrivacySettingsRobot {
        return this
    }

    fun disablePreventTakingScreenshots(): PrivacySettingsRobot {
        return this
    }

    fun enableRequestLinkConfirmation(): PrivacySettingsRobot {
        return this
    }

    fun disableRequestLinkConfirmation(): PrivacySettingsRobot {
        return this
    }

    @SuppressWarnings("EmptyFunctionBlock")
    private fun selectSettingsItem(@StringRes title: Int) {
    }

    @SuppressWarnings("EmptyFunctionBlock")
    private fun toggleSwitchWithTitle(@IdRes titleId: Int, value: Boolean) {
    }

    @SuppressWarnings("EmptyFunctionBlock")
    private fun switch(@StringRes tagId: Int) {
    }

    /**
     * Contains all the validations that can be performed by [PrivacySettingsRobot].
     */
    class Verify {

        @SuppressWarnings("EmptyFunctionBlock")
        fun autoDownloadImagesIsEnabled() {}

        @SuppressWarnings("EmptyFunctionBlock")
        fun backgroundSyncIsEnabled() {}

        @SuppressWarnings("EmptyFunctionBlock")
        fun takingScreenshotIsDisabled() {}
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
