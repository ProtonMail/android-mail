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

import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.get
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.models.avatar.AvatarInitial
import ch.protonmail.android.uitest.models.mailbox.MailboxListItemEntry
import ch.protonmail.android.uitest.models.mailbox.ParticipantEntry
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.section.listSection
import ch.protonmail.android.uitest.robot.mailbox.section.verify
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Test

@RegressionTest
@HiltAndroidTest
@UninstallModules(ServerProofModule::class)
internal class MailboxParticipantsTest : MockedNetworkTest() {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    private val expectedInboxListEntries = arrayOf(
        MailboxListItemEntry(
            index = 0,
            avatarInitial = AvatarInitial.WithText("M"),
            participants = listOf(
                ParticipantEntry.WithParticipant("mobileappsuitesting3@proton.black")
            ),
            subject = "Test no contact, empty sender name",
            date = "Mar 20, 2023"
        ),
        MailboxListItemEntry(
            index = 1,
            avatarInitial = AvatarInitial.WithText("?"),
            participants = listOf(ParticipantEntry.NoSender),
            subject = "Test no contact, empty",
            date = "Mar 20, 2023"
        ),
        MailboxListItemEntry(
            index = 2,
            avatarInitial = AvatarInitial.WithText("U"),
            participants = listOf(ParticipantEntry.WithParticipant("UI Tests Contact 1")),
            subject = "From contact with no sender name",
            date = "Mar 20, 2023"
        )
    )

    @Test
    @TestId("77426")
    fun checkAvatarInitialsWithMissingParticipantDetailsInMessageMode() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(
            useDefaultMailSettings = false,
            useDefaultContacts = false
        ) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_77426.json"
                    withStatusCode 200,
                get("/contacts/v4/contacts")
                    respondWith "/contacts/v4/contacts/contacts_77426.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/contacts/v4/contacts/emails")
                    respondWith "/contacts/v4/contacts/emails/contacts-emails_77426.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_77426.json"
                    withStatusCode 200 ignoreQueryParams true
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        mailboxRobot {
            listSection {
                verify { listItemsAreShown(*expectedInboxListEntries) }
            }
        }
    }

    @Test
    @TestId("77427")
    fun checkAvatarInitialsWithMissingParticipantDetailsInConversationMode() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(
            useDefaultMailSettings = false,
            useDefaultContacts = false
        ) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_77427.json"
                    withStatusCode 200,
                get("/contacts/v4/contacts")
                    respondWith "/contacts/v4/contacts/contacts_77427.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/contacts/v4/contacts/emails")
                    respondWith "/contacts/v4/contacts/emails/contacts-emails_77427.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_77427.json"
                    withStatusCode 200 ignoreQueryParams true
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        mailboxRobot {
            listSection {
                verify { listItemsAreShown(*expectedInboxListEntries) }
            }
        }
    }
}
