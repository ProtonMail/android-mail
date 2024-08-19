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
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
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
import ch.protonmail.android.uitest.models.folders.Tint
import ch.protonmail.android.uitest.robot.detail.conversationDetailRobot
import ch.protonmail.android.uitest.robot.detail.model.bottomsheet.MoveToBottomSheetFolderEntry
import ch.protonmail.android.uitest.robot.detail.section.bottomBarSection
import ch.protonmail.android.uitest.robot.detail.section.messageBodySection
import ch.protonmail.android.uitest.robot.detail.section.moveToBottomSheetSection
import ch.protonmail.android.uitest.robot.detail.section.verify
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Test

@RegressionTest
@HiltAndroidTest
@UninstallModules(ServerProofModule::class)
internal class DetailMoveToBottomSheetMainTests : MockedNetworkTest() {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    private val firstCustomFolder = MoveToBottomSheetFolderEntry(
        index = 0, name = "Test Folder", iconTint = Tint.WithColor.Carrot
    )
    private val secondCustomFolder = MoveToBottomSheetFolderEntry(
        index = 1, name = "Child Test Folder", iconTint = Tint.WithColor.Fern
    )
    private val systemFolders = arrayOf(
        MoveToBottomSheetFolderEntry.SystemFolders.Inbox,
        MoveToBottomSheetFolderEntry.SystemFolders.Archive,
        MoveToBottomSheetFolderEntry.SystemFolders.Spam,
        MoveToBottomSheetFolderEntry.SystemFolders.Trash
    )

    @Test
    @TestId("185411")
    fun checkMoveToBottomSheetComponentsWithNoCustomFolders() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(
            useDefaultMailSettings = false,
            useDefaultCustomFolders = false
        ) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_185411.json"
                    withStatusCode 200,
                get("/core/v4/labels?Type=3")
                    respondWith "/core/v4/labels/labels-type3_base_placeholder_empty.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_base_placeholder.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_base_placeholder.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_185411.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.MailDetail(0))
        }

        conversationDetailRobot {
            messageBodySection { waitUntilMessageIsShown() }
            bottomBarSection { openMoveToBottomSheet() }

            moveToBottomSheetSection {
                verify { hasFolders(*systemFolders) }
            }
        }
    }

    @Test
    @TestId("185412")
    fun checkMoveToBottomSheetComponentsWithCustomFolders() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(
            useDefaultMailSettings = false,
            useDefaultCustomFolders = false,
            useDefaultMailReadResponses = true
        ) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_185412.json"
                    withStatusCode 200,
                get("/core/v4/labels?Type=3")
                    respondWith "/core/v4/labels/labels-type3_185412.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_base_placeholder.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_base_placeholder.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_185412.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        val expectedFolders = arrayOf(firstCustomFolder, secondCustomFolder).combineWithSystemFolders()

        navigator {
            navigateTo(Destination.MailDetail(0))
        }

        conversationDetailRobot {
            messageBodySection { waitUntilMessageIsShown() }
            bottomBarSection { openMoveToBottomSheet() }

            moveToBottomSheetSection {
                verify { hasFolders(*expectedFolders) }
            }
        }
    }

    @Test
    @TestId("185414")
    fun checkMoveToBottomSheetComponentsSelection() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(
            useDefaultMailSettings = false,
            useDefaultCustomFolders = false
        ) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_185414.json"
                    withStatusCode 200,
                get("/core/v4/labels?Type=3")
                    respondWith "/core/v4/labels/labels-type3_base_placeholder_empty.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_base_placeholder.json"
                    withStatusCode 200 ignoreQueryParams true serveOnce true,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_base_placeholder.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_185414.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        val expectedSelectedFolders = systemFolders.map {
            if (it == MoveToBottomSheetFolderEntry.SystemFolders.Trash) it.copy(isSelected = true) else it
        }.toTypedArray()

        navigator {
            navigateTo(Destination.MailDetail(0))
        }

        conversationDetailRobot {
            messageBodySection { waitUntilMessageIsShown() }
            bottomBarSection { openMoveToBottomSheet() }

            moveToBottomSheetSection {
                selectFolderAtPosition(MoveToBottomSheetFolderEntry.SystemFolders.Trash.index)
                verify { hasFolders(*expectedSelectedFolders) }

                tapDoneButton()
                verify { isHidden() }
            }
        }
    }

    @Test
    @TestId("185415")
    fun checkMoveToBottomSheetSelectionIsGoneAfterDismissal() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(
            useDefaultMailSettings = false,
            useDefaultCustomFolders = false
        ) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_185415.json"
                    withStatusCode 200,
                get("/core/v4/labels?Type=3")
                    respondWith "/core/v4/labels/labels-type3_base_placeholder_empty.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_base_placeholder.json"
                    withStatusCode 200 ignoreQueryParams true serveOnce true,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_base_placeholder.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_185415.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        val expectedSelectedFolders = systemFolders.map {
            if (it == MoveToBottomSheetFolderEntry.SystemFolders.Trash) it.copy(isSelected = true) else it
        }.toTypedArray()

        navigator {
            navigateTo(Destination.MailDetail(0))
        }

        conversationDetailRobot {
            messageBodySection { waitUntilMessageIsShown() }
            bottomBarSection { openMoveToBottomSheet() }

            moveToBottomSheetSection {
                selectFolderAtPosition(MoveToBottomSheetFolderEntry.SystemFolders.Trash.index)
                verify { hasFolders(*expectedSelectedFolders) }
                dismiss()
            }

            bottomBarSection { openMoveToBottomSheet() }

            moveToBottomSheetSection {
                verify { hasFolders(*systemFolders) }
            }
        }
    }

    private fun Array<MoveToBottomSheetFolderEntry>.combineWithSystemFolders(): Array<MoveToBottomSheetFolderEntry> {
        return systemFolders.map {
            it.copy(index = size + it.index)
        }.toTypedArray()
    }
}
