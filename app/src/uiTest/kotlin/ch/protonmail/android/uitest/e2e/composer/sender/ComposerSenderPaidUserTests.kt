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

package ch.protonmail.android.uitest.e2e.composer.sender

import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.MockPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.get
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.withPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.e2e.composer.ComposerTests
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.login.LoginTestUserTypes
import ch.protonmail.android.uitest.robot.common.section.keyboardSection
import ch.protonmail.android.uitest.robot.composer.composerRobot
import ch.protonmail.android.uitest.robot.composer.model.sender.ChangeSenderEntry
import ch.protonmail.android.uitest.robot.composer.section.changeSenderBottomSheet
import ch.protonmail.android.uitest.robot.composer.section.messageBodySection
import ch.protonmail.android.uitest.robot.composer.section.senderSection
import ch.protonmail.android.uitest.robot.composer.section.topAppBarSection
import ch.protonmail.android.uitest.robot.composer.section.verify
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.section.topAppBarSection
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Test

@RegressionTest
@HiltAndroidTest
@UninstallModules(ServerProofModule::class)
internal class ComposerSenderPaidUserTests :
    MockedNetworkTest(loginType = LoginTestUserTypes.Paid.FancyCapybara),
    ComposerTests {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    private val expectedPrimaryAddress = "fancycapybara@proton.black"

    @Test
    @TestId("192115")
    fun testMainSenderForPaidUser() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher()

        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            senderSection { verify { hasValue(expectedPrimaryAddress) } }
        }
    }

    @Test
    @SmokeTest
    @TestId("192117", "192119", "192123")
    fun testMultipleAliasForPaidUser() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher {
            addMockRequests(
                get("/core/v4/addresses")
                    respondWith "/core/v4/addresses/addresses_192117.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        val expectedEntries = arrayOf(
            ChangeSenderEntry(index = 0, address = "fancycapybara@proton.black"),
            ChangeSenderEntry(index = 1, address = "shortcapybara@pm.me.proton.black"),
            ChangeSenderEntry(index = 2, address = "fancycapybara@pm.me.proton.black"),
            ChangeSenderEntry(index = 3, address = "fancynotenabled@proton.black", isEnabled = false)
        )

        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            senderSection { verify { hasValue(expectedPrimaryAddress) } }

            senderSection { tapChangeSender() }
            changeSenderBottomSheet {
                verify { hasEntries(*expectedEntries) }
            }
        }
    }

    @Test
    @TestId("192122")
    fun testPrimaryAddressIsStillDefaultAfterDraftIsSaved() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher {
            addMockRequests(
                get("/core/v4/addresses")
                    respondWith "/core/v4/addresses/addresses_192122.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        val expectedSecondaryAddress = "shortcapybara@pm.me.proton.black"

        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            senderSection { verify { hasValue(expectedPrimaryAddress) } }

            senderSection { tapChangeSender() }
            changeSenderBottomSheet { tapEntryAt(position = 1) }
            senderSection { verify { hasValue(expectedSecondaryAddress) } }

            messageBodySection { typeMessageBody("Test value") }
            keyboardSection { dismissKeyboard() }
            topAppBarSection { tapCloseButton() }
        }

        mailboxRobot {
            topAppBarSection { tapComposerIcon() }
        }

        composerRobot {
            senderSection { verify { hasValue(expectedPrimaryAddress) } }
        }
    }

    @Test
    @TestId("192125")
    fun testSenderBottomSheetSwipeDismissal() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher()

        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            keyboardSection { dismissKeyboard() }
            senderSection { tapChangeSender() }

            changeSenderBottomSheet {
                dismiss()
                verify { isHidden() }
            }

            senderSection { verify { hasValue(expectedPrimaryAddress) } }
        }
    }

    @Test
    @TestId("192126")
    fun testSenderBottomSheetTapDismissal() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher()

        navigator { navigateTo(Destination.Composer) }

        composerRobot {
            keyboardSection { dismissKeyboard() }
            senderSection { tapChangeSender() }
            changeSenderBottomSheet { verify { isShown() } }

            senderSection { tapChangeSender() }
            changeSenderBottomSheet { verify { isHidden() } }
            senderSection { verify { hasValue(expectedPrimaryAddress) } }
        }
    }
}
