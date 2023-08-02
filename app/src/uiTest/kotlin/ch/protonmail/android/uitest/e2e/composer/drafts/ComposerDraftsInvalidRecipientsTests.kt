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
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.robot.composer.composerRobot
import ch.protonmail.android.uitest.robot.composer.section.recipients.bccRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.ccRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.toRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.topAppBarSection
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
internal class ComposerDraftsInvalidRecipientsTests : MockedNetworkTest(), ComposerDraftsTests {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    private val invalidEmailAddress = "test@aa"

    @Before
    fun navigateToComposer() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher()
        navigator { navigateTo(Destination.Composer) }
    }

    @Test
    @TestId("207358")
    fun testIncompleteAddressDoesNotTriggerDraftCreation() {
        composerRobot {
            toRecipientSection { typeRecipient(invalidEmailAddress) }

            topAppBarSection { tapCloseButton() }
        }

        verifyEmptyDrafts()
    }

    @Test
    @TestId("207359")
    fun testInvalidToAddressDoesNotTriggerDraftCreation() {
        composerRobot {
            toRecipientSection { typeRecipient(invalidEmailAddress, autoConfirm = true) }

            topAppBarSection { tapCloseButton() }
        }

        verifyEmptyDrafts()
    }

    @Test
    @TestId("207360")
    fun testInvalidCcAddressDoesNotTriggerDraftCreation() {
        composerRobot {
            toRecipientSection { expandCcAndBccFields() }
            ccRecipientSection { typeRecipient(invalidEmailAddress, autoConfirm = true) }

            topAppBarSection { tapCloseButton() }
        }

        verifyEmptyDrafts()
    }

    @Test
    @TestId("207361")
    fun testInvalidBccAddressDoesNotTriggerDraftCreation() {
        composerRobot {
            toRecipientSection { expandCcAndBccFields() }
            bccRecipientSection { typeRecipient(invalidEmailAddress, autoConfirm = true) }

            topAppBarSection { tapCloseButton() }
        }

        verifyEmptyDrafts()
    }

    @Test
    @TestId("207362")
    fun testInvalidAddressesDoNotTriggerDraftCreation() {
        composerRobot {
            toRecipientSection {
                typeRecipient(invalidEmailAddress, autoConfirm = true)
                expandCcAndBccFields()
            }

            ccRecipientSection { typeRecipient(invalidEmailAddress, autoConfirm = true) }
            bccRecipientSection { typeRecipient(invalidEmailAddress, autoConfirm = true) }

            topAppBarSection { tapCloseButton() }
        }

        verifyEmptyDrafts()
    }
}
