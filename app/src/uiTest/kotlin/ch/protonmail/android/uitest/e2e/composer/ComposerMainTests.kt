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

package ch.protonmail.android.uitest.e2e.composer

import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.MockPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.given
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.withPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.test.annotations.suite.TemporaryTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.models.snackbar.SnackbarTextEntry
import ch.protonmail.android.uitest.robot.common.section.keyboardSection
import ch.protonmail.android.uitest.robot.common.section.snackbarSection
import ch.protonmail.android.uitest.robot.common.section.verify
import ch.protonmail.android.uitest.robot.composer.composerRobot
import ch.protonmail.android.uitest.robot.composer.section.messageBodySection
import ch.protonmail.android.uitest.robot.composer.section.recipients.bccRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.ccRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.toRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.verify
import ch.protonmail.android.uitest.robot.composer.section.senderSection
import ch.protonmail.android.uitest.robot.composer.section.subjectSection
import ch.protonmail.android.uitest.robot.composer.section.topAppBarSection
import ch.protonmail.android.uitest.robot.composer.section.verify
import ch.protonmail.android.uitest.robot.composer.verify
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.section.topAppBarSection
import ch.protonmail.android.uitest.robot.mailbox.verify
import ch.protonmail.android.uitest.util.UiDeviceHolder.uiDevice
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Test

@RegressionTest
@HiltAndroidTest
@UninstallModules(ServerProofModule::class)
internal class ComposerMainTests : MockedNetworkTest(), ComposerTests {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    @TemporaryTest
    @Test
    @TestId("79034")
    fun checkNavigationToComposerIsDisabledWhenFeatureToggleIsEnabled() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher {
            addMockRequests(
                given("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                given("/core/v4/features?Code=HideComposerAndroid&Type=boolean")
                    respondWith "/core/v4/features/composer/hide_composer_enabled.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        mailboxRobot {
            topAppBarSection { tapComposerIcon() }

            snackbarSection {
                verify { hasMessage(SnackbarTextEntry.FeatureComingSoon) }
            }
        }
    }

    @TemporaryTest
    @Test
    @TestId("79035")
    fun checkNavigationToComposerIsEnabledWhenFeatureToggleIsDisabled() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher {
            addMockRequests(
                given("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                given("/core/v4/features?Code=HideComposerAndroid&Type=boolean")
                    respondWith "/core/v4/features/composer/hide_composer_disabled.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        mailboxRobot {
            topAppBarSection { tapComposerIcon() }
        }

        composerRobot {
            verify { composerIsShown() }
        }
    }

    @Test
    @TestId("79036")
    fun checkComposerMainFieldsAndInteractions() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher {
            addMockRequests(
                given("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                given("/core/v4/features?Code=HideComposerAndroid&Type=boolean")
                    respondWith "/core/v4/features/composer/hide_composer_disabled.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        val expectedSender = "mobile.apps.uitesting@proton.black"
        val expectedRecipient = "Recipient"
        val expectedSubject = "Subject"
        val expectedBody = "Text message"

        navigator {
            navigateTo(Destination.Inbox)
        }

        mailboxRobot {
            topAppBarSection { tapComposerIcon() }
        }

        composerRobot {
            verify { composerIsShown() }

            // Sender field
            senderSection {
                verify { hasValue(expectedSender) }
            }

            keyboardSection {
                verify { keyboardIsShown() }
            }

            // Recipient field
            toRecipientSection {
                verify {
                    isFieldFocused()
                    isEmptyField()
                }

                typeRecipient(expectedRecipient)
                verify { hasRecipient(expectedRecipient) }
            }

            // Subject field
            subjectSection {
                verify { hasEmptySubject() }
                typeSubject(expectedSubject)
                verify { hasSubject(expectedSubject) }
            }

            // Message body field
            messageBodySection {
                verify { hasPlaceholderText() }
                typeMessageBody(expectedBody)
                verify { hasText(expectedBody) }
            }
        }
    }

    @Test
    @TestId("79037")
    fun checkComposerCloseNavigation() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher {
            addMockRequests(
                given("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                given("/core/v4/features?Code=HideComposerAndroid&Type=boolean")
                    respondWith "/core/v4/features/composer/hide_composer_disabled.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        mailboxRobot {
            topAppBarSection { tapComposerIcon() }
        }

        composerRobot {
            topAppBarSection { tapCloseButton() }
        }

        mailboxRobot {
            verify { isShown() }
        }
    }

    @Test
    @TestId("79038")
    fun checkComposerBackButtonNavigation() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher {
            addMockRequests(
                given("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                given("/core/v4/features?Code=HideComposerAndroid&Type=boolean")
                    respondWith "/core/v4/features/composer/hide_composer_disabled.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        mailboxRobot {
            topAppBarSection { tapComposerIcon() }
        }

        composerRobot {
            keyboardSection { dismissKeyboard() }
        }

        uiDevice.pressBack()

        mailboxRobot {
            verify { isShown() }
        }
    }

    @Test
    @TestId("79039")
    fun checkComposerKeyboardDismissalWithBackButton() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher {
            addMockRequests(
                given("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                given("/core/v4/features?Code=HideComposerAndroid&Type=boolean")
                    respondWith "/core/v4/features/composer/hide_composer_disabled.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        mailboxRobot {
            topAppBarSection { tapComposerIcon() }
        }

        composerRobot {
            keyboardSection {
                dismissKeyboard()

                verify { keyboardIsNotShown() }
            }
        }
    }

    @Test
    @TestId("190226", "190227")
    fun testCollapseExpandChevron() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher()

        navigator {
            navigateTo(Destination.Composer)
        }

        composerRobot {
            toRecipientSection {
                expandCcAndBccFields()
                verify { isEmptyField() }
            }

            ccRecipientSection {
                verify { isEmptyField() }
            }

            bccRecipientSection {
                verify { isEmptyField() }
            }

            toRecipientSection {
                hideCcAndBccFields()
            }

            ccRecipientSection {
                verify { isHidden() }
            }

            bccRecipientSection {
                verify { isHidden() }
            }
        }
    }
}
