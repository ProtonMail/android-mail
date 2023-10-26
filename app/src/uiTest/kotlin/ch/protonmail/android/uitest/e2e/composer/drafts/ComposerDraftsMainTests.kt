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
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.models.mailbox.ParticipantEntry
import ch.protonmail.android.uitest.robot.composer.composerRobot
import ch.protonmail.android.uitest.robot.composer.section.topAppBarSection
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
internal class ComposerDraftsMainTests : MockedNetworkTest(), ComposerDraftsTests {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    private val subject = "A subject!"
    private val messageBody = "sample body"

    @Before
    fun navigateToComposer() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher()
        navigator { navigateTo(Destination.Composer) }
    }

    @Test
    @TestId("190295", "207357")
    fun testNoDraftSavedUponComposerExit() {
        composerRobot { topAppBarSection { tapCloseButton() } }
        verifyEmptyDrafts()
    }

    @Test
    @TestId("190296", "207367")
    fun testDraftSavedWithSubjectOnlyUponEmptyBody() {
        composerRobot {
            prepareDraft(toRecipients = emptyList(), subject = subject, body = null)
            topAppBarSection { tapCloseButton() }
        }

        verifyDraftCreation(ParticipantEntry.NoRecipient, subject = subject)
    }

    @Test
    @TestId("190297")
    fun testDraftSavedWhenBodyIsPopulated() {
        composerRobot {
            prepareDraft(toRecipients = emptyList(), body = "sample body")
            topAppBarSection { tapCloseButton() }
        }

        verifyDraftCreation(ParticipantEntry.NoRecipient, body = messageBody)
    }

    @Test
    @TestId("190298")
    fun testDraftSavedWhenAllFieldsArePopulated() {
        val participant = "test@example.com"

        composerRobot {
            prepareDraft(toRecipient = participant, subject = subject, body = messageBody)
            topAppBarSection { tapCloseButton() }
        }

        verifyDraftCreation(participant, subject = subject, body = messageBody)
    }
}
