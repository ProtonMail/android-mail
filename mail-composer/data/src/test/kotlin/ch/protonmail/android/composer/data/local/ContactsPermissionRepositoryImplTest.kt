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

package ch.protonmail.android.composer.data.local

import app.cash.turbine.test
import arrow.core.Either
import arrow.core.right
import ch.protonmail.android.composer.data.repository.ContactsPermissionRepositoryImpl
import ch.protonmail.android.mailcommon.domain.model.DataError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ContactsPermissionRepositoryImplTest {

    private val dataSource = mockk<ContactsPermissionLocalDataSource>()

    @Test
    fun `should observe the permission denied state from the data source`() = runTest {
        // Given
        val sharedFlow = MutableSharedFlow<Either<DataError, Boolean>>()
        val expectedResult = true.right()
        every { dataSource.observePermissionDenied() } returns sharedFlow
        val repo = ContactsPermissionRepositoryImpl(dataSource)

        // When
        repo.observePermissionDenied().test {
            sharedFlow.emit(expectedResult)

            assertEquals(expectedResult, awaitItem())
        }

        // Then
        verify(exactly = 1) { dataSource.observePermissionDenied() }
        confirmVerified(dataSource)
    }

    @Test
    fun `should track the denial state through the data source`() = runTest {
        // Given
        coEvery { dataSource.trackPermissionDeniedEvent() } just runs
        val repo = ContactsPermissionRepositoryImpl(dataSource)

        // When
        repo.trackPermissionDenied()
        // Then
        coVerify(exactly = 1) { dataSource.trackPermissionDeniedEvent() }
        confirmVerified(dataSource)
    }
}
