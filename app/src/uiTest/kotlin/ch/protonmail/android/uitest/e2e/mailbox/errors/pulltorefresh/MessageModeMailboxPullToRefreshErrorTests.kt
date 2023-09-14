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

package ch.protonmail.android.uitest.e2e.mailbox.errors.pulltorefresh

import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.get
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.serveOnce
import ch.protonmail.android.networkmocks.mockwebserver.requests.withNetworkDelay
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Test

@RegressionTest
@HiltAndroidTest
@UninstallModules(ServerProofModule::class)
internal class MessageModeMailboxPullToRefreshErrorTests :
    MockedNetworkTest(), MailboxPullToRefreshErrorTests {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    @Test
    @TestId("188899")
    fun checkMessagesLoadingErrorToError() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_messages.json"
                    withStatusCode 200,
                get("/mail/v4/messages")
                    respondWith "/global/errors/error_mock.json"
                    withStatusCode 503 ignoreQueryParams true serveOnce true,
                get("/mail/v4/messages")
                    respondWith "/global/errors/error_mock.json"
                    withStatusCode 503 ignoreQueryParams true withNetworkDelay 2000 serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        verifyErrorToError()
    }

    @Test
    @TestId("188900")
    fun checkMessagesLoadingContentToError() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_messages.json"
                    withStatusCode 200,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_base_placeholder.json"
                    withStatusCode 200 ignoreQueryParams true serveOnce true,
                get("/mail/v4/messages")
                    respondWith "/global/errors/error_mock.json"
                    withStatusCode 503 ignoreQueryParams true withNetworkDelay 2000 serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        verifyContentToError()
    }

    @Test
    @SmokeTest
    @TestId("188901")
    fun checkMessagesLoadingErrorToContent() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_messages.json"
                    withStatusCode 200,
                get("/mail/v4/messages")
                    respondWith "/global/errors/error_mock.json"
                    withStatusCode 503 ignoreQueryParams true serveOnce true,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_base_placeholder.json"
                    withStatusCode 200 ignoreQueryParams true withNetworkDelay 2000 serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        verifyErrorToContent()
    }

    @Test
    @TestId("188902")
    fun checkMessagesLoadingEmptyToError() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_messages.json"
                    withStatusCode 200,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true serveOnce true,
                get("/mail/v4/messages")
                    respondWith "/global/errors/error_mock.json"
                    withStatusCode 503 ignoreQueryParams true withNetworkDelay 2000 serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        verifyEmptyToError()
    }

    @Test
    @TestId("188903")
    fun checkMessagesLoadingErrorToEmpty() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_messages.json"
                    withStatusCode 200,
                get("/mail/v4/messages")
                    respondWith "/global/errors/error_mock.json"
                    withStatusCode 503 ignoreQueryParams true serveOnce true,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true withNetworkDelay 2000 serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        verifyErrorToEmpty()
    }

    @Test
    @SmokeTest
    @TestId("188904")
    fun checkMessagesLoadingEmptyToContent() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_messages.json"
                    withStatusCode 200,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true serveOnce true,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_base_placeholder.json"
                    withStatusCode 200 ignoreQueryParams true withNetworkDelay 2000 serveOnce true
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        verifyEmptyToContent()
    }
}
