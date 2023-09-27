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
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.robot.composer.composerRobot
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
internal class ComposerDraftsValidRecipientsTests : MockedNetworkTest(), ComposerDraftsTests {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    private val validToRecipient = "a@b.c"
    private val validCcRecipient = "d@e.f"
    private val validBccRecipient = "g@h.i"

    @Before
    fun navigateToComposer() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher()
        navigator { navigateTo(Destination.Composer) }
    }

    @Test
    @SmokeTest
    @TestId("207363")
    fun testValidToAddressDoesTriggerDraftCreation() {
        composerRobot {
            prepareDraft(toRecipients = listOf(validToRecipient))
            topAppBarSection { tapCloseButton() }
        }

        verifyDraftCreation(validToRecipient)
    }

    @Test
    @TestId("207364")
    fun testValidCcAddressDoesTriggerDraftCreation() {
        composerRobot {
            prepareDraft(ccRecipients = listOf(validCcRecipient))
            topAppBarSection { tapCloseButton() }
        }

        verifyDraftCreation(validCcRecipient)
    }

    @Test
    @TestId("207365")
    fun testValidBccAddressDoesTriggerDraftCreation() {
        composerRobot {
            prepareDraft(bccRecipients = listOf(validBccRecipient))
            topAppBarSection { tapCloseButton() }
        }

        verifyDraftCreation(validBccRecipient)
    }

    @Test
    @TestId("207366")
    fun testValidAddressesDoTriggerDraftCreation() {
        composerRobot {
            prepareDraft(
                toRecipients = listOf(validToRecipient),
                ccRecipients = listOf(validCcRecipient),
                bccRecipients = listOf(validBccRecipient)
            )

            topAppBarSection { tapCloseButton() }
        }

        verifyDraftCreation(validToRecipient, validCcRecipient, validBccRecipient, subject = "", body = "")
    }
}
