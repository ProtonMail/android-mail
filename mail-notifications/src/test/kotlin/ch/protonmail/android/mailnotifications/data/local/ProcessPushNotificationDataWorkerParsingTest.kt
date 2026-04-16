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

package ch.protonmail.android.mailnotifications.data.local

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.mailnotifications.domain.usecase.ProcessPushNotification
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.session.SessionId
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class ProcessPushNotificationDataWorkerParsingTest {

    private val context = mockk<Context>(relaxUnitFun = true)
    private val processPushNotification = mockk<ProcessPushNotification> {
        coEvery {
            this@mockk.invoke(
                userId = UserId(RawUserId),
                sessionId = SessionId(RawSessionId),
                encryptedPayload = RawNotification
            )
        } returns ListenableWorker.Result.success()
    }

    private val params: WorkerParameters = mockk {
        every { taskExecutor } returns mockk(relaxed = true)
        every { inputData.getString(ProcessPushNotificationDataWorker.KeyPushNotificationUserId) } returns RawUserId
        every { inputData.getString(ProcessPushNotificationDataWorker.KeyPushNotificationUid) } returns RawSessionId
        every {
            inputData.getString(
                ProcessPushNotificationDataWorker.KeyPushNotificationEncryptedMessage
            )
        } returns RawNotification
    }

    private val worker = ProcessPushNotificationDataWorker(
        context = context,
        workerParameters = params,
        processPushNotification = processPushNotification
    )

    @Before
    fun reset() {
        unmockkAll()
    }

    @Test
    fun `null user id makes the worker fail`() = runTest {
        // Given
        coEvery {
            params.inputData.getString(ProcessPushNotificationDataWorker.KeyPushNotificationUserId)
        } returns null

        // When
        val result = worker.doWork()

        // Then
        assertEquals(MissingInputData, result)
    }

    @Test
    fun `null session id makes the worker fail`() = runTest {
        // Given
        coEvery {
            params.inputData.getString(ProcessPushNotificationDataWorker.KeyPushNotificationUid)
        } returns null

        // When
        val result = worker.doWork()

        // Then
        assertEquals(MissingInputData, result)
    }

    @Test
    fun `null encrypted notification makes the worker fail`() = runTest {
        // Given
        coEvery {
            params.inputData.getString(ProcessPushNotificationDataWorker.KeyPushNotificationEncryptedMessage)
        } returns null

        // When
        val result = worker.doWork()

        // Then
        assertEquals(MissingInputData, result)
    }

    @Test
    fun `valid input delegates to process push notification use case`() = runTest {
        // Given
        val expected = ListenableWorker.Result.success()
        coEvery {
            processPushNotification(
                userId = UserId(RawUserId),
                sessionId = SessionId(RawSessionId),
                encryptedPayload = RawNotification
            )
        } returns expected

        // When
        val result = worker.doWork()

        // Then
        assertEquals(expected, result)
        coVerify(exactly = 1) {
            processPushNotification(
                userId = UserId(RawUserId),
                sessionId = SessionId(RawSessionId),
                encryptedPayload = RawNotification
            )
        }
    }

    private companion object {

        const val RawNotification = "notification"
        const val RawSessionId = "sessionId"
        const val RawUserId = "userId"

        val MissingInputData = ListenableWorker.Result.failure(
            workDataOf(
                ProcessPushNotificationDataWorker.KeyPushNotificationDataError to
                    "Input data is missing"
            )
        )
    }

}
