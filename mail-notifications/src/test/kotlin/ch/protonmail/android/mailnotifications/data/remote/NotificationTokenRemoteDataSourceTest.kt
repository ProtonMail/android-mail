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

package ch.protonmail.android.mailnotifications.data.remote

import java.util.UUID
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Test

internal class NotificationTokenRemoteDataSourceTest {

    private val enqueuer: Enqueuer = mockk<Enqueuer>(relaxUnitFun = true)
    private val remoteDataSource = NotificationTokenRemoteDataSourceImpl(mockk(), enqueuer)
    private val token = UUID.randomUUID().toString()
    private val userId = UserId("userId")

    @Test
    fun `when the synchronization is invoked, it is delegated to the underlying enqueuer`() = runTest {
        // When
        remoteDataSource.bindTokenToUser(userId, token)

        // Then
        coVerify(exactly = 1) {
            enqueuer.enqueue<RegisterDeviceWorker>(userId, RegisterDeviceWorker.params(userId, token))
        }
    }
}
