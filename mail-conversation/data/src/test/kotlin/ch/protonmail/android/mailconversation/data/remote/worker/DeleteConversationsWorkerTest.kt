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

package ch.protonmail.android.mailconversation.data.remote.worker

import java.net.UnknownHostException
import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailconversation.data.remote.ConversationApi
import ch.protonmail.android.mailconversation.data.remote.resource.ConversationActionBody
import ch.protonmail.android.mailmessage.data.sample.PutLabelResponseSample
import ch.protonmail.android.testdata.conversation.ConversationTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.network.data.ApiManagerFactory
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.session.SessionId
import me.proton.core.network.domain.session.SessionProvider
import me.proton.core.test.android.api.TestApiManager
import me.proton.core.util.kotlin.DefaultDispatcherProvider
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class DeleteConversationsWorkerTest {

    private val conversationIds = listOf(ConversationId(ConversationTestData.RAW_CONVERSATION_ID))
    private val labelId = LabelId("10")

    private val workManager: WorkManager = mockk {
        coEvery { enqueue(any<OneTimeWorkRequest>()) } returns mockk()
    }
    private val parameters: WorkerParameters = mockk {
        every { taskExecutor } returns mockk(relaxed = true)
        every { inputData.getString(DeleteConversationsWorker.RawUserIdKey) } returns userId.id
        every {
            inputData.getStringArray(DeleteConversationsWorker.RawConversationIdsKey)
        } returns arrayOf(ConversationTestData.RAW_CONVERSATION_ID)
        every { inputData.getString(DeleteConversationsWorker.RawLabelIdKey) } returns labelId.id
    }
    private val context: Context = mockk()

    private val sessionProvider = mockk<SessionProvider> {
        coEvery { getSessionId(userId) } returns SessionId("testSessionId")
    }
    private val conversationApi = mockk<ConversationApi> {
        coEvery { deleteConversations(any()) } returns PutLabelResponseSample.putLabelResponseForOneMessage
    }
    private val apiManagerFactory = mockk<ApiManagerFactory> {
        every { create(any(), ConversationApi::class) } returns TestApiManager(conversationApi)
    }

    private lateinit var apiProvider: ApiProvider
    private lateinit var deleteConversationsWorker: DeleteConversationsWorker

    @Before
    fun setUp() {
        apiProvider = ApiProvider(apiManagerFactory, sessionProvider, DefaultDispatcherProvider())
        deleteConversationsWorker = DeleteConversationsWorker(context, parameters, apiProvider)
    }

    @Test
    fun `worker is enqueued with given parameters`() {
        // When
        Enqueuer(workManager).enqueue<DeleteConversationsWorker>(
            userId,
            DeleteConversationsWorker.params(userId, conversationIds, labelId)
        )

        // Then
        val requestSlot = slot<OneTimeWorkRequest>()
        verify { workManager.enqueue(capture(requestSlot)) }
        val workSpec = requestSlot.captured.workSpec
        val constraints = workSpec.constraints
        val inputData = workSpec.input
        val actualUserId = inputData.getString(DeleteConversationsWorker.RawUserIdKey)
        val actualConversationIds = inputData.getStringArray(DeleteConversationsWorker.RawConversationIdsKey)
        val actualLabelId = inputData.getString(DeleteConversationsWorker.RawLabelIdKey)
        assertEquals(userId.id, actualUserId)
        assertEquals(conversationIds, actualConversationIds?.toList()?.map { ConversationId(it) })
        assertEquals(labelId.id, actualLabelId)
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
    }

    @Test
    fun `when worker is started then api is called with the given parameters`() = runTest {
        // When
        deleteConversationsWorker.doWork()

        // Then
        coVerify {
            conversationApi.deleteConversations(ConversationActionBody(labelId.id, conversationIds.map { it.id }))
        }
    }

    @Test
    fun `worker returns failure when userid worker parameter is missing`() = runTest {
        // Given
        every { parameters.inputData.getString(DeleteConversationsWorker.RawUserIdKey) } returns null

        // When
        val result = deleteConversationsWorker.doWork()

        // Then
        coVerify { conversationApi wasNot Called }
        assertEquals(Result.failure(), result)
    }

    @Test
    fun `worker returns failure when conversationIds worker parameter is null`() = runTest {
        // Given
        every { parameters.inputData.getStringArray(DeleteConversationsWorker.RawConversationIdsKey) } returns null

        // When
        val result = deleteConversationsWorker.doWork()

        // Then
        coVerify { conversationApi wasNot Called }
        assertEquals(Result.failure(), result)
    }

    @Test
    fun `worker returns failure when labelId worker parameter is blank`() = runTest {
        // Given
        every { parameters.inputData.getString(DeleteConversationsWorker.RawLabelIdKey) } returns " "

        // When
        val result = deleteConversationsWorker.doWork()

        // Then
        coVerify { conversationApi wasNot Called }
        assertEquals(Result.failure(), result)
    }

    @Test
    fun `worker returns success when api call was successful`() = runTest {
        // When
        val result = deleteConversationsWorker.doWork()

        // Then
        assertEquals(Result.success(), result)
    }

    @Test
    fun `worker returns retry when api call fails due to connection error`() = runTest {
        // Given
        coEvery { conversationApi.deleteConversations(any()) } throws UnknownHostException()

        // When
        val result = deleteConversationsWorker.doWork()

        // Then
        assertEquals(Result.retry(), result)
    }

    @Test
    fun `worker returns failure when api call fails due to serializationException error`() = runTest {
        // Given
        coEvery { conversationApi.deleteConversations(any()) } throws SerializationException()

        // When
        val result = deleteConversationsWorker.doWork()

        // Then
        assertEquals(Result.failure(), result)
    }
}
