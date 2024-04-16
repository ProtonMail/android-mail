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

package ch.protonmail.android.uitest.e2e.account

import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.robot.account.section.buttonsSection
import ch.protonmail.android.uitest.robot.account.signOutAccountDialogRobot
import ch.protonmail.android.uitest.robot.account.verify
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.verify
import ch.protonmail.android.uitest.robot.menu.menuRobot
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
internal class SignOutAccountTest : MockedNetworkTest() {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    @Before
    fun navigateToLogout() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher()
        navigator { navigateTo(Destination.SidebarMenu) }
        menuRobot { tapSignOut() }
    }

    @Test
    @TestId("256595")
    fun testSignOutIsPerformedOnDialogConfirmationWhenSingleAccountLoggedIn() {
        signOutAccountDialogRobot {
            buttonsSection { tapSignOut() }
            // Do not call isNotShown() here as it transitions to an external non-compose screen.
        }

        addAccountRobot {
            verify { isDisplayed() }
        }
    }

    @Test
    @TestId("256596")
    fun testSignOutIsNotPerformedOnDialogCancellationWhenSingleAccountLoggedIn() {
        signOutAccountDialogRobot {
            buttonsSection { tapCancel() }
            verify { isNotShown() }
        }

        mailboxRobot {
            verify { isShown() }
        }
    }
}
