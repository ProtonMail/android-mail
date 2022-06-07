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

package ch.protonmail.android.uitest.test.suite

import ch.protonmail.android.uitest.annotation.SmokeTest
import ch.protonmail.android.uitest.test.login.LoginFlowTests
import ch.protonmail.android.uitest.test.settings.SettingsFlowTest
import ch.protonmail.android.uitest.test.settings.account.AccountSettingsScreenTest
import ch.protonmail.android.uitest.test.settings.account.conversationmode.ConversationModeSettingScreenTest
import ch.protonmail.android.uitest.test.settings.appsettings.SettingsScreenTest
import ch.protonmail.android.uitest.test.settings.appsettings.theme.ThemeSettingScreenTest
import org.junit.experimental.categories.Categories
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Categories::class)
@Categories.IncludeCategory(SmokeTest::class)
@Suite.SuiteClasses(
    LoginFlowTests::class,
    SettingsFlowTest::class,
    SettingsScreenTest::class,
    AccountSettingsScreenTest::class,
    ConversationModeSettingScreenTest::class,
    ThemeSettingScreenTest::class
)
class SmokeSuite
