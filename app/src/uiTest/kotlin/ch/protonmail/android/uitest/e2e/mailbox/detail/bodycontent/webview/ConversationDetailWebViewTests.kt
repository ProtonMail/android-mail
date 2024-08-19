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

package ch.protonmail.android.uitest.e2e.mailbox.detail.bodycontent.webview

import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.get
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.matchWildcards
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.serveOnce
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.test.utils.mocks.WebViewProviderMocks.mockWebViewAvailabilityOnDevice
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.robot.detail.conversationDetailRobot
import ch.protonmail.android.uitest.robot.detail.section.messageBodySection
import ch.protonmail.android.uitest.robot.detail.section.verify
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Test

@RegressionTest
@HiltAndroidTest
@UninstallModules(ServerProofModule::class)
internal class ConversationDetailWebViewTests : MockedNetworkTest(), MockedWebViewTests {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    @Test
    @TestId("225746")
    fun testWebViewProviderNotPresentShowsWarningMessageInPlaceOfBodyInConversationMode() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_225746.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_225746.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_225746.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_225746.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        mockWebViewAvailabilityOnDevice(isPackagePresent = false)

        navigator {
            navigateTo(Destination.MailDetail())
        }

        conversationDetailRobot {
            messageBodySection {
                verify { isShowingMissingWebViewWarning() }
            }
        }
    }

    @Test
    @TestId("225746/2", "225748")
    fun testWebViewProviderPresentButDisabledShowsWarningMessageInPlaceOfBodyInConversationMode() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_225746.json"
                    withStatusCode 200,
                get("/mail/v4/conversations")
                    respondWith "/mail/v4/conversations/conversations_225746.json"
                    withStatusCode 200 ignoreQueryParams true,
                get("/mail/v4/conversations/*")
                    respondWith "/mail/v4/conversations/conversation-id/conversation-id_225746.json"
                    withStatusCode 200 matchWildcards true,
                get("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/message-id/message-id_225746.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        mockWebViewAvailabilityOnDevice(isPackagePresent = true, isPackageEnabled = false)

        navigator {
            navigateTo(Destination.MailDetail())
        }

        conversationDetailRobot {
            messageBodySection {
                verify { isShowingMissingWebViewWarning() }
            }
        }
    }
}
