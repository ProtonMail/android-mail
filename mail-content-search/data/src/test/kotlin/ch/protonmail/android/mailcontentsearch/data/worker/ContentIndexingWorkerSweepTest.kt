/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailcontentsearch.data.worker

import androidx.work.ProgressUpdater
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingError
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchIndexer
import ch.protonmail.android.mailcontentsearch.domain.usecase.FindFirstEligibleAccountToIndex
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchEnabled
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import com.google.common.util.concurrent.Futures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Before
import uniffi.mail_uniffi.MailBackgroundExecScope
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
internal class ContentIndexingWorkerSweepTest {

    private val userOne = UserId("user-1")
    private val userTwo = UserId("user-2")

    private val primaryUserId = MutableStateFlow<UserId?>(userOne)
    private val contentSearchEnabled = MutableStateFlow(true)

    private val indexer = mockk<ContentSearchIndexer>(relaxUnitFun = true)
    private val backgroundScope = mockk<MailBackgroundExecScope>()
    private val mailSessionRepository = mockk<MailSessionRepository> {
        every { getMailSession() } returns mockk {
            every { newBackgroundExecutionScope() } returns backgroundScope
        }
    }
    private val userSessionRepository = mockk<UserSessionRepository>(relaxUnitFun = true) {
        every { observePrimaryUserId() } returns primaryUserId
        coEvery { getAccount(any()) } returns null
    }
    private val findFirstEligibleAccountToIndex = mockk<FindFirstEligibleAccountToIndex>()
    private val observeContentSearchEnabled = mockk<ObserveContentSearchEnabled> {
        every { this@mockk.invoke(any()) } returns contentSearchEnabled
    }
    private val appInBackgroundState = mockk<AppInBackgroundState> {
        every { observe() } returns emptyFlow()
    }

    private val progressUpdater = mockk<ProgressUpdater> {
        every { updateProgress(any(), any(), any()) } returns Futures.immediateFuture(null)
    }
    private val workerParameters = mockk<WorkerParameters>(relaxed = true) {
        every { id } returns UUID.randomUUID()
        every { progressUpdater } returns this@ContentIndexingWorkerSweepTest.progressUpdater
        every { inputData } returns workDataOf(
            ContentIndexingWorker.KeyRunAsForeground to false
        )
    }

    private val worker = ContentIndexingWorker(
        mockk(relaxed = true),
        workerParameters,
        indexer,
        mailSessionRepository,
        userSessionRepository,
        findFirstEligibleAccountToIndex,
        observeContentSearchEnabled,
        appInBackgroundState
    )

    @Before
    fun setUp() {
        justRun { backgroundScope.finsihed() }
    }

    @Test
    fun `sweep indexes each eligible account in turn then succeeds`() = runTest {
        // Given
        coEvery { findFirstEligibleAccountToIndex(any()) } returnsMany listOf(userOne, userTwo, null)
        coEvery { indexer.index(userOne, any()) } returns Unit.right()
        coEvery { indexer.index(userTwo, any()) } returns Unit.right()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(androidx.work.ListenableWorker.Result.success(), result)
        coVerifyOrder {
            indexer.index(userOne, any())
            indexer.index(userTwo, any())
        }
    }

    @Test
    fun `sweep skips an account that fails and advances without retrying it`() = runTest {
        // Given
        coEvery { findFirstEligibleAccountToIndex(any()) } returnsMany listOf(userOne, userTwo, null)
        coEvery { indexer.index(userOne, any()) } returns ContentIndexingError.Cancelled.left()
        coEvery { indexer.index(userTwo, any()) } returns Unit.right()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(androidx.work.ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { indexer.index(userOne, any()) }
        coVerify(exactly = 1) { indexer.index(userTwo, any()) }
        coVerify { indexer.cancel(userOne) }
    }

    @Test
    fun `sweep interrupts the current account when another should run, then resumes`() = runTest {
        // Given
        coEvery {
            findFirstEligibleAccountToIndex(any())
        } returnsMany listOf(userOne, userTwo, userTwo, null)
        coEvery { indexer.index(userOne, any()) } coAnswers { awaitCancellation() }
        coEvery { indexer.index(userTwo, any()) } returns Unit.right()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(androidx.work.ListenableWorker.Result.success(), result)
        coVerify { indexer.index(userOne, any()) }
        coVerify { indexer.cancel(userOne) }
        coVerify { indexer.index(userTwo, any()) }
    }
}
