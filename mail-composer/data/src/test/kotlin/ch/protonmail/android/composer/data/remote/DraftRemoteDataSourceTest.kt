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

package ch.protonmail.android.composer.data.remote

import arrow.core.right
import ch.protonmail.android.composer.data.remote.resource.CreateDraftBody
import ch.protonmail.android.composer.data.remote.resource.UpdateDraftBody
import ch.protonmail.android.composer.data.remote.response.SaveDraftResponse
import ch.protonmail.android.composer.data.sample.CreateDraftBodySample
import ch.protonmail.android.composer.data.sample.MessageWithBodyResourceSample
import ch.protonmail.android.composer.data.sample.UpdateDraftBodySample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.model.DraftAction
import ch.protonmail.android.mailmessage.data.remote.resource.MessageWithBodyResource
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.network.data.ApiManagerFactory
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.session.SessionId
import me.proton.core.network.domain.session.SessionProvider
import me.proton.core.test.android.api.TestApiManager
import me.proton.core.util.kotlin.DefaultDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals

class DraftRemoteDataSourceTest {

    private val sessionId = SessionId("testSessionId")
    private val userId = UserIdSample.Primary

    private val draftApi = mockk<DraftApi>()
    private val apiManagerFactory = mockk<ApiManagerFactory> {
        every { create(any(), DraftApi::class) } returns TestApiManager(draftApi)
    }
    private val sessionProvider = mockk<SessionProvider> {
        coEvery { getSessionId(userId) } returns sessionId
    }

    private val apiProvider = ApiProvider(
        apiManagerFactory = apiManagerFactory,
        sessionProvider = sessionProvider,
        dispatcherProvider = DefaultDispatcherProvider()
    )

    private val remoteDataSource = DraftRemoteDataSourceImpl(apiProvider = apiProvider)

    @Test
    fun `create draft returns message with body when API call is successful`() = runTest {
        // Given
        val apiMessageId = MessageId("remote-api-assigned-messageId")
        val action = DraftAction.Compose
        val inputDraft = MessageWithBodySample.NewDraftWithSubject
        val expectedRequest = CreateDraftBodySample.NewDraftWithSubject
        val expectedApiResponse = MessageWithBodyResourceSample.NewDraftWithSubject.copy(id = apiMessageId.id)
        expectCreateDraftApiSucceeds(expectedRequest, expectedApiResponse)

        // When
        val actual = remoteDataSource.create(userId, inputDraft, action)

        // Then
        val expected = inputDraft.copy(
            message = inputDraft.message.copy(messageId = apiMessageId),
            messageBody = inputDraft.messageBody.copy(messageId = apiMessageId)
        )
        assertEquals(expected.right(), actual)
    }

    @Test
    fun `update draft returns message with body when API call is successful`() = runTest {
        // Given
        val apiTime = 123L
        val messageId = MessageIdSample.RemoteDraft
        val inputDraft = MessageWithBodySample.RemoteDraft
        val expectedRequest = UpdateDraftBodySample.RemoteDraft
        // Change time to ensure difference between input and API response drafts (API sets time)
        val expectedApiResponse = MessageWithBodyResourceSample.RemoteDraft.copy(time = apiTime)
        expectUpdateDraftApiSucceeds(messageId, expectedRequest, expectedApiResponse)

        // When
        val actual = remoteDataSource.update(userId, inputDraft)

        // Then
        val expected = inputDraft.copy(message = inputDraft.message.copy(time = apiTime))
        assertEquals(expected.right(), actual)
    }

    private fun expectCreateDraftApiSucceeds(body: CreateDraftBody, expected: MessageWithBodyResource) {
        coEvery { draftApi.createDraft(body) } returns SaveDraftResponse(code = ResponseCodes.OK, expected)
    }

    private fun expectUpdateDraftApiSucceeds(
        messageId: MessageId,
        body: UpdateDraftBody,
        expected: MessageWithBodyResource
    ) {
        coEvery { draftApi.updateDraft(messageId.id, body) } returns
            SaveDraftResponse(code = ResponseCodes.OK, expected)
    }

    companion object {
        private object ResponseCodes {
            const val OK = 1000
        }
    }
}
