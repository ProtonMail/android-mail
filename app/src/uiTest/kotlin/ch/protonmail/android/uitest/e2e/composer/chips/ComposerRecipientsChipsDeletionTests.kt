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

package ch.protonmail.android.uitest.e2e.composer.chips

import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.robot.composer.composerRobot
import ch.protonmail.android.uitest.robot.composer.model.chips.ChipsCreationTrigger
import ch.protonmail.android.uitest.robot.composer.model.chips.RecipientChipEntry
import ch.protonmail.android.uitest.robot.composer.model.chips.RecipientChipValidationState
import ch.protonmail.android.uitest.robot.composer.section.recipients.ComposerRecipientsSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.toRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.verify
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
internal class ComposerRecipientsChipsDeletionTests : MockedNetworkTest(), ComposerChipsTests {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    @Before
    fun navigateToComposer() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher()
        navigator { navigateTo(Destination.Composer) }
    }

    @Test
    @SmokeTest
    @TestId("190261")
    fun testValidChipDeletion() {
        val expectedValidChip = RecipientChipEntry(
            index = 0,
            text = "delete@me.com",
            hasDeleteIcon = true,
            state = RecipientChipValidationState.Valid
        )

        composerRobot {
            toRecipientSection {
                validateSimpleChipCreationAndDeletion(expectedValidChip)
            }
        }
    }

    @Test
    @SmokeTest
    @TestId("190262")
    fun testInvalidChipDeletion() {
        val expectedInvalidChip = RecipientChipEntry(
            index = 0,
            text = "deleteme.com",
            hasDeleteIcon = true,
            state = RecipientChipValidationState.Invalid
        )

        composerRobot {
            toRecipientSection {
                validateSimpleChipCreationAndDeletion(expectedInvalidChip)
            }
        }
    }

    @Test
    @TestId("190263")
    fun testMultipleChipsLastChipDeletion() {
        val expectedFinalChips = arrayOf(
            RecipientChipEntry(
                index = 0,
                text = "one",
                hasDeleteIcon = true,
                state = RecipientChipValidationState.Invalid
            ),
            RecipientChipEntry(
                index = 1,
                text = "two@test.com",
                hasDeleteIcon = true,
                state = RecipientChipValidationState.Valid
            )
        )

        val expectedChip = RecipientChipEntry(
            index = 2,
            text = "delete@me.com",
            hasDeleteIcon = true,
            state = RecipientChipValidationState.Valid
        )

        composerRobot {
            toRecipientSection {
                typeMultipleRecipients(expectedFinalChips[0].text, expectedFinalChips[1].text)
                validateSimpleChipCreationAndDeletion(expectedChip)
                verify { hasRecipientChips(*expectedFinalChips) }
            }
        }
    }

    @Test
    @TestId("190264")
    fun testMultipleChipsFirstChipDeletion() {
        val toBeDeletedChipText = "additional@chip.com"
        val expectedFinalChips = arrayOf(
            RecipientChipEntry(
                index = 0,
                text = "one",
                hasDeleteIcon = true,
                state = RecipientChipValidationState.Invalid
            ),
            RecipientChipEntry(
                index = 1,
                text = "two@test.com",
                hasDeleteIcon = true,
                state = RecipientChipValidationState.Valid
            )
        )

        composerRobot {
            toRecipientSection {
                typeMultipleRecipients(toBeDeletedChipText, expectedFinalChips[0].text, expectedFinalChips[1].text)
                deleteChipAt(position = 0)
                verify { hasRecipientChips(*expectedFinalChips) }
            }
        }
    }

    @Test
    @TestId("190265")
    fun testMultipleChipsMiddleChipDeletion() {
        val toBeDeletedChipText = "additional@chip.com"
        val expectedFinalChips = arrayOf(
            RecipientChipEntry(
                index = 0,
                text = "one",
                hasDeleteIcon = true,
                state = RecipientChipValidationState.Invalid
            ),
            RecipientChipEntry(
                index = 1,
                text = "two@test.com",
                hasDeleteIcon = true,
                state = RecipientChipValidationState.Valid
            )
        )

        composerRobot {
            toRecipientSection {
                typeMultipleRecipients(expectedFinalChips[0].text, toBeDeletedChipText, expectedFinalChips[1].text)
                deleteChipAt(position = 1)

                verify { hasRecipientChips(*expectedFinalChips) }
            }
        }
    }

    @Test
    @TestId("190266")
    fun testAllChipsDeletion() {
        val rawRecipients = arrayOf("test1@example.com", "test2@example.com", "test3@example.com")

        composerRobot {
            toRecipientSection {
                typeMultipleRecipients(*rawRecipients)

                for (index in 1..rawRecipients.size) {
                    deleteChipAt(rawRecipients.size - index)
                }

                verify { isEmptyField() }
            }
        }
    }

    @Test
    @TestId("190269")
    fun testValidChipDeletionWithBackspace() {
        composerRobot {
            toRecipientSection {
                typeRecipient("rec@ipient.com")
                triggerChipCreation(ChipsCreationTrigger.NewLine)
                tapBackspace()

                verify { isEmptyField() }
            }
        }
    }

    @Test
    @TestId("190270")
    fun testInvalidChipDeletionWithBackspace() {
        composerRobot {
            toRecipientSection {
                typeRecipient("example.com")
                triggerChipCreation(ChipsCreationTrigger.NewLine)
                tapBackspace()

                verify { isEmptyField() }
            }
        }
    }

    @Test
    @TestId("190271")
    fun testMultipleValidChipsDeletionWithBackspace() {
        val rawRecipients = arrayOf("rec@ipient.com", "rec@ipient1.com", "rec@ipient2.com")

        composerRobot {
            toRecipientSection {
                validateChipsCreationAndDeletionWithBackspace(rawRecipients)
            }
        }
    }

    @Test
    @TestId("190272")
    fun testMultipleInvalidChipsDeletionWithBackspace() {
        val rawRecipients = arrayOf("recipient.com", "recipient1.com", "recipient2.com")

        composerRobot {
            toRecipientSection {
                validateChipsCreationAndDeletionWithBackspace(rawRecipients)
            }
        }
    }

    @Test
    @TestId("190273")
    fun testChipDeletionAndRecreation() {
        composerRobot {
            toRecipientSection {
                validateChipDeletionAndRecreation { deleteChipAt(position = 1) }
            }
        }
    }

    @Test
    @TestId("190274")
    fun testChipDeletionAndRecreationWithBackspace() {
        composerRobot {
            toRecipientSection {
                validateChipDeletionAndRecreation { tapBackspace() }
            }
        }
    }

    private fun ComposerRecipientsSection.validateSimpleChipCreationAndDeletion(chipEntry: RecipientChipEntry) {
        typeRecipient(chipEntry.text)
        triggerChipCreation(ChipsCreationTrigger.NewLine)
        verify { hasRecipientChips(chipEntry) }

        deleteChipAt(chipEntry.index)
        verify {
            recipientChipIsNotDisplayed(chipEntry)
            isFieldFocused()
        }
    }

    private fun ComposerRecipientsSection.validateChipsCreationAndDeletionWithBackspace(rawRecipients: Array<String>) {
        typeMultipleRecipients(*rawRecipients)
        repeat(times = rawRecipients.size) { tapBackspace() }

        verify {
            isFieldFocused()
            isEmptyField()
        }
    }

    private fun ComposerRecipientsSection.validateChipDeletionAndRecreation(
        deleteAction: ComposerRecipientsSection.() -> Unit
    ) {
        val expectedChips = arrayOf(
            RecipientChipEntry(
                index = 0,
                text = "rec@ipient.com",
                hasDeleteIcon = true,
                state = RecipientChipValidationState.Valid
            ),
            RecipientChipEntry(
                index = 1,
                text = "rec@ipient2.com",
                hasDeleteIcon = true,
                state = RecipientChipValidationState.Valid
            )
        )

        typeMultipleRecipients("rec@ipient.com", "rec@ipient0.com")
        deleteAction()
        typeRecipient("rec@ipient2.com", autoConfirm = true)

        verify { hasRecipientChips(*expectedChips) }
    }
}
