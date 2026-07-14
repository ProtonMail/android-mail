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

import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import uniffi.mail_uniffi.DebugFeatureFlagOverride
import uniffi.mail_uniffi.DebugFeatureFlagOverrideEntry
import uniffi.mail_uniffi.MailUserSession
import uniffi.mail_uniffi.MailUserSessionClearAllDebugFeatureFlagOverridesResult
import uniffi.mail_uniffi.MailUserSessionListDebugFeatureFlagOverridesResult
import uniffi.mail_uniffi.MailUserSessionSetDebugFeatureFlagOverrideResult
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class RustFeatureFlagOverrideManagerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val rustUserSession: MailUserSession = mockk()
    private val sessionResolver: RustFeatureFlagSessionResolver = mockk {
        coEvery { activeSession() } returns ActiveSession.User(rustUserSession)
    }

    private val manager = RustFeatureFlagOverrideManager(
        sessionResolver = sessionResolver,
        ioDispatcher = mainDispatcherRule.testDispatcher
    )

    private val key = "test_feature"

    @Test
    fun `sets a debug override with the requested value`() = runTest {
        // Given
        coEvery {
            rustUserSession.setDebugFeatureFlagOverride(key, DebugFeatureFlagOverride(enabled = true, variant = null))
        } returns MailUserSessionSetDebugFeatureFlagOverrideResult.Ok

        // When
        manager.setDebugOverride(key, enabled = true)

        // Then
        coVerify(exactly = 1) {
            rustUserSession.setDebugFeatureFlagOverride(key, DebugFeatureFlagOverride(enabled = true, variant = null))
        }
    }

    @Test
    fun `maps overridden flags to their name and enabled value`() = runTest {
        // Given
        val entries = listOf(
            DebugFeatureFlagOverrideEntry("flag_a", DebugFeatureFlagOverride(enabled = true, variant = null)),
            DebugFeatureFlagOverrideEntry("flag_b", DebugFeatureFlagOverride(enabled = null, variant = null))
        )
        coEvery { rustUserSession.listDebugFeatureFlagOverrides() } returns
            MailUserSessionListDebugFeatureFlagOverridesResult.Ok(entries)

        // When
        val result = manager.overriddenDebugFlags()

        // Then
        assertEquals(mapOf("flag_a" to true, "flag_b" to null), result)
    }

    @Test
    fun `clear all lists the overridden flags and clears each one`() = runTest {
        // Given
        val entries = listOf(
            DebugFeatureFlagOverrideEntry("flag_a", DebugFeatureFlagOverride(enabled = true, variant = null)),
            DebugFeatureFlagOverrideEntry("flag_b", DebugFeatureFlagOverride(enabled = false, variant = null))
        )
        coEvery { rustUserSession.clearAllDebugFeatureFlagOverrides() } returns
            MailUserSessionClearAllDebugFeatureFlagOverridesResult.Ok

        // When
        manager.clearAllDebugOverrides()

        // Then
        coVerify(exactly = 1) { rustUserSession.clearAllDebugFeatureFlagOverrides() }
    }
}
