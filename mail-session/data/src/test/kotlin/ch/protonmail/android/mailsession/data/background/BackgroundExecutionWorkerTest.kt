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

package ch.protonmail.android.mailsession.data.background

import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import ch.protonmail.android.mailsession.data.usecase.StartBackgroundExecution
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uniffi.mail_uniffi.BackgroundExecutionResult
import uniffi.mail_uniffi.BackgroundExecutionStatus
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class BackgroundExecutionWorkerTest(
    @Suppress("unused") private val testName: String,
    val inputs: TestInputs
) {

    private val startBackgroundExecution = mockk<StartBackgroundExecution>()
    private val params = mockk<WorkerParameters>()

    private val worker = BackgroundExecutionWorker(
        mockk(),
        params,
        startBackgroundExecution
    )

    @Test
    fun `should propagate the result correctly`() = runTest {
        // Given
        every { startBackgroundExecution() } returns flowOf(inputs.result)

        // When
        val result = worker.doWork()

        // Then
        assertEquals(result, inputs.expected)
    }

    companion object {

        data class TestInputs(
            val result: BackgroundExecutionResult,
            val expected: ListenableWorker.Result
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(
                "failed status from background state",
                TestInputs(
                    result = BackgroundExecutionResult(
                        status = BackgroundExecutionStatus.Failed("failure"),
                        hasPendingActions = false,
                        hasUnsentMessages = false
                    ),
                    expected = ListenableWorker.Result.failure()
                )
            ),
            arrayOf(
                "aborted in background status from background state",
                TestInputs(
                    result = BackgroundExecutionResult(
                        status = BackgroundExecutionStatus.AbortedInBackground,
                        hasPendingActions = true,
                        hasUnsentMessages = false
                    ),
                    expected = ListenableWorker.Result.success()
                )
            ),
            arrayOf(
                "aborted in foreground status from background state",
                TestInputs(
                    result = BackgroundExecutionResult(
                        status = BackgroundExecutionStatus.AbortedInForeground,
                        hasPendingActions = true,
                        hasUnsentMessages = false
                    ),
                    expected = ListenableWorker.Result.success()
                )
            ),
            arrayOf(
                "skipped no active status from background state",
                TestInputs(
                    result = BackgroundExecutionResult(
                        status = BackgroundExecutionStatus.SkippedNoActiveContexts,
                        hasPendingActions = true,
                        hasUnsentMessages = false
                    ),
                    expected = ListenableWorker.Result.success()
                )
            ),
            arrayOf(
                "executed status from background state (no pending actions)",
                TestInputs(
                    result = BackgroundExecutionResult(
                        status = BackgroundExecutionStatus.Executed,
                        hasPendingActions = false,
                        hasUnsentMessages = false
                    ),
                    expected = ListenableWorker.Result.success()
                )
            ),
            arrayOf(
                "executed status from background state with pending actions",
                TestInputs(
                    result = BackgroundExecutionResult(
                        status = BackgroundExecutionStatus.Executed,
                        hasPendingActions = true,
                        hasUnsentMessages = false
                    ),
                    expected = ListenableWorker.Result.success()
                )
            ),
            arrayOf(
                "executed status from background state with unsent messages",
                TestInputs(
                    result = BackgroundExecutionResult(
                        status = BackgroundExecutionStatus.Executed,
                        hasPendingActions = false,
                        hasUnsentMessages = true
                    ),
                    expected = ListenableWorker.Result.success()
                )
            ),
            arrayOf(
                "timed out status from background state with pending actions",
                TestInputs(
                    result = BackgroundExecutionResult(
                        status = BackgroundExecutionStatus.TimedOut,
                        hasPendingActions = true,
                        hasUnsentMessages = false
                    ),
                    expected = ListenableWorker.Result.success()
                )
            ),
            arrayOf(
                "timed out status from background state with unsent messages",
                TestInputs(
                    result = BackgroundExecutionResult(
                        status = BackgroundExecutionStatus.TimedOut,
                        hasPendingActions = false,
                        hasUnsentMessages = true
                    ),
                    expected = ListenableWorker.Result.success()
                )
            )
        )
    }
}
