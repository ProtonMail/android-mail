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
import ch.protonmail.android.uitest.robot.composer.model.chips.RecipientChipEntry
import ch.protonmail.android.uitest.robot.composer.model.chips.RecipientChipValidationState
import ch.protonmail.android.uitest.robot.composer.section.recipients.bccRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.ccRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.toRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.verify
import ch.protonmail.android.uitest.robot.composer.section.subjectSection
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
internal class ComposerRecipientsCollapsedChipsTests : MockedNetworkTest(), ComposerChipsTests {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    private val expectedPlusOneEntry = RecipientChipEntry(
        index = 1,
        text = "+1",
        hasDeleteIcon = false,
        state = RecipientChipValidationState.Valid
    )

    private val expectedMultipleFocusedEntries = arrayOf(
        RecipientChipEntry(
            index = 0,
            text = "test@example.com",
            hasDeleteIcon = true,
            state = RecipientChipValidationState.Valid
        ),
        RecipientChipEntry(
            index = 1,
            text = "test2@example.com",
            hasDeleteIcon = true,
            state = RecipientChipValidationState.Valid
        ),
        RecipientChipEntry(
            index = 2,
            text = "test3@example.com",
            hasDeleteIcon = true,
            state = RecipientChipValidationState.Valid
        )
    )

    @Before
    fun navigateToComposer() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher()
        navigator { navigateTo(Destination.Composer) }
    }

    @Test
    @SmokeTest
    @TestId("190243")
    fun testMoveFocusFromToFieldCollapseChips() {
        composerRobot {
            toRecipientSection {
                typeMultipleRecipients("recipient1@example.com", "recipient2@example.com")
            }

            subjectSection { focusField() }

            toRecipientSection {
                verify { hasRecipientChips(expectedPlusOneEntry) }
            }
        }
    }

    @Test
    @TestId("190244")
    fun testMoveFocusFromCcFieldCollapseChips() {
        composerRobot {
            toRecipientSection {
                expandCcAndBccFields()
            }

            ccRecipientSection {
                typeMultipleRecipients("recipient1@example.com", "recipient2@example.com")
            }

            subjectSection { focusField() }

            ccRecipientSection {
                verify { hasRecipientChips(expectedPlusOneEntry) }
            }
        }
    }

    @Test
    @TestId("190245")
    fun testMoveFocusFromBccFieldCollapseChips() {
        composerRobot {
            toRecipientSection {
                expandCcAndBccFields()
            }

            bccRecipientSection {
                typeMultipleRecipients("recipient1@example.com", "recipient2@example.com")
            }

            subjectSection { focusField() }

            bccRecipientSection {
                verify { hasRecipientChips(expectedPlusOneEntry) }
            }
        }
    }

    @Test
    @SmokeTest
    @TestId("190246")
    fun testExpandedChipsRestoreRecipientsContents() {
        val unfocusedEntries = arrayOf(
            RecipientChipEntry(
                index = 0,
                text = "test@example.com",
                hasDeleteIcon = false,
                state = RecipientChipValidationState.Valid
            ),
            RecipientChipEntry(
                index = 1,
                text = "+2",
                hasDeleteIcon = false,
                state = RecipientChipValidationState.Valid
            )
        )

        composerRobot {
            toRecipientSection {
                typeMultipleRecipients("test@example.com", "test2@example.com", "test3@example.com")

                verify { hasRecipientChips(*expectedMultipleFocusedEntries) }
            }

            subjectSection { focusField() }

            toRecipientSection {
                verify { hasRecipientChips(*unfocusedEntries) }

                focusField()

                verify { hasRecipientChips(*expectedMultipleFocusedEntries) }
            }
        }
    }

    @Test
    @TestId("190247")
    fun testChipAdditionAfterFieldExpansion() {
        composerRobot {
            toRecipientSection {
                typeMultipleRecipients("test@example.com", "test2@example.com")
            }

            subjectSection { focusField() }

            toRecipientSection {
                typeRecipient("test3@example.com", autoConfirm = true)

                verify { hasRecipientChips(*expectedMultipleFocusedEntries) }
            }
        }
    }
}
