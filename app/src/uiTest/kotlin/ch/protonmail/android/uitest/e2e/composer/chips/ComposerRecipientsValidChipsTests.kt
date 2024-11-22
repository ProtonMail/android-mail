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
import ch.protonmail.android.uitest.robot.composer.section.recipients.bccRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.ccRecipientSection
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
internal class ComposerRecipientsValidChipsTests : MockedNetworkTest(), ComposerChipsTests {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    private val expectedRecipientChipEntry = RecipientChipEntry(
        index = 0,
        text = "rec@ipient.com",
        state = RecipientChipValidationState.Valid
    )

    @Before
    fun navigateToComposer() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher()
        navigator { navigateTo(Destination.Composer) }
    }

    @Test
    @SmokeTest
    @TestId("190228")
    fun testValidToRecipientChip() {
        composerRobot {
            toRecipientSection {
                createAndVerifyValidChip()
            }
        }
    }

    @Test
    @TestId("190229")
    fun testValidCcRecipientChip() {
        composerRobot {
            toRecipientSection {
                expandCcAndBccFields()
            }

            ccRecipientSection {
                createAndVerifyValidChip()
            }
        }
    }

    @Test
    @TestId("190230")
    fun testValidBccRecipientChip() {
        composerRobot {
            toRecipientSection {
                expandCcAndBccFields()
            }

            bccRecipientSection {
                createAndVerifyValidChip()
            }
        }
    }

    @Test
    @TestId("190231")
    fun testValidRecipientChipOnFocusChange() {
        composerRobot {
            toRecipientSection {
                expandCcAndBccFields()
                typeRecipient("rec@ipient.com")
            }

            ccRecipientSection {
                tapRecipientField()
            }

            toRecipientSection {
                verify { hasRecipientChips(expectedRecipientChipEntry) }
            }
        }
    }

    @Test
    @TestId("190236")
    fun testValidRecipientChipOnNewLine() {
        composerRobot {
            toRecipientSection {
                createAndVerifyValidChip(trigger = ChipsCreationTrigger.NewLine)
            }
        }
    }

    @Test
    @TestId("190237")
    fun testMultipleValidRecipientChipsOnNewLine() {
        composerRobot {
            toRecipientSection {
                withMultipleRecipients(size = 100, state = RecipientChipValidationState.Valid) {
                    typeRecipient(it.text, autoConfirm = true)

                    verify { hasRecipientChips(it) }
                }
            }
        }
    }

    private fun ComposerRecipientsSection.createAndVerifyValidChip(
        trigger: ChipsCreationTrigger = ChipsCreationTrigger.ImeAction
    ) = createAndVerifyChip(state = RecipientChipValidationState.Valid, trigger)
}
