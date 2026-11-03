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

package ch.protonmail.android.mailbyoe.domain.usecase

import ch.protonmail.android.mailsession.domain.repository.EventLoopRepository
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.AfterTest
import kotlin.test.Test

internal class RefreshAfterGmailConnectionTest {

    private val eventLoopRepository = mockk<EventLoopRepository>(relaxUnitFun = true)
    private val refreshAfterGmailConnection = RefreshAfterGmailConnection(eventLoopRepository)

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a finished connection flow, when refreshing, then the event loop is triggered`() = runTest {
        // When
        refreshAfterGmailConnection(userId)

        // Then
        coVerify(exactly = 1) { eventLoopRepository.trigger(userId) }
    }

    private companion object {

        val userId = UserId("user")
    }
}
