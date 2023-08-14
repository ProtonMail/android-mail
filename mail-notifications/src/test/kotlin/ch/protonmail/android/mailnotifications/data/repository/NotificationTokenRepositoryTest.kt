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

package ch.protonmail.android.mailnotifications.data.repository

import java.util.UUID
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailnotifications.data.local.NotificationTokenLocalDataSource
import ch.protonmail.android.mailnotifications.data.remote.NotificationTokenRemoteDataSource
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Test

internal class NotificationTokenRepositoryTest {

    private val token = UUID.randomUUID().toString()
    private val userId = UserId("userId")
    private val localDataSource = mockk<NotificationTokenLocalDataSource>(relaxUnitFun = true) {
        coEvery { getToken() } returns token.right()
    }
    private val remoteDataSource = mockk<NotificationTokenRemoteDataSource>(relaxUnitFun = true) {
        coEvery { fetchToken() } returns token.right()
    }
    private val repository = NotificationTokenRepositoryImpl(localDataSource, remoteDataSource)

    @Test
    fun `when the repo saves a token, the remote data source is not called at all`() = runTest {
        // When
        repository.storeToken(token)

        // Then
        coVerify(exactly = 1) { localDataSource.storeToken(token) }
        coVerify { remoteDataSource wasNot called }
    }

    @Test
    fun `when the repo binds a token, it fetches it from local and then passes it to the remote data source`() =
        runTest {
            // When
            repository.bindTokenToUser(userId)

            // Then
            coVerifyOrder {
                localDataSource.getToken()
                remoteDataSource.bindTokenToUser(userId, token)
            }

            coVerify(exactly = 1) { localDataSource.getToken() }
            coVerify(exactly = 1) { remoteDataSource.bindTokenToUser(userId, token) }
            coVerify(exactly = 0) { remoteDataSource.fetchToken() }
            coVerify(exactly = 0) { localDataSource.storeToken(token) }
        }

    @Test
    fun `when binding a token with no local data, the repo fetches it and stores it again locally first`() = runTest {
        // Given
        coEvery { localDataSource.getToken() } returns DataError.Local.Unknown.left()

        // When
        repository.bindTokenToUser(userId)

        // Then
        coVerifyOrder {
            localDataSource.getToken()
            remoteDataSource.fetchToken()
            localDataSource.storeToken(token)
            remoteDataSource.bindTokenToUser(userId, token)
        }

        coVerify(exactly = 1) { localDataSource.getToken() }
        coVerify(exactly = 1) { remoteDataSource.fetchToken() }
        coVerify(exactly = 1) { localDataSource.storeToken(token) }
        coVerify(exactly = 1) { remoteDataSource.bindTokenToUser(userId, token) }
    }
}
