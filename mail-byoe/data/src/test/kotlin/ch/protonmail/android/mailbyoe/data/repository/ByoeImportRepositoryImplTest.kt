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

package ch.protonmail.android.mailbyoe.data.repository

import app.cash.turbine.test
import ch.protonmail.android.mailbyoe.domain.model.ByoeImportError
import ch.protonmail.android.mailbyoe.domain.model.ByoeImportState
import ch.protonmail.android.mailbyoe.domain.model.ByoeImportStatus
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.ActionError
import uniffi.mail_uniffi.ByoeImports
import uniffi.mail_uniffi.ByoeImportsImportStatusesResult
import uniffi.mail_uniffi.ByoeImportsWatchImportsResult
import uniffi.mail_uniffi.Id
import uniffi.mail_uniffi.LiveQueryCallback
import uniffi.mail_uniffi.MailUserSession
import uniffi.mail_uniffi.WatchHandle
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import uniffi.mail_uniffi.ByoeImportError as RustByoeImportError
import uniffi.mail_uniffi.ByoeImportState as RustByoeImportState
import uniffi.mail_uniffi.ByoeImportStatus as RustByoeImportStatus

internal class ByoeImportRepositoryImplTest {

    private val watchHandle = mockk<WatchHandle>(relaxed = true)
    private val callback = slot<LiveQueryCallback>()
    private val byoeImports = mockk<ByoeImports>(relaxed = true) {
        coEvery { watchImports(capture(callback)) } returns ByoeImportsWatchImportsResult.Ok(watchHandle)
    }
    private val rustSession = mockk<MailUserSession> {
        every { byoeImports() } returns byoeImports
    }
    private val userSessionRepository = mockk<UserSessionRepository> {
        coEvery { getUserSession(userId) } returns MailUserSessionWrapper(rustSession)
    }
    private val repository = ByoeImportRepositoryImpl(userSessionRepository)

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given an import paused for storage, when observing, then it is reported`() = runTest {
        // Given
        coEvery { byoeImports.importStatuses() } returns ByoeImportsImportStatusesResult.Ok(listOf(rustPausedImport))

        // When
        val result = repository.observeImportStatuses(userId).first()

        // Then
        assertEquals(listOf(pausedImport), result)
    }

    @Test
    fun `given the imports change, when observing, then the new imports are reported`() = runTest {
        // Given
        coEvery { byoeImports.importStatuses() } returnsMany listOf(
            ByoeImportsImportStatusesResult.Ok(listOf(rustPausedImport)),
            ByoeImportsImportStatusesResult.Ok(emptyList())
        )

        repository.observeImportStatuses(userId).test {
            assertEquals(listOf(pausedImport), awaitItem())

            // When
            callback.captured.onUpdate()

            // Then
            assertEquals(emptyList(), awaitItem())
        }
    }

    @Test
    fun `given the imports cannot be read, when observing, then no import is reported`() = runTest {
        // Given
        coEvery { byoeImports.importStatuses() } returns ByoeImportsImportStatusesResult.Error(mockk<ActionError>())

        // When
        repository.observeImportStatuses(userId).test {

            // Then
            expectNoEvents()
        }
    }

    @Test
    fun `given no user session, when observing, then no import is reported`() = runTest {
        // Given
        coEvery { userSessionRepository.getUserSession(userId) } returns null

        // When
        repository.observeImportStatuses(userId).test {

            // Then
            awaitComplete()
        }
    }

    @Test
    fun `when observing stops, then the imports are no longer watched`() = runTest {
        // Given
        coEvery { byoeImports.importStatuses() } returns ByoeImportsImportStatusesResult.Ok(emptyList())

        // When
        repository.observeImportStatuses(userId).first()

        // Then
        verify { watchHandle.disconnect() }
    }

    private companion object {

        val userId = UserId("user")

        val rustPausedImport = RustByoeImportStatus(
            addressId = Id(42uL),
            state = RustByoeImportState.PAUSED,
            error = RustByoeImportError.STORAGE_LIMIT,
            processed = 1_200uL,
            total = null
        )

        val pausedImport = ByoeImportStatus(
            addressId = "42",
            state = ByoeImportState.Paused,
            error = ByoeImportError.StorageLimit,
            processed = 1_200,
            total = null
        )
    }
}
