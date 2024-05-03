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

package ch.protonmail.android.uitest.e2e.mailbox.selection

import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.get
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.login.LoginTestUserTypes
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.models.mailbox.MailboxType
import ch.protonmail.android.uitest.robot.bottombar.bottomBarSection
import ch.protonmail.android.uitest.robot.bottombar.verify
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.section.listSection
import ch.protonmail.android.uitest.robot.mailbox.section.topAppBarSection
import ch.protonmail.android.uitest.robot.mailbox.section.verify
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
internal class SelectionModeMainTests : MockedNetworkTest(
    loginType = LoginTestUserTypes.Paid.FancyCapybara
) {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    @Before
    fun prepareTests() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_conversation.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_215427.json"
                    withStatusCode 200 ignoreQueryParams true
            )
        }


        navigator { navigateTo(Destination.Inbox) }
    }

    @Test
    @TestId("254596")
    fun backButtonDismissesSelectionMode() {
        val selectedItemPosition = 0

        mailboxRobot {
            listSection {
                selectItemsAt(selectedItemPosition)
                verify { selectedItemAtPosition(selectedItemPosition) }
            }

            bottomBarSection { verify { isShown() } }

            topAppBarSection {
                verify { isInSelectionMode(numSelected = 1) }

                tapExitSelectionMode()
                verify { isMailbox(MailboxType.Inbox) }
            }

            bottomBarSection { verify { isNotShown() } }
        }
    }

    @Test
    @TestId("215426")
    fun testItemIsSelectedWithLongPress() {
        val selectedItemPosition = 0

        mailboxRobot {
            listSection {
                longPressItemAtPosition(selectedItemPosition)
                verify { selectedItemAtPosition(selectedItemPosition) }
            }

            topAppBarSection {
                verify { isInSelectionMode(numSelected = 1) }
            }

            bottomBarSection { verify { isShown() } }
        }
    }

    @Test
    @TestId("215427", "215428", "215430")
    fun testMultipleSelectedItems() {
        val selectedItemPosition = 0
        val secondSelectedItemPosition = 1
        val unselectedItemPosition = 2
        val secondUnselectedItemPosition = 3

        mailboxRobot {
            listSection {
                selectItemsAt(selectedItemPosition, secondSelectedItemPosition)

                verify {
                    selectedItemAtPosition(selectedItemPosition)
                    selectedItemAtPosition(secondSelectedItemPosition)
                    unSelectedItemAtPosition(unselectedItemPosition)
                    unSelectedItemAtPosition(secondUnselectedItemPosition)
                }
            }

            topAppBarSection {
                verify { isInSelectionMode(numSelected = 2) }
            }

            bottomBarSection { verify { isShown() } }
        }
    }

    @Test
    @TestId("215431")
    fun testSelectionCountIncreases() {
        val selectedItemPosition = 0
        val secondSelectedItemPosition = 1

        mailboxRobot {
            listSection {
                selectItemsAt(selectedItemPosition)
            }

            topAppBarSection {
                verify { isInSelectionMode(numSelected = 1) }
            }

            bottomBarSection { verify { isShown() } }

            listSection {
                selectItemsAt(secondSelectedItemPosition)
            }

            topAppBarSection {
                verify { isInSelectionMode(numSelected = 2) }
            }

            bottomBarSection { verify { isShown() } }
        }
    }

    @Test
    @TestId("215429", "215432", "215433")
    fun testSelectionCountDecreasesAndExitSelectionMode() {
        val selectedItemPosition = 0
        val secondSelectedItemPosition = 1

        mailboxRobot {
            listSection {
                selectItemsAt(selectedItemPosition, secondSelectedItemPosition)
            }

            topAppBarSection {
                verify { isInSelectionMode(numSelected = 2) }
            }

            bottomBarSection { verify { isShown() } }

            listSection {
                unselectItemsAtPosition(secondSelectedItemPosition)
                verify { unSelectedItemAtPosition(secondSelectedItemPosition) }
            }

            topAppBarSection {
                verify { isInSelectionMode(numSelected = 1) }
            }

            listSection {
                unselectItemsAtPosition(selectedItemPosition)
                verify { unSelectedItemAtPosition(selectedItemPosition) }
            }

            topAppBarSection {
                verify { isMailbox(MailboxType.Inbox) }
            }

            bottomBarSection { verify { isNotShown() } }
        }
    }
}
