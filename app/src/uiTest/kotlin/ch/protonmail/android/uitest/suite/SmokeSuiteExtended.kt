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

package ch.protonmail.android.uitest.suite

import ch.protonmail.android.uitest.e2e.login.LoginFlowTests
import ch.protonmail.android.uitest.e2e.mailbox.MailboxFlowTest
import ch.protonmail.android.uitest.e2e.settings.SettingsFlowTest
import ch.protonmail.android.uitest.screen.common.BottomActionBarTest
import ch.protonmail.android.uitest.screen.mailbox.MailboxItemLabelsTest
import ch.protonmail.android.uitest.screen.mailbox.MailboxScreenTest
import ch.protonmail.android.uitest.screen.mailbox.MailboxTopAppBarTest
import ch.protonmail.android.uitest.screen.settings.account.AccountSettingsScreenTest
import ch.protonmail.android.uitest.screen.settings.account.conversationmode.ConversationModeSettingScreenTest
import ch.protonmail.android.uitest.screen.settings.appsettings.SettingsScreenTest
import ch.protonmail.android.uitest.screen.settings.appsettings.alternativerouting.AlternativeRoutingSettingScreenTest
import ch.protonmail.android.uitest.screen.settings.appsettings.combinedcontacts.CombinedContactsSettingScreenTest
import ch.protonmail.android.uitest.screen.settings.appsettings.swipeactions.EditSwipeActionPreferenceScreenTest
import ch.protonmail.android.uitest.screen.settings.appsettings.swipeactions.SwipeActionsPreferenceScreenTest
import ch.protonmail.android.uitest.screen.settings.appsettings.theme.ThemeSettingScreenTest
import ch.protonmail.android.uitest.screen.sidebar.SidebarScreenTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    AccountSettingsScreenTest::class,
    AlternativeRoutingSettingScreenTest::class,
    BottomActionBarTest::class,
    CombinedContactsSettingScreenTest::class,
    ConversationModeSettingScreenTest::class,
    EditSwipeActionPreferenceScreenTest::class,
    LoginFlowTests::class,
    MailboxFlowTest::class,
    MailboxItemLabelsTest::class,
    MailboxScreenTest::class,
    MailboxTopAppBarTest::class,
    SettingsFlowTest::class,
    SettingsScreenTest::class,
    SidebarScreenTest::class,
    SwipeActionsPreferenceScreenTest::class,
    ThemeSettingScreenTest::class
)
class SmokeSuiteExtended
