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

package ch.protonmail.android.uitest.e2e.mailbox.errors.append

import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.MockPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.get
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.serveOnce
import ch.protonmail.android.networkmocks.mockwebserver.requests.withNetworkDelay
import ch.protonmail.android.networkmocks.mockwebserver.requests.withPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.models.avatar.AvatarInitial
import ch.protonmail.android.uitest.models.mailbox.MailboxListItemEntry
import ch.protonmail.android.uitest.models.mailbox.ParticipantEntry
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Ignore
import org.junit.Test

@SmokeTest
@HiltAndroidTest
@Ignore("To be enabled again when MAILANDR-1162 is addressed.")
@UninstallModules(ServerProofModule::class)
internal class ConversationModeAppendItemsTests : MockedNetworkTest(), MailboxAppendItemsTests {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    override val lastExpectedMailboxItem = MailboxListItemEntry(
        index = 101,
        avatarInitial = AvatarInitial.WithText("P"),
        participants = listOf(ParticipantEntry.WithParticipant("Proton", isProton = true)),
        subject = "Last Element!",
        date = "May 7, 2023"
    )

    @Test
    @TestId("189113")
    @Suppress("MaxLineLength")
    fun checkAppendErrorAndRetryInConversationMode() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_conversation.json"
                    withStatusCode 200,
                get("/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=0&Sort=Time&Desc=1")
                    respondWith "/mail/v4/conversations/conversations_189113_1.json"
                    withStatusCode 200 serveOnce true,
                get("/mail/v4/conversations?Page=0&PageSize=25&Limit=25&LabelID=0&Sort=Time&Desc=1&End=1683964808&EndID=-ca1Hsn5gJ5pVXKT683Jks9DF_HMYnJ320IAdwRamIM8Y-qmce6sHmX9ybG692_KPk89lEuTp5OU0iAFzwF2zA%3D%3D")
                    respondWith "/mail/v4/conversations/conversations_189113_2.json"
                    withStatusCode 200 serveOnce true,
                get("/mail/v4/conversations?Page=0&PageSize=25&Limit=25&LabelID=0&Sort=Time&Desc=1&End=1683532886&EndID=yFFVROGaGAfA0O4rOchW_1oF_-Giys_QfSaRS69zTeWOuyQmwx_SESSDZlVp67N76pBde92SyQ-cMDlA_71T5w%3D%3D")
                    respondWith "/global/errors/error_mock.json"
                    withStatusCode 503 withPriority MockPriority.Highest withNetworkDelay 2000 serveOnce true,
                get("/mail/v4/conversations?Page=0&PageSize=25&Limit=25&LabelID=0&Sort=Time&Desc=1&End=1683532886&EndID=yFFVROGaGAfA0O4rOchW_1oF_-Giys_QfSaRS69zTeWOuyQmwx_SESSDZlVp67N76pBde92SyQ-cMDlA_71T5w%3D%3D")
                    respondWith "/mail/v4/conversations/conversations_189113_3.json"
                    withStatusCode 200 withNetworkDelay 2000
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        verifyAppendAdditionalItemsErrorAndRetry()
    }

    @Test
    @TestId("189113/2", "189158")
    @Suppress("MaxLineLength")
    fun checkAppendItemsInConversationMode() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(useDefaultMailSettings = false) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_placeholder_conversation.json"
                    withStatusCode 200,
                get("/mail/v4/conversations?Page=0&PageSize=75&Limit=75&LabelID=0&Sort=Time&Desc=1")
                    respondWith "/mail/v4/conversations/conversations_189113_1.json"
                    withStatusCode 200 serveOnce true,
                get("/mail/v4/conversations?Page=0&PageSize=25&Limit=25&LabelID=0&Sort=Time&Desc=1&End=1683964808&EndID=-ca1Hsn5gJ5pVXKT683Jks9DF_HMYnJ320IAdwRamIM8Y-qmce6sHmX9ybG692_KPk89lEuTp5OU0iAFzwF2zA%3D%3D")
                    respondWith "/mail/v4/conversations/conversations_189113_2.json"
                    withStatusCode 200 serveOnce true,
                get("/mail/v4/conversations?Page=0&PageSize=25&Limit=25&LabelID=0&Sort=Time&Desc=1&End=1683532886&EndID=yFFVROGaGAfA0O4rOchW_1oF_-Giys_QfSaRS69zTeWOuyQmwx_SESSDZlVp67N76pBde92SyQ-cMDlA_71T5w%3D%3D")
                    respondWith "/mail/v4/conversations/conversations_189113_3.json"
                    withStatusCode 200 withNetworkDelay 2000
            )
        }

        navigator {
            navigateTo(Destination.Inbox)
        }

        verifyAppendAdditionalItems()
    }
}
