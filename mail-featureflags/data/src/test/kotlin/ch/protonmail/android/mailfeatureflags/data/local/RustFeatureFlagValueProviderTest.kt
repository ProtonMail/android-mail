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

package ch.protonmail.android.mailfeatureflags.data.local

import ch.protonmail.android.mailfeatureflags.domain.FeatureFlagProviderPriority
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailSessionIsFeatureEnabledResult
import uniffi.mail_uniffi.MailUserSession
import uniffi.mail_uniffi.MailUserSessionIsFeatureEnabledResult
import uniffi.mail_uniffi.ProtonError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class RustFeatureFlagValueProviderTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val rustUserSession: MailUserSession = mockk()
    private val rustMailSession: MailSession = mockk()
    private val sessionResolver: RustFeatureFlagSessionResolver = mockk()

    private val provider = RustFeatureFlagValueProvider(
        sessionResolver = Lazy { sessionResolver },
        ioDispatcher = mainDispatcherRule.testDispatcher
    )

    private val key = "test_feature"

    @Test
    fun `has the Rust provider priority`() {
        assertEquals(FeatureFlagProviderPriority.RustProvider, provider.priority)
    }

    @Test
    fun `reads from the user session when logged in`() = runTest {
        // Given
        coEvery { sessionResolver.activeSession() } returns ActiveSession.User(rustUserSession)
        coEvery { rustUserSession.isFeatureEnabled(featureId = key) } returns
            MailUserSessionIsFeatureEnabledResult.Ok(true)

        // When
        val result = provider.getFeatureFlagValue(key)

        // Then
        assertEquals(true, result)
    }

    @Test
    fun `reads from the app session when not logged in`() = runTest {
        // Given
        coEvery { sessionResolver.activeSession() } returns ActiveSession.App(rustMailSession)
        coEvery { rustMailSession.isFeatureEnabled(featureId = key) } returns
            MailSessionIsFeatureEnabledResult.Ok(false)

        // When
        val result = provider.getFeatureFlagValue(key)

        // Then
        assertEquals(false, result)
    }

    @Test
    fun `returns null when the session read errors`() = runTest {
        // Given
        coEvery { sessionResolver.activeSession() } returns ActiveSession.User(rustUserSession)
        coEvery { rustUserSession.isFeatureEnabled(featureId = key) } returns
            MailUserSessionIsFeatureEnabledResult.Error(ProtonError.Network)

        // When
        val result = provider.getFeatureFlagValue(key)

        // Then
        assertNull(result)
    }
}
