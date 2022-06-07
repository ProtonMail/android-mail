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
package ch.protonmail.android.uitest.robot.settings

import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailsettings.presentation.R.string
import ch.protonmail.android.mailsettings.presentation.settings.TEST_TAG_SETTINGS_SCREEN_ACCOUNT_ITEM
import ch.protonmail.android.uitest.robot.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitest.robot.settings.account.AccountSettingsRobot
import ch.protonmail.android.uitest.robot.settings.autolock.AutoLockRobot
import ch.protonmail.android.uitest.util.onNodeWithText
import me.proton.core.compose.component.PROTON_PROGRESS_TEST_TAG

/**
 * [SettingsRobot] class contains actions and verifications for Settings view.
 */
@Suppress("unused", "ExpressionBodySyntax")
class SettingsRobot(
    private val composeTestRule: ComposeContentTestRule? = null
) {

    fun navigateUpToInbox(): InboxRobot {
        return InboxRobot()
    }

    fun emptyCache(): SettingsRobot {
        return this
    }

    fun openUserAccountSettings(): AccountSettingsRobot {
        composeTestRule!!
            .onNodeWithTag(TEST_TAG_SETTINGS_SCREEN_ACCOUNT_ITEM)
            .performClick()

        composeTestRule.waitUntil { progressIsHidden(composeTestRule) }

        return AccountSettingsRobot(composeTestRule)
    }

    fun selectThemeSettings(): ThemeRobot {
        composeTestRule!!
            .onNodeWithText(string.mail_settings_theme)
            .performClick()
        composeTestRule.waitForIdle()

        return ThemeRobot(composeTestRule)
    }

    fun selectLanguageSettings(): LanguageRobot {
        composeTestRule!!
            .onNodeWithText(string.mail_settings_app_language)
            .performClick()
        composeTestRule.waitForIdle()

        return LanguageRobot(composeTestRule)
    }

    fun selectAutoLock(): AutoLockRobot {
        return AutoLockRobot()
    }

    fun selectSettingsItemByValue(value: String): AccountSettingsRobot {
        return AccountSettingsRobot()
    }

    @Suppress("EmptyFunctionBlock")
    private fun selectItemByHeader(header: String) {}

    private fun progressIsHidden(composeTestRule: ComposeContentTestRule): Boolean {
        try {
            composeTestRule
                .onNodeWithTag(PROTON_PROGRESS_TEST_TAG)
                .assertIsNotDisplayed()
        } catch (ignored: AssertionError) {
            return true
        }
        return false
    }

    /**
     * Contains all the validations that can be performed by [SettingsRobot].
     */
    class Verify {

        @Suppress("EmptyFunctionBlock")
        fun settingsOpened() {}
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
