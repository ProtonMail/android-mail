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

package ch.protonmail.android.uitest.e2e.mailbox.allmail

import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.requests.MockPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.withPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.login.LoginStrategy
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.models.avatar.AvatarInitial
import ch.protonmail.android.uitest.models.folders.MailFolderEntry
import ch.protonmail.android.uitest.models.folders.Tint
import ch.protonmail.android.uitest.models.mailbox.MailboxListItemEntry
import ch.protonmail.android.uitest.robot.menu.MenuRobot
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Test

@RegressionTest
@HiltAndroidTest
@UninstallModules(ServerProofModule::class)
internal class AllMailMailboxFolderColorsTests : MockedNetworkTest(loginStrategy = LoginStrategy.LoggedOut) {

    private val menuRobot = MenuRobot()

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    private val firstMessageEntry = MailboxListItemEntry(
        index = 0,
        avatarInitial = AvatarInitial.WithText("M"),
        participants = "mobileappsuitesting3",
        subject = "Parent folder message",
        date = "Mar 28, 2023"
    )

    private val secondMessageEntry = MailboxListItemEntry(
        index = 1,
        avatarInitial = AvatarInitial.WithText("M"),
        participants = "mobileappsuitesting2",
        subject = "Child folder message",
        date = "Mar 21, 2023"
    )

