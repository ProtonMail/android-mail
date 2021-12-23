/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.logging

import ch.protonmail.android.testdata.AccountTestData
import ch.protonmail.android.testdata.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import io.sentry.Sentry
import io.sentry.protocol.User
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.getPrimaryAccount
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class SentryUserObserverTest {

    private val accountManager = mockk<AccountManager> {
        mockkStatic(this@mockk::getPrimaryAccount)
        every { this@mockk.getPrimaryAccount() } returns flowOf(AccountTestData.readyAccount)
    }

    private lateinit var sentryUserObserver: SentryUserObserver

    @Before
    fun setUp() {
        mockkStatic(Sentry::class)
        sentryUserObserver = SentryUserObserver(
            TestCoroutineScope(),
            accountManager
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(accountManager::getPrimaryAccount)
        unmockkStatic(Sentry::class)
    }

    @Test
    fun `register userId in Sentry for valid primary account`() = runTest {
        // WHEN
        sentryUserObserver.start()
        // THEN
        val sentryUserSlot = slot<User>()
        verify { Sentry.setUser(capture(sentryUserSlot)) }
        assertEquals(UserIdTestData.userId.id, sentryUserSlot.captured.id)
    }
}
