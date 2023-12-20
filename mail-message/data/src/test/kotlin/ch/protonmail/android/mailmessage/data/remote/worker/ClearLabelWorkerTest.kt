package ch.protonmail.android.mailmessage.data.remote.worker

import java.net.UnknownHostException
import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailmessage.data.remote.MessageApi
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.network.data.ApiManagerFactory
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.session.SessionId
import me.proton.core.network.domain.session.SessionProvider
import me.proton.core.test.android.api.TestApiManager
import me.proton.core.util.kotlin.DefaultDispatcherProvider
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals

class ClearLabelWorkerTest {

    private val labelId = LabelIdSample.Spam.id
    private val userId = UserIdSample.Primary.id

    private val workManager: WorkManager = mockk {
        coEvery { enqueue(any<OneTimeWorkRequest>()) } returns mockk()
    }
    private val parameters: WorkerParameters = mockk {
        every { taskExecutor } returns mockk(relaxed = true)
        every { inputData.getString(ClearLabelWorker.RawUserIdKey) } returns userId
        every { inputData.getString(ClearLabelWorker.RawLabelIdKey) } returns labelId
    }
    private val context: Context = mockk()

    private val sessionProvider = mockk<SessionProvider> {
        coEvery { getSessionId(UserId(userId)) } returns SessionId("testSessionId")
    }
    private val messageApi = mockk<MessageApi> {
        coEvery { emptyLabel(labelId) } returns mockk()
    }
    private val apiManagerFactory = mockk<ApiManagerFactory> {
        every { create(any(), MessageApi::class) } returns TestApiManager(messageApi)
    }
    private val messageLocalDataSource: MessageLocalDataSource = mockk() {
        coJustRun { deleteMessagesWithLabel(UserId(userId), LabelId(labelId)) }
    }

    private lateinit var apiProvider: ApiProvider
    private lateinit var deleteMessagesWithLabel: ClearLabelWorker

    @Before
    fun setUp() {
        apiProvider = ApiProvider(apiManagerFactory, sessionProvider, DefaultDispatcherProvider())
        deleteMessagesWithLabel = ClearLabelWorker(context, parameters, apiProvider, messageLocalDataSource)
    }

    @Test
    fun `worker is enqueued with given parameters`() {
        // When
        Enqueuer(workManager).enqueue<ClearLabelWorker>(
            UserId(userId), ClearLabelWorker.params(UserId(userId), LabelId(labelId))
        )

        // Then
        val requestSlot = slot<OneTimeWorkRequest>()
        verify { workManager.enqueue(capture(requestSlot)) }
        val workSpec = requestSlot.captured.workSpec
        val constraints = workSpec.constraints
        val inputData = workSpec.input
        val actualUserId = inputData.getString(ClearLabelWorker.RawUserIdKey)
        val actualLabelId = inputData.getString(ClearLabelWorker.RawLabelIdKey)
        assertEquals(userId, actualUserId)
        assertEquals(labelId, actualLabelId)
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
    }

    @Test
    fun `when delete messages with labels is started then api is called with the given parameters`() = runTest {
        // When
        val result = deleteMessagesWithLabel.doWork()

        // Then
        coVerify { messageApi.emptyLabel(labelId) }
        coVerify { messageLocalDataSource.deleteMessagesWithLabel(UserId(userId), LabelId(labelId)) }
        assertEquals(Result.success(), result)
    }

    @Test
    fun `delete messages with labels fails when userId is null`() = runTest {
        // Given
        every { parameters.inputData.getString(ClearLabelWorker.RawUserIdKey) } returns null

        // When
        val result = deleteMessagesWithLabel.doWork()

        // Then
        assertEquals(Result.failure(), result)
        coVerify {
            messageApi wasNot Called
            messageLocalDataSource wasNot Called
        }
    }

    @Test
    fun `delete messages with labels fails when labelId is null`() = runTest {
        // Given
        every { parameters.inputData.getString(ClearLabelWorker.RawLabelIdKey) } returns null

        // When
        val result = deleteMessagesWithLabel.doWork()

        // Then
        assertEquals(Result.failure(), result)
        coVerify {
            messageApi wasNot Called
            messageLocalDataSource wasNot Called
        }
    }

    @Test
    fun `delete messages with labels returns retry when api call fails with retryable error`() = runTest {
        // Given
        coEvery { messageApi.emptyLabel(labelId) } throws UnknownHostException()

        // When
        val result = deleteMessagesWithLabel.doWork()

        // Then
        assertEquals(Result.retry(), result)
        coVerify {
            messageApi.emptyLabel(labelId)
            messageLocalDataSource wasNot Called
        }
    }

    @Test
    fun `delete messages with labels returns failure when api call fails due to non retryable error`() = runTest {
        // Given
        coEvery { messageApi.emptyLabel(labelId) } throws SerializationException()

        // When
        val result = deleteMessagesWithLabel.doWork()

        // Then
        assertEquals(Result.failure(), result)
        coVerify {
            messageApi.emptyLabel(labelId)
            messageLocalDataSource wasNot Called
        }
    }
}
