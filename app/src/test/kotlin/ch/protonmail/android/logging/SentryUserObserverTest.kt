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

package ch.protonmail.android.logging

import java.util.UUID
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import io.sentry.Sentry
import io.sentry.protocol.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.test.kotlin.TestCoroutineScopeProvider
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SentryUserObserverTest {

    private val accountManager = mockk<AccountManager> {
        every { this@mockk.getPrimaryUserId() } returns flowOf(UserIdTestData.userId)
    }

    private lateinit var sentryUserObserver: SentryUserObserver

    @Before
    fun setUp() {
        Dispatchers.setMain(TestDispatcherProvider().Main)
        mockkStatic(Sentry::class)
        sentryUserObserver = SentryUserObserver(
            scopeProvider = TestCoroutineScopeProvider(),
            accountManager = accountManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Sentry::class)
    }

    @Test
    fun `register userId in Sentry for valid primary account`() = runTest {
        // When
        sentryUserObserver.start().join()
        // Then
        val sentryUserSlot = slot<User>()
        verify { Sentry.setUser(capture(sentryUserSlot)) }
        assertEquals(UserIdTestData.userId.id, sentryUserSlot.captured.id)
    }

    @Test
    fun `register random UUID in Sentry when no primary account available`() = runTest {
        // Given
        every { accountManager.getPrimaryUserId() } returns flowOf(null)
        // When
        sentryUserObserver.start().join()
        // Then
        val sentryUserSlot = slot<User>()
        verify { Sentry.setUser(capture(sentryUserSlot)) }
        val actual = UUID.fromString(sentryUserSlot.captured.id)
        assertTrue(actual.toString().isNotBlank())
    }
}
