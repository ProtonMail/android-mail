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

package ch.protonmail.android.uitest.e2e.composer.sending

import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.e2e.composer.ComposerTests
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.robot.composer.composerRobot
import ch.protonmail.android.uitest.robot.composer.section.composerAlertDialogSection
import ch.protonmail.android.uitest.robot.composer.section.messageBodySection
import ch.protonmail.android.uitest.robot.composer.section.recipients.toRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.subjectSection
import ch.protonmail.android.uitest.robot.composer.section.topAppBarSection
import ch.protonmail.android.uitest.robot.composer.section.verify
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Before
import org.junit.Test

@RegressionTest
@HiltAndroidTest
@UninstallModules(ServerProofModule::class)
internal class ComposerSendButtonTests : MockedNetworkTest(), ComposerTests {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    private val placeholderString = "Random text"

    @Before
    fun setMockDispatcher() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher()
    }

    @Test
    @SmokeTest
    @TestId("216681")
    fun checkComposerSendButtonDisabledUponOpening() {
        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            topAppBarSection { verify { isSendButtonDisabled() } }
        }
    }

    @Test
    @TestId("216682")
    fun checkComposerSendButtonDisabledUponOnlySubjectAdded() {
        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            subjectSection { typeSubject(placeholderString) }

            topAppBarSection { verify { isSendButtonDisabled() } }
        }
    }

    @Test
    @TestId("216683")
    fun checkComposerSendButtonDisabledUponOnlyMessageBodyAdded() {
        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            messageBodySection { typeMessageBody(placeholderString) }

            topAppBarSection { verify { isSendButtonDisabled() } }
        }
    }

    @Test
    @TestId("216684")
    fun checkComposerSendButtonDisabledUponSubjectAndMessageBodyAdded() {
        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            subjectSection { typeSubject(placeholderString) }
            messageBodySection { typeMessageBody(placeholderString) }

            topAppBarSection { verify { isSendButtonDisabled() } }
        }
    }

    @Test
    @TestId("216685")
    fun checkComposerSendButtonDisabledUponPressingBackspaceInRecipientFields() {
        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            toRecipientSection { tapBackspace() }
            topAppBarSection { verify { isSendButtonDisabled() } }
        }
    }

    @Test
    @TestId("216686")
    fun checkComposerSendButtonDisabledUponAddingAndRemovingRecipients() {
        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            toRecipientSection { typeRecipient("someone@proton.me", autoConfirm = true) }
            topAppBarSection { verify { isSendButtonEnabled() } }

            toRecipientSection { deleteChipAt(0) }
            topAppBarSection { verify { isSendButtonDisabled() } }
        }
    }

    @Test
    @TestId("216687")
    fun checkComposerSendButtonDisabledUponAddingAndDeletingInvalidRecipient() {
        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            toRecipientSection {
                typeRecipient("proton.me", autoConfirm = true)
                deleteChipAt(0)
            }

            topAppBarSection { verify { isSendButtonDisabled() } }
        }
    }

    @Test
    @TestId("216688")
    fun checkComposerSendButtonDisabledUponAddingAnInvalidRecipient() {
        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            toRecipientSection { typeRecipient("proton.me", autoConfirm = true) }

            topAppBarSection { verify { isSendButtonDisabled() } }
        }
    }

    @Test
    @TestId("216689")
    fun checkComposerSendButtonDisabledUponAddingMultipleRecipientsWithOneInvalid() {
        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            toRecipientSection { typeMultipleRecipients("proton.me", "test@proton.me") }

            topAppBarSection { verify { isSendButtonDisabled() } }
        }
    }

    @Test
    @TestId("268527")
    fun checkConfirmationDialogIsShownWhenSubjectIsEmptyAndSendButtonClicked() {
        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            toRecipientSection { typeRecipient("test@proton.me") }
            messageBodySection { typeMessageBody(placeholderString) }

            topAppBarSection { verify { isSendButtonEnabled() } }
            topAppBarSection { tapSendButton() }

            composerAlertDialogSection {
                verify {
                    isSendWithEmptySubjectDialogDisplayed()
                }

                clickSendWithEmptySubjectDialogDismissButton()

                verify {
                    isSendWithEmptySubjectDialogDismissed()
                }
            }

            subjectSection {
                verify { hasEmptySubject() }

                verify { hasFocus() }
            }
        }
    }
}
