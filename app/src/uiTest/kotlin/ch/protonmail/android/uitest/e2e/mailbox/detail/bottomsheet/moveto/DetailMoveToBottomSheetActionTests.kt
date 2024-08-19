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
import ch.protonmail.android.uitest.models.folders.Tint
import ch.protonmail.android.uitest.models.mailbox.MailboxListItemEntry
import ch.protonmail.android.uitest.models.mailbox.ParticipantEntry
import ch.protonmail.android.uitest.robot.common.section.snackbarSection
import ch.protonmail.android.uitest.robot.common.section.verify
import ch.protonmail.android.uitest.robot.detail.conversationDetailRobot
import ch.protonmail.android.uitest.robot.detail.model.MessageDetailSnackbar
import ch.protonmail.android.uitest.robot.detail.model.bottomsheet.MoveToBottomSheetFolderEntry
import ch.protonmail.android.uitest.robot.detail.model.bottomsheet.MoveToBottomSheetFolderEntry.SystemFolders.Archive
import ch.protonmail.android.uitest.robot.detail.model.bottomsheet.MoveToBottomSheetFolderEntry.SystemFolders.Inbox
import ch.protonmail.android.uitest.robot.detail.section.bottomBarSection
import ch.protonmail.android.uitest.robot.detail.section.messageBodySection
import ch.protonmail.android.uitest.robot.detail.section.moveToBottomSheetSection
import ch.protonmail.android.uitest.robot.detail.section.verify
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.section.emptyListSection
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
internal class DetailMoveToBottomSheetActionTests : MockedNetworkTest() {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    private val firstCustomFolder = MoveToBottomSheetFolderEntry(
        index = 0, name = "Test Folder", iconTint = Tint.WithColor.Carrot
    )
    private val secondCustomFolder = MoveToBottomSheetFolderEntry(
        index = 1, name = "Child Test Folder", iconTint = Tint.WithColor.Fern
    )

    private val expectedMailboxItem = MailboxListItemEntry(
        index = 0,
        avatarInitial = AvatarInitial.WithText("M"),
        participants = listOf(ParticipantEntry.WithParticipant("mobileappsuitesting3")),
        subject = "Move this somewhere else",
        date = "Mar 28, 2023"
    )

    @Test
    @TestId("185418")
    fun checkMoveToBottomSheetSystemToSystemFolder() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_185418.json"
                    withStatusCode 200,
                get("/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=6&Sort=Time&Desc=1")
                    respondWith "/mail/v4/conversations/conversations_empty.json"
                    withStatusCode 200 serveOnce true,
                get("/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=0&Sort=Time&Desc=1")
                    respondWith "/mail/v4/conversations/conversations_185418.json"
                    withStatusCode 200,
                get("/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=6&Sort=Time&Desc=1")
                    respondWith "/mail/v4/conversations/conversations_185418_2.json"
                    withStatusCode 200 serveOnce true,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_185418.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_185418.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.Archive)
        }

        mailboxRobot {
            emptyListSection { verify { isShown() } }
        }

        moveMessageToFolder(
            startingFolder = Inbox,
            destinationFolder = Archive
        )
    }

    @Test
    @TestId("185419")
    fun checkMoveToBottomSheetCustomToCustomFolder() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(
            useDefaultMailSettings = false,
            useDefaultCustomFolders = false,
            useDefaultMailReadResponses = true
        ) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_185419.json"
                    withStatusCode 200,
                get("/core/v4/labels?Type=3")
                    respondWith "/core/v4/labels/labels-type3_185419.json"
                    withStatusCode 200,
                get("/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=0&Sort=Time&Desc=1")
                    respondWith "/mail/v4/conversations/conversations_empty.json"
                    withStatusCode 200,
                get("/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=testid&Sort=Time&Desc=1")
                    respondWith "/mail/v4/conversations/conversations_185419.json"
                    withStatusCode 200,
                get("/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=childid&Sort=Time&Desc=1")
                    respondWith "/mail/v4/conversations/conversations_185419_2.json"
                    withStatusCode 200,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_185419.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_185419.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        moveMessageToFolder(
            startingFolder = firstCustomFolder,
            destinationFolder = secondCustomFolder
        )
    }

    @Test
    @TestId("185419/2", "185421")
    fun checkMoveToBottomSheetCustomToSystemFolder() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(
            useDefaultMailSettings = false,
            useDefaultCustomFolders = false,
            useDefaultMailReadResponses = true
        ) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_185419.json"
                    withStatusCode 200,
                get("/core/v4/labels?Type=3")
                    respondWith "/core/v4/labels/labels-type3_185419.json"
                    withStatusCode 200,
                get("/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=0&Sort=Time&Desc=1")
                    respondWith "/mail/v4/conversations/conversations_empty.json"
                    withStatusCode 200,
                get("/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=testid&Sort=Time&Desc=1")
                    respondWith "/mail/v4/conversations/conversations_185419.json"
                    withStatusCode 200,
                get("/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=6&Sort=Time&Desc=1")
                    respondWith "/mail/v4/conversations/conversations_185419_3.json"
                    withStatusCode 200,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_185419_3.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_185419.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        moveMessageToFolder(
            startingFolder = firstCustomFolder,
            destinationFolder = Archive
        )
    }

    @Test
    @TestId("185419/3", "185423")
    fun checkMoveToBottomSheetSystemToCustomFolder() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(
            useDefaultMailSettings = false,
            useDefaultCustomFolders = false,
            useDefaultMailReadResponses = true
        ) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_185419.json"
                    withStatusCode 200,
                get("/core/v4/labels?Type=3")
                    respondWith "/core/v4/labels/labels-type3_185419.json"
                    withStatusCode 200,
                get("/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=0&Sort=Time&Desc=1")
                    respondWith "/mail/v4/conversations/conversations_empty.json"
                    withStatusCode 200,
                get("/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=6&Sort=Time&Desc=1")
                    respondWith "/mail/v4/conversations/conversations_185419_3.json"
                    withStatusCode 200,
                get("/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=childid&Sort=Time&Desc=1")
                    respondWith "/mail/v4/conversations/conversations_185419_2.json"
                    withStatusCode 200,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_185419_2.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_185419_3.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        moveMessageToFolder(
            startingFolder = Archive,
            destinationFolder = secondCustomFolder
        )
    }

    private fun moveMessageToFolder(
        index: Int = 0,
        startingFolder: MoveToBottomSheetFolderEntry,
        destinationFolder: MoveToBottomSheetFolderEntry
    ) {
        val snackbar = MessageDetailSnackbar.ConversationMovedToFolder(destinationFolder.name)

        menuRobot {
            openSidebarMenu()
            openFolderWithName(startingFolder.name)
        }

        mailboxRobot {
            listSection { clickMessageByPosition(index) }
        }

        conversationDetailRobot {
            messageBodySection { waitUntilMessageIsShown() }
            bottomBarSection { openMoveToBottomSheet() }

            moveToBottomSheetSection {
                selectFolderWithName(destinationFolder.name)
                tapDoneButton()

                verify { isHidden() }
            }

            snackbarSection {
                verify { isDisplaying(snackbar) }
            }
        }

        menuRobot {
            openSidebarMenu()
            openFolderWithName(destinationFolder.name)
        }

        mailboxRobot {
            listSection { verify { listItemsAreShown(expectedMailboxItem) } }
        }
    }
}
