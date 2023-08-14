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

import java.util.UUID
import arrow.core.right
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class NotificationTokenLocalDataSourceTest {

    private val token = UUID.randomUUID().toString()
    private val preferences: NotificationTokenPreferences = mockk<NotificationTokenPreferences>(relaxUnitFun = true) {
        coEvery { getToken() } returns token.right()
    }
    private val dataSource = NotificationTokenLocalDataSourceImpl(preferences)

    @Test
    fun `when the local data source stores a token, it passes it down to the token data store`() = runTest {
        // When
        dataSource.storeToken(token)

        // Then
        coVerify(exactly = 1) { preferences.storeToken(token) }
    }

    @Test
    fun `when the local data is requested a token, it fetches it from the token  data store`() = runTest {
        // When
        dataSource.getToken()

        // Then
        coVerify(exactly = 1) { preferences.getToken() }
    }
}
