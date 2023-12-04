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

import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class ProcessPushNotificationDataWorkerParamsErrorTest(private val testInput: TestInput) {

    private val params: WorkerParameters = mockk {
        every { taskExecutor } returns mockk(relaxed = true)
    }

    private val worker = ProcessPushNotificationDataWorker(
        mockk(),
        params,
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk()
    )

    @Before
    fun reset() {
        unmockkAll()
    }

    @Test
    fun `input data missing makes the worker fail`() = withSuspend(testInput) {
        // Given
        every {
            params.inputData.getString(ProcessPushNotificationDataWorker.KeyPushNotificationUid)
        } returns mockedSessionId

        every {
            params.inputData.getString(ProcessPushNotificationDataWorker.KeyPushNotificationEncryptedMessage)
        } returns mockedEncryptedNotification

        // When
        val result = worker.doWork()

        // Then
        assertEquals(expectedFailure, result)
    }

    internal data class TestInput(
        val mockedSessionId: String?,
        val mockedEncryptedNotification: String?,
        val expectedFailure: ListenableWorker.Result
    )

    private fun withSuspend(testInput: TestInput, block: suspend TestInput.() -> Unit) = with(testInput) {
        runTest { block() }
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        internal fun data() = arrayOf(
            TestInput(
                null,
                null,
                ListenableWorker.Result.failure(
                    workDataOf(
                        ProcessPushNotificationDataWorker.KeyProcessPushNotificationDataError to
                            "Input data is missing"
                    )
                )
            ),
            TestInput(
                "id",
                null,
                ListenableWorker.Result.failure(
                    workDataOf(
                        ProcessPushNotificationDataWorker.KeyProcessPushNotificationDataError to
                            "Input data is missing"
                    )
                )
            ),
            TestInput(
                null,
                "notification",
                ListenableWorker.Result.failure(
                    workDataOf(
                        ProcessPushNotificationDataWorker.KeyProcessPushNotificationDataError to
                            "Input data is missing"
                    )
                )
            ),
            TestInput(
                "",
                "notification",
                ListenableWorker.Result.failure(
                    workDataOf(
                        ProcessPushNotificationDataWorker.KeyProcessPushNotificationDataError to
                            "Input data is missing"
                    )
                )
            ),
            TestInput(
                "id",
                "",
                ListenableWorker.Result.failure(
                    workDataOf(
                        ProcessPushNotificationDataWorker.KeyProcessPushNotificationDataError to
                            "Input data is missing"
                    )
                )
            ),
            TestInput(
                "",
                "",
                ListenableWorker.Result.failure(
                    workDataOf(
                        ProcessPushNotificationDataWorker.KeyProcessPushNotificationDataError to
                            "Input data is missing"
                    )
                )
            )
        )
    }
}
