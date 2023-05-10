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
import ch.protonmail.android.networkmocks.mockwebserver.requests.MockPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.withPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.SmokeExtendedTest
import ch.protonmail.android.test.annotations.suite.TemporaryTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.login.LoginStrategy
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.models.snackbar.SnackbarTextEntry
import ch.protonmail.android.uitest.robot.composer.ComposerRobot
import ch.protonmail.android.uitest.robot.composer.keyboardSection
import ch.protonmail.android.uitest.robot.composer.messageSection
import ch.protonmail.android.uitest.robot.composer.participantsSection
import ch.protonmail.android.uitest.robot.composer.subjectSection
import ch.protonmail.android.uitest.robot.composer.topAppBarSection
import ch.protonmail.android.uitest.robot.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitest.robot.mailbox.snackbarSection
import ch.protonmail.android.uitest.robot.mailbox.topAppBarSection
import ch.protonmail.android.uitest.util.UiDeviceHolder.uiDevice
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Test

@SmokeExtendedTest
@HiltAndroidTest
@UninstallModules(ServerProofModule::class)
internal class ComposerMainTests : MockedNetworkTest(loginStrategy = LoginStrategy.LoggedOut) {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    private val inboxRobot = InboxRobot(composeTestRule)
    private val composerRobot = ComposerRobot(composeTestRule)

    @TemporaryTest
    @Test
    @TestId("79034")
    fun checkNavigationToComposerIsDisabledWhenFeatureToggleIsEnabled() {
        mockWebServer.dispatcher = mockNetworkDispatcher {
            addMockRequests(
                "/mail/v4/messages"
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                "/core/v4/features?Code=HideComposerAndroid&Type=boolean"
                    respondWith "/core/v4/features/composer/hide_composer_enabled.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        inboxRobot.run {
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
        mockWebServer.dispatcher = mockNetworkDispatcher {
            addMockRequests(
                "/mail/v4/messages"
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                "/core/v4/features?Code=HideComposerAndroid&Type=boolean"
                    respondWith "/core/v4/features/composer/hide_composer_disabled.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        inboxRobot.topAppBarSection { tapComposerIcon() }
        composerRobot.verify { composerIsShown() }
    }

    @TemporaryTest
    @Test
    @TestId("79036")
    fun checkComposerMainFieldsAndInteractions() {
        mockWebServer.dispatcher = mockNetworkDispatcher {
            addMockRequests(
                "/mail/v4/messages"
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                "/core/v4/features?Code=HideComposerAndroid&Type=boolean"
                    respondWith "/core/v4/features/composer/hide_composer_disabled.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        val expectedSender = "Sender"
        val expectedRecipient = "Recipient"
        val expectedSubject = "Subject"
        val expectedBody = "Text message"

        navigator {
            navigateTo(Destination.Inbox)
        }

        inboxRobot.topAppBarSection { tapComposerIcon() }

        composerRobot.run {
            verify { composerIsShown() }

            participantsSection {
                verify { hasRecipientFieldFocused() }
            }

            keyboardSection {
                verify { keyboardIsShown() }
            }

            // Sender field
            participantsSection {
                verify { hasEmptySender() }
                typeSender(expectedSender)
                verify { hasSender(expectedSender) }
            }

            // Recipient field
            participantsSection {
                verify { hasEmptyRecipient() }
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
            messageSection {
                verify { hasPlaceholderText() }
                typeMessageBody(expectedBody)
                verify { hasText(expectedBody) }
            }
        }
    }

    @Test
    @TestId("79037")
    fun checkComposerCloseNavigation() {
        mockWebServer.dispatcher = mockNetworkDispatcher {
            addMockRequests(
                "/mail/v4/messages"
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                "/core/v4/features?Code=HideComposerAndroid&Type=boolean"
                    respondWith "/core/v4/features/composer/hide_composer_disabled.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        inboxRobot.topAppBarSection { tapComposerIcon() }
        composerRobot.topAppBarSection { tapCloseButton() }

        inboxRobot.verify { mailboxScreenDisplayed() }
    }

    @Test
    @TestId("79038")
    fun checkComposerBackButtonNavigation() {
        mockWebServer.dispatcher = mockNetworkDispatcher {
            addMockRequests(
                "/mail/v4/messages"
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                "/core/v4/features?Code=HideComposerAndroid&Type=boolean"
                    respondWith "/core/v4/features/composer/hide_composer_disabled.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        inboxRobot.topAppBarSection { tapComposerIcon() }
        composerRobot.keyboardSection { dismissKeyboard() }
        uiDevice.pressBack()

        inboxRobot.verify { mailboxScreenDisplayed() }
    }

    @Test
    @TestId("79039")
    fun checkComposerKeyboardDismissalWithBackButton() {
        mockWebServer.dispatcher = mockNetworkDispatcher {
            addMockRequests(
                "/mail/v4/messages"
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true,
                "/core/v4/features?Code=HideComposerAndroid&Type=boolean"
                    respondWith "/core/v4/features/composer/hide_composer_disabled.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        inboxRobot.topAppBarSection { tapComposerIcon() }

        composerRobot.keyboardSection {
            dismissKeyboard()

            verify { keyboardIsNotShown() }
        }
    }
}
