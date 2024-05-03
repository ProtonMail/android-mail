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

package ch.protonmail.android.uitest.e2e.mailbox.detail.bottomsheet.moveto

import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.get
import ch.protonmail.android.networkmocks.mockwebserver.requests.matchWildcards
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.serveOnce
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.models.avatar.AvatarInitial
import ch.protonmail.android.uitest.models.folders.MailLabelEntry
import ch.protonmail.android.uitest.models.mailbox.MailboxListItemEntry
import ch.protonmail.android.uitest.models.mailbox.ParticipantEntry
import ch.protonmail.android.uitest.robot.detail.conversationDetailRobot
import ch.protonmail.android.uitest.robot.detail.model.bottomsheet.MoveToBottomSheetFolderEntry.SystemFolders.Trash
import ch.protonmail.android.uitest.robot.detail.section.bottomBarSection
import ch.protonmail.android.uitest.robot.detail.section.messageBodySection
import ch.protonmail.android.uitest.robot.detail.section.moveToBottomSheetSection
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.section.listSection
import ch.protonmail.android.uitest.robot.mailbox.section.verify
import ch.protonmail.android.uitest.robot.menu.menuRobot
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Test

@RegressionTest
@HiltAndroidTest
@UninstallModules(ServerProofModule::class)
internal class DetailMoveToBottomSheetLabelsTests : MockedNetworkTest() {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    private val startMailboxItem = MailboxListItemEntry(
        index = 0,
        avatarInitial = AvatarInitial.WithText("M"),
        participants = listOf(ParticipantEntry.WithParticipant("mobileappsuitesting2")),
        labels = listOf(MailLabelEntry(index = 0, name = "Test Label")),
        subject = "Example test",
        date = "Mar 6, 2023"
    )

    private val finalMailboxItem = startMailboxItem.copy(labels = emptyList())

    @Test
    @TestId("185425")
    fun checkMoveToBottomSheetMoveToTrashFolder() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(
            useDefaultMailSettings = false,
            useDefaultLabels = false,
            useDefaultCustomFolders = false,
            useDefaultMailReadResponses = true
        ) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_185425.json"
                    withStatusCode 200,
                get("/core/v4/labels?Type=1")
                    respondWith "/core/v4/labels/labels-type1_185425.json"
                    withStatusCode 200,
                get("/core/v4/labels?Type=3")
                    respondWith "/core/v4/labels/labels-type3_base_placeholder_empty.json"
                    withStatusCode 200,
                get("/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=0&Sort=Time&Desc=1")
                    respondWith "/mail/v4/conversations/conversations_185425.json"
                    withStatusCode 200,
                get("/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=3&Sort=Time&Desc=1")
                    respondWith "/mail/v4/conversations/conversations_185425_2.json"
                    withStatusCode 200,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_185425.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_185425.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        mailboxRobot {
            listSection {
                verify { listItemsAreShown(startMailboxItem) }
                clickMessageByPosition(0)
            }
        }

        conversationDetailRobot {
            messageBodySection { waitUntilMessageIsShown() }
            bottomBarSection { openMoveToBottomSheet() }

            moveToBottomSheetSection {
                selectFolderWithName(Trash.name)
                tapDoneButton()
            }
        }

        menuRobot {
            openSidebarMenu()
            openTrash()
        }

        mailboxRobot {
            listSection {
                verify { listItemsAreShown(finalMailboxItem) }
            }
        }
    }

    @Test
    @TestId("185425/2", "185426")
    fun checkMoveToBottomSheetMoveToSpamFolder() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(
            useDefaultMailSettings = false,
            useDefaultLabels = false,
            useDefaultCustomFolders = false
        ) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_185425.json"
                    withStatusCode 200,
                get("/core/v4/labels?Type=1")
                    respondWith "/core/v4/labels/labels-type1_185425.json"
                    withStatusCode 200,
                get("/core/v4/labels?Type=3")
                    respondWith "/core/v4/labels/labels-type3_base_placeholder_empty.json"
                    withStatusCode 200,
                get("/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=0&Sort=Time&Desc=1")
                    respondWith "/mail/v4/conversations/conversations_185425.json"
                    withStatusCode 200,
                get("/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=4&Sort=Time&Desc=1")
                    respondWith "/mail/v4/conversations/conversations_185425_3.json"
                    withStatusCode 200,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_185425.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_185425.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        mailboxRobot {
            listSection {
                verify { listItemsAreShown(startMailboxItem) }
                clickMessageByPosition(0)
            }
        }

        conversationDetailRobot {
            messageBodySection { waitUntilMessageIsShown() }
            bottomBarSection { openMoveToBottomSheet() }

            moveToBottomSheetSection {
                selectFolderWithName(Trash.name)
                tapDoneButton()
            }
        }

        menuRobot {
            openSidebarMenu()
            openSpam()
        }

        mailboxRobot {
            listSection {
                verify { listItemsAreShown(finalMailboxItem) }
            }
        }
    }
}
