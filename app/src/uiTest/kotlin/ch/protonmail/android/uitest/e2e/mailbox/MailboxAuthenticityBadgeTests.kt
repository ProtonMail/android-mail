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
import ch.protonmail.android.networkmocks.mockwebserver.requests.MockPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.get
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.withPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.login.LoginTestUserTypes
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.models.avatar.AvatarInitial
import ch.protonmail.android.uitest.models.mailbox.MailboxListItemEntry
import ch.protonmail.android.uitest.models.mailbox.MailboxType
import ch.protonmail.android.uitest.models.mailbox.ParticipantEntry
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.section.listSection
import ch.protonmail.android.uitest.robot.mailbox.section.topAppBarSection
import ch.protonmail.android.uitest.robot.mailbox.section.verify
import ch.protonmail.android.uitest.robot.menu.menuRobot
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Test

@RegressionTest
@UninstallModules(ServerProofModule::class)
@HiltAndroidTest
internal class MailboxAuthenticityBadgeTests : MockedNetworkTest(
    loginType = LoginTestUserTypes.Paid.FancyCapybara
) {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    private val mailboxItemProtonOfficial = MailboxListItemEntry(
        index = 0,
        avatarInitial = AvatarInitial.WithText("P"),
        participants = listOf(ParticipantEntry.Common.ProtonOfficial),
        subject = "Official message",
        date = "Jul 5, 2023"
    )

    private val mailboxItemProtonUnofficial = MailboxListItemEntry(
        index = 1,
        avatarInitial = AvatarInitial.WithText("P"),
        participants = listOf(ParticipantEntry.Common.ProtonUnofficial),
        subject = "Not official message",
        date = "Jul 5, 2023"
    )

    private val mailboxItemLongNameOfficial = mailboxItemProtonOfficial.copy(
        participants = listOf(
            ParticipantEntry.WithParticipant(
                name = "ProtonProtonProtonProtonProtonProtonProtonProton",
                isProton = true
            )
        )
    )

    @Test
    @SmokeTest
    @TestId("192128", "192129")
    fun testAuthenticityBadgeInMailboxInMessageMode() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_192128.json"
                    withStatusCode 200,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_192128.json"
                    withStatusCode 200 ignoreQueryParams true
            )
        }

        val expectedItems = arrayOf(mailboxItemProtonOfficial, mailboxItemProtonUnofficial)

        navigator { navigateTo(Destination.Inbox) }

        mailboxRobot {
            listSection {
                verify { listItemsAreShown(*expectedItems) }
            }
        }
    }

    @Test
    @TestId("194274")
    fun testAuthenticityBadgeWithLongNameInMessageMode() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_194274.json"
                    withStatusCode 200,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_194274.json"
                    withStatusCode 200 ignoreQueryParams true
            )
        }

        navigator { navigateTo(Destination.Inbox) }

        mailboxRobot {
            listSection {
                verify { listItemsAreShown(mailboxItemLongNameOfficial) }
            }
        }
    }

    @Test
    @TestId("192130", "192131", "192132", "192133")
    fun testAuthenticityBadgeInSentFolder() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_192130.json"
                    withStatusCode 200,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/messages?Page=0&PageSize=75&Limit=75&LabelID=7&Sort=Time&Desc=1")
                    respondWith "/mail/v4/messages/messages_192130.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        val expectedItems = arrayOf(
            mailboxItemProtonUnofficial.copy(
                index = 0,
                participants = listOf(ParticipantEntry.Common.ProtonUnofficial, ParticipantEntry.Common.FreeUser)
            ),
            mailboxItemProtonOfficial.copy(
                index = 1,
                participants = listOf(ParticipantEntry.Common.ProtonOfficial, ParticipantEntry.Common.FreeUser)
            ),
            mailboxItemProtonUnofficial.copy(index = 2),
            mailboxItemProtonOfficial.copy(index = 3)
        )

        navigator { navigateTo(Destination.Inbox) }

        menuRobot {
            openSidebarMenu()
            openSent()
        }

        mailboxRobot {
            topAppBarSection { verify { isMailbox(MailboxType.Sent) } }

            listSection {
                verify { listItemsAreShown(*expectedItems) }
            }
        }
    }

    @Test
    @SmokeTest
    @TestId("192134", "192135")
    fun testAuthenticityBadgeInMailboxInConversationMode() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_192134.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_192134.json"
                    withStatusCode 200 ignoreQueryParams true
            )
        }

        val expectedItems = arrayOf(mailboxItemProtonOfficial, mailboxItemProtonUnofficial)

        navigator { navigateTo(Destination.Inbox) }

        mailboxRobot {
            listSection {
                verify { listItemsAreShown(*expectedItems) }
            }
        }
    }

    @Test
    @TestId("194275")
    fun testAuthenticityBadgeWithLongNameInConversationMode() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_194275.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_194275.json"
                    withStatusCode 200 ignoreQueryParams true
            )
        }

        navigator { navigateTo(Destination.Inbox) }

        mailboxRobot {
            listSection {
                verify { listItemsAreShown(mailboxItemLongNameOfficial) }
            }
        }
    }
}
