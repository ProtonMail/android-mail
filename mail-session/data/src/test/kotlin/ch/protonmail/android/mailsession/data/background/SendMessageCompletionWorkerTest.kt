/*
 * Copyright (c) 2026 Proton Technologies AG
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
import ch.protonmail.android.mailsession.domain.background.PendingSendTracker
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import uniffi.mail_uniffi.BackgroundExecutionResult
import uniffi.mail_uniffi.BackgroundExecutionStatus
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SendMessageCompletionWorkerTest {

    private val startBackgroundExecution = mockk<StartBackgroundExecution>()
    private val pendingSendTracker = mockk<PendingSendTracker>()
    private val params = mockk<WorkerParameters>()

    private val worker = SendMessageCompletionWorker(
        mockk(),
        params,
        startBackgroundExecution,
        pendingSendTracker
    )

    @Test
    fun `returns success and stops immediately when no sends are pending`() = runTest {
        // Given
        every { startBackgroundExecution() } returns flowOf(executedResult)
        every { pendingSendTracker.hasPendingSends() } returns false

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `keeps foreground work alive until pending sends complete`() = runTest {
        // Given
        every { startBackgroundExecution() } returns flow { awaitCancellation() }
        every { pendingSendTracker.hasPendingSends() } returnsMany listOf(true, true, false)

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 3) { pendingSendTracker.hasPendingSends() }
    }

    private companion object {

        val executedResult = BackgroundExecutionResult(
            status = BackgroundExecutionStatus.Executed,
            hasPendingActions = false,
            hasUnsentMessages = false
        )
    }
}
