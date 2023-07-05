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

package ch.protonmail.android.uitest.e2e.composer.drafts

import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.test.annotations.suite.TemporaryTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.e2e.composer.ComposerTests
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.login.LoginStrategy
import ch.protonmail.android.uitest.models.avatar.AvatarInitial
import ch.protonmail.android.uitest.models.mailbox.MailboxListItemEntry
import ch.protonmail.android.uitest.models.mailbox.MailboxType
import ch.protonmail.android.uitest.robot.common.section.keyboardSection
import ch.protonmail.android.uitest.robot.composer.composerRobot
import ch.protonmail.android.uitest.robot.composer.section.messageBodySection
import ch.protonmail.android.uitest.robot.composer.section.recipients.toRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.subjectSection
import ch.protonmail.android.uitest.robot.composer.section.topAppBarSection
import ch.protonmail.android.uitest.robot.helpers.mockRobot
import ch.protonmail.android.uitest.robot.helpers.time
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.section.emptyListSection
import ch.protonmail.android.uitest.robot.mailbox.section.listSection
import ch.protonmail.android.uitest.robot.mailbox.section.topAppBarSection
import ch.protonmail.android.uitest.robot.mailbox.section.verify
import ch.protonmail.android.uitest.robot.menu.menuRobot
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Before
import org.junit.Test

@SmokeTest
@HiltAndroidTest
@UninstallModules(ServerProofModule::class)
internal class ComposerDraftsMainTests : MockedNetworkTest(loginStrategy = LoginStrategy.LoggedOut), ComposerTests {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    @Before
    fun navigateToComposer() {
        mockWebServer.dispatcher = composerMockNetworkDispatcher()
        navigator { navigateTo(Destination.Composer) }
    }

    @Test
    @TestId("190295")
    fun testNoDraftSavedUponComposerExit() {
        composerRobot {
            topAppBarSection { tapCloseButton() }
        }

        verifyEmptyDrafts()
    }

    @Test
    @TemporaryTest
    @TestId("190296")
    fun testNoDraftSavedUponEmptyBody() {
        composerRobot {
            toRecipientSection {
                typeRecipient("rec@ipient.com", autoConfirm = true)
            }

            subjectSection {
                typeSubject("Subject!")
            }

            topAppBarSection { tapCloseButton() }
        }

        verifyEmptyDrafts()
    }

    @Test
    @TemporaryTest
    @TestId("190297")
    fun testDraftSavedWhenBodyIsPopulated() {
        createAndVerifyDraft(recipient = null, subject = null)
    }

    @Test
    @TemporaryTest
    @TestId("190298")
    fun testDraftSavedWhenAllFieldsArePopulated() {
        createAndVerifyDraft(recipient = "test@example.com", subject = "A subject")
    }

    private fun verifyEmptyDrafts() {
        menuRobot {
            swipeOpenSidebarMenu()
            openDrafts()
        }

        mailboxRobot {
            topAppBarSection { verify { isMailbox(MailboxType.Drafts) } }
            emptyListSection { verify { isShown() } }
        }
    }


    private fun createAndVerifyDraft(
        recipient: String? = null,
        subject: String? = null,
        body: String = "sample body"
    ) {
        // For now the item is always the same as some of the fields are not populated.
        // Will be extracted once MAILANDR-495 is implemented.
        val expectedDraftItem = MailboxListItemEntry(
            index = 0,
            participants = "(No Recipient)",
            avatarInitial = AvatarInitial.Draft,
            date = "Jul 1, 2023",
            subject = ""
        )

        mockRobot {
            time { forceCurrentMillisTo(1_688_211_755) } // Jul 1st, 2023
        }

        composerRobot {
            recipient?.let {
                toRecipientSection { typeRecipient(it, autoConfirm = true) }
            }

            subject?.let {
                subjectSection { typeSubject(it) }
            }

            messageBodySection {
                typeMessageBody(body)
            }

            keyboardSection { dismissKeyboard() }

            topAppBarSection { tapCloseButton() }
        }

        menuRobot {
            swipeOpenSidebarMenu()
            openDrafts()
        }

        mailboxRobot {
            listSection {
                verify { listItemsAreShown(expectedDraftItem) }
            }
        }
    }
}
