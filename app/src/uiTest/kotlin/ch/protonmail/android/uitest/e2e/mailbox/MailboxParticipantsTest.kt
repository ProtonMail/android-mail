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

package ch.protonmail.android.uitest.e2e.mailbox

import androidx.test.filters.SdkSuppress
import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.login.LoginStrategy
import ch.protonmail.android.uitest.helpers.login.MockedLoginTestUsers
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.models.mailbox.InboxListItemEntry
import ch.protonmail.android.uitest.robot.mailbox.inbox.InboxRobot
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import me.proton.core.auth.domain.usecase.ValidateServerProof
import me.proton.core.test.android.robots.auth.AddAccountRobot
import org.junit.Test

@HiltAndroidTest
@SdkSuppress(minSdkVersion = 28)
@UninstallModules(ServerProofModule::class)
internal class MailboxParticipantsTest : MockedNetworkTest(loginStrategy = LoginStrategy.LoggedOut) {

    private val addAccountRobot = AddAccountRobot()
    private val inboxRobot = InboxRobot(composeTestRule)

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk {
        every { this@mockk.invoke(any(), any(), any()) } just runs
    }

    private val expectedInboxListEntries = arrayOf(
        InboxListItemEntry(
            index = 0,
            avatarText = "M",
            participants = "mobileappsuitesting3@proton.black",
            subject = "Test no contact, empty sender name",
            date = "Mar 20, 2023"
        ),
        InboxListItemEntry(
            index = 1,
            avatarText = "?",
            participants = "",
            subject = "Test no contact, empty",
            date = "Mar 20, 2023"
        ),
        InboxListItemEntry(
            index = 2,
            avatarText = "U",
            participants = "UI Tests Contact 1",
            subject = "From contact with no sender name",
            date = "Mar 20, 2023"
        ),
    )

    @Test
    @TestId("77426")
    fun checkAvatarInitialsWithMissingParticipantDetailsInMessageMode() {
        mockWebServer.dispatcher = mockNetworkDispatcher(useDefaultMailSettings = false, useDefaultContacts = false) {
            addMockRequests(
                "/mail/v4/settings" respondWith "/mail/v4/settings/mail-v4-settings_77426.json" withStatusCode 200,
                "/contacts/v4/contacts" respondWith "/contacts/v4/contacts/contacts_77426.json" withStatusCode 200 ignoreQueryParams true,
                "/contacts/v4/contacts/emails" respondWith "/contacts/v4/contacts/emails/contacts-emails_77426.json" withStatusCode 200 ignoreQueryParams true,
                "/mail/v4/messages" respondWith "/mail/v4/messages/messages_77426.json" withStatusCode 200 ignoreQueryParams true,
            )
        }

        addAccountRobot
            .signIn()
            .loginUser<Any>(MockedLoginTestUsers.defaultLoginUser)

        inboxRobot.verify { listItemsAreShown(*expectedInboxListEntries) }
    }

    @Test
    @TestId("77427")
    fun checkAvatarInitialsWithMissingParticipantDetailsInConversationMode() {
        mockWebServer.dispatcher = mockNetworkDispatcher(useDefaultMailSettings = false, useDefaultContacts = false) {
            addMockRequests(
                "/mail/v4/settings" respondWith "/mail/v4/settings/mail-v4-settings_77427.json" withStatusCode 200,
                "/contacts/v4/contacts" respondWith "/contacts/v4/contacts/contacts_77427.json" withStatusCode 200 ignoreQueryParams true,
                "/contacts/v4/contacts/emails" respondWith "/contacts/v4/contacts/emails/contacts-emails_77427.json" withStatusCode 200 ignoreQueryParams true,
                "/mail/v4/conversations" respondWith "/mail/v4/conversations/conversations_77427.json" withStatusCode 200 ignoreQueryParams true
            )
        }

        addAccountRobot
            .signIn()
            .loginUser<Any>(MockedLoginTestUsers.defaultLoginUser)

        inboxRobot.verify { listItemsAreShown(*expectedInboxListEntries) }
    }
}