    @Test
    @TestId("80673")
    fun checkFolderColorInAllMailWithSettingEnabledAndParentInheritingDisabledInConversationMode() {
        mockWebServer.dispatcher = mockNetworkDispatcher(
            useDefaultMailSettings = false,
            useDefaultCustomFolders = false
        ) {
            addMockRequests(
                "/mail/v4/settings"
                    respondWith "/mail/v4/settings/mail-v4-settings_80673.json"
                    withStatusCode 200,
                "/core/v4/labels?Type=3"
                    respondWith "/core/v4/labels/labels-type3_80673.json"
                    withStatusCode 200,
                "/mail/v4/conversations"
                    respondWith "/mail/v4/conversations/conversations_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                "/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=5&Sort=Time&Desc=1"
                    respondWith "/mail/v4/conversations/conversations_80673.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        verifyMailboxItems(
            firstLocationIcon = MailFolderEntry(index = 0, iconTint = Tint.WithColor.Carrot),
            secondLocationIcon = MailFolderEntry(index = 0, iconTint = Tint.WithColor.Fern)
        )
    }

    @Test
    @TestId("80674")
    fun checkFolderColorInAllMailWithSettingEnabledAndParentInheritingDisabledInMessageMode() {
        mockWebServer.dispatcher = mockNetworkDispatcher(
            useDefaultMailSettings = false,
            useDefaultCustomFolders = false
        ) {
            addMockRequests(
                "/mail/v4/settings"
                    respondWith "/mail/v4/settings/mail-v4-settings_80674.json"
                    withStatusCode 200,
                "/core/v4/labels?Type=3"
                    respondWith "/core/v4/labels/labels-type3_80674.json"
                    withStatusCode 200,
                "/mail/v4/messages"
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                "/mail/v4/messages?Page=0&PageSize=75&Limit=75&LabelID=5&Sort=Time&Desc=1"
                    respondWith "/mail/v4/messages/messages_80674.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        verifyMailboxItems(
            firstLocationIcon = MailFolderEntry(index = 0, iconTint = Tint.WithColor.Carrot),
            secondLocationIcon = MailFolderEntry(index = 0, iconTint = Tint.WithColor.Fern)
        )
    }

    @Test
    @TestId("80675")
    fun checkFolderColorInAllMailWithSettingEnabledAndParentInheritingEnabledInConversationMode() {
        mockWebServer.dispatcher = mockNetworkDispatcher(
            useDefaultMailSettings = false,
            useDefaultCustomFolders = false
        ) {
            addMockRequests(
                "/mail/v4/settings"
                    respondWith "/mail/v4/settings/mail-v4-settings_80675.json"
                    withStatusCode 200,
                "/core/v4/labels?Type=3"
                    respondWith "/core/v4/labels/labels-type3_80675.json"
                    withStatusCode 200,
                "/mail/v4/conversations"
                    respondWith "/mail/v4/conversations/conversations_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                "/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=5&Sort=Time&Desc=1"
                    respondWith "/mail/v4/conversations/conversations_80675.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        verifyMailboxItems(
            firstLocationIcon = MailFolderEntry(index = 0, iconTint = Tint.WithColor.Carrot),
            secondLocationIcon = MailFolderEntry(index = 0, iconTint = Tint.WithColor.Carrot)
        )
    }

    @Test
    @TestId("80676")
    fun checkFolderColorInAllMailWithSettingEnabledAndParentInheritingEnabledInMessageMode() {
        mockWebServer.dispatcher = mockNetworkDispatcher(
            useDefaultMailSettings = false,
            useDefaultCustomFolders = false
        ) {
            addMockRequests(
                "/mail/v4/settings"
                    respondWith "/mail/v4/settings/mail-v4-settings_80676.json"
                    withStatusCode 200,
                "/core/v4/labels?Type=3"
                    respondWith "/core/v4/labels/labels-type3_80676.json"
                    withStatusCode 200,
                "/mail/v4/messages"
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                "/mail/v4/messages?Page=0&PageSize=75&Limit=75&LabelID=5&Sort=Time&Desc=1"
                    respondWith "/mail/v4/messages/messages_80676.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        verifyMailboxItems(
            firstLocationIcon = MailFolderEntry(index = 0, iconTint = Tint.WithColor.Carrot),
            secondLocationIcon = MailFolderEntry(index = 0, iconTint = Tint.WithColor.Carrot)
        )
    }

    @Test
    @TestId("80677")
    fun checkFolderColorInAllMailWithSettingDisabledAndParentInheritingEnabledInConversationMode() {
        mockWebServer.dispatcher = mockNetworkDispatcher(
            useDefaultMailSettings = false,
            useDefaultCustomFolders = false
        ) {
            addMockRequests(
                "/mail/v4/settings"
                    respondWith "/mail/v4/settings/mail-v4-settings_80677.json"
                    withStatusCode 200,
                "/core/v4/labels?Type=3"
                    respondWith "/core/v4/labels/labels-type3_80677.json"
                    withStatusCode 200,
                "/mail/v4/conversations"
                    respondWith "/mail/v4/conversations/conversations_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                "/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=5&Sort=Time&Desc=1"
                    respondWith "/mail/v4/conversations/conversations_80677.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        verifyMailboxItems(
            firstLocationIcon = MailFolderEntry(index = 0, iconTint = Tint.NoColor),
            secondLocationIcon = MailFolderEntry(index = 0, iconTint = Tint.NoColor)
        )
    }

    @Test
    @TestId("80678")
    fun checkFolderColorInAllMailWithSettingDisabledAndParentInheritingEnabledInMessageMode() {
        mockWebServer.dispatcher = mockNetworkDispatcher(
            useDefaultMailSettings = false,
            useDefaultCustomFolders = false
        ) {
            addMockRequests(
                "/mail/v4/settings"
                    respondWith "/mail/v4/settings/mail-v4-settings_80678.json"
                    withStatusCode 200,
                "/core/v4/labels?Type=3"
                    respondWith "/core/v4/labels/labels-type3_80678.json"
                    withStatusCode 200,
                "/mail/v4/messages"
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                "/mail/v4/messages?Page=0&PageSize=75&Limit=75&LabelID=5&Sort=Time&Desc=1"
                    respondWith "/mail/v4/messages/messages_80678.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        verifyMailboxItems(
            firstLocationIcon = MailFolderEntry(index = 0, iconTint = Tint.NoColor),
            secondLocationIcon = MailFolderEntry(index = 0, iconTint = Tint.NoColor)
        )
    }

    @Test
    @TestId("80679")
    fun checkFolderColorInAllMailWithSettingDisabledAndParentInheritingDisabledInConversationMode() {
        mockWebServer.dispatcher = mockNetworkDispatcher(
            useDefaultMailSettings = false,
            useDefaultCustomFolders = false
        ) {
            addMockRequests(
                "/mail/v4/settings"
                    respondWith "/mail/v4/settings/mail-v4-settings_80679.json"
                    withStatusCode 200,
                "/core/v4/labels?Type=3"
                    respondWith "/core/v4/labels/labels-type3_80679.json"
                    withStatusCode 200,
                "/mail/v4/conversations"
                    respondWith "/mail/v4/conversations/conversations_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                "/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=5&Sort=Time&Desc=1"
                    respondWith "/mail/v4/conversations/conversations_80679.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        verifyMailboxItems(
            firstLocationIcon = MailFolderEntry(index = 0, iconTint = Tint.NoColor),
            secondLocationIcon = MailFolderEntry(index = 0, iconTint = Tint.NoColor)
        )
    }

    @Test
    @TestId("80680")
    fun checkFolderColorInAllMailWithSettingDisabledAndParentInheritingDisabledInMessageMode() {
        mockWebServer.dispatcher = mockNetworkDispatcher(
            useDefaultMailSettings = false,
            useDefaultCustomFolders = false
        ) {
            addMockRequests(
                "/mail/v4/settings"
                    respondWith "/mail/v4/settings/mail-v4-settings_80680.json"
                    withStatusCode 200,
                "/core/v4/labels?Type=3"
                    respondWith "/core/v4/labels/labels-type3_80680.json"
                    withStatusCode 200,
                "/mail/v4/messages"
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                "/mail/v4/messages?Page=0&PageSize=75&Limit=75&LabelID=5&Sort=Time&Desc=1"
                    respondWith "/mail/v4/messages/messages_80680.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        verifyMailboxItems(
            firstLocationIcon = MailFolderEntry(index = 0, iconTint = Tint.NoColor),
            secondLocationIcon = MailFolderEntry(index = 0, iconTint = Tint.NoColor)
        )
    }

    private fun verifyMailboxItems(firstLocationIcon: MailFolderEntry, secondLocationIcon: MailFolderEntry) {
        navigator { navigateTo(Destination.Inbox) }

        val expectedMailboxEntries = arrayOf(
            firstMessageEntry.copy(locationIcons = listOf(firstLocationIcon)),
            secondMessageEntry.copy(locationIcons = listOf(secondLocationIcon))
        )

        menuRobot
            .swipeOpenSidebarMenu()
            .openAllMail()
            .verify { listItemsAreShown(*expectedMailboxEntries) }
    }
}
