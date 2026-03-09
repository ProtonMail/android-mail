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
import dagger.Lazy
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Before
import org.junit.Rule
import uniffi.mail_uniffi.MailSessionIsFeatureEnabledResult
import uniffi.mail_uniffi.MailUserSessionIsFeatureEnabledResult
import uniffi.mail_uniffi.ProtonError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class UnleashFeatureFlagValueProviderTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private val scope = CoroutineScope(mainDispatcherRule.testDispatcher)
    private val sessionFacade: SessionFacade = mockk()
    private val sessionLazy: Lazy<SessionFacade> = mockk()

    private lateinit var provider: UnleashFeatureFlagValueProvider

    private val testFeatureFlagKey = "test_feature"
    private val userId = UserId("user-123")
    private val appFeatureFlagValue = true

    @Before
    fun setup() {
        // Given
        clearMocks(sessionFacade, sessionLazy)

        every { sessionLazy.get() } returns sessionFacade

        provider = UnleashFeatureFlagValueProvider(
            sessionLazy,
            scope
        )

        coEvery { sessionFacade.getIsMailSessionFeatureEnabled(testFeatureFlagKey) } returns
            MailSessionIsFeatureEnabledResult.Ok(
                appFeatureFlagValue
            )
    }

    @Test
    fun `should return app FF as fallback value when mail session is not initialized`() = runTest {
        // Given
        coEvery { sessionFacade.isMailSessionInitialised() } returns false

        // When
        val result = provider.getFeatureFlagValue(testFeatureFlagKey)

        // Then
        coVerify(exactly = 1) { sessionFacade.isMailSessionInitialised() }
        // Verify that the fallback method on the facade was called
        coVerify(exactly = 1) { sessionFacade.getIsMailSessionFeatureEnabled(testFeatureFlagKey) }
        coVerify(exactly = 0) { sessionFacade.getIsUserSessionFeatureEnabled(any(), any()) }
        assertNotNull(appFeatureFlagValue)
        assertEquals(result, appFeatureFlagValue)
    }

    @Test
    fun `should return app FF as fallback value when mail session is initialized but user id is null`() = runTest {
        // Given
        coEvery { sessionFacade.isMailSessionInitialised() } returns true
        coEvery { sessionFacade.getUserId() } returns null

        // When
        val result = provider.getFeatureFlagValue(testFeatureFlagKey)

        // Then
        coVerify(exactly = 1) { sessionFacade.isMailSessionInitialised() }
        coVerify(exactly = 1) { sessionFacade.getUserId() }
        coVerify(exactly = 1) { sessionFacade.getIsMailSessionFeatureEnabled(testFeatureFlagKey) }
        coVerify(exactly = 0) { sessionFacade.getIsUserSessionFeatureEnabled(any(), any()) }
        assertNotNull(appFeatureFlagValue)
        assertEquals(result, appFeatureFlagValue)
    }

    @Test
    fun `should return true when user session check returns enabled`() = runTest {
        // Given
        coEvery { sessionFacade.isMailSessionInitialised() } returns true
        coEvery { sessionFacade.getUserId() } returns userId
        coEvery {
            sessionFacade.getIsUserSessionFeatureEnabled(
                key = testFeatureFlagKey,
                userId = userId
            )
        } returns MailUserSessionIsFeatureEnabledResult.Ok(v1 = true)

        // When
        val result = provider.getFeatureFlagValue(testFeatureFlagKey)

        // Then
        coVerify(exactly = 1) { sessionFacade.isMailSessionInitialised() }
        coVerify(exactly = 1) { sessionFacade.getUserId() }
        coVerify(exactly = 1) {
            sessionFacade
                .getIsUserSessionFeatureEnabled(key = testFeatureFlagKey, userId = userId)
        }
        assertNotNull(result)
        assertEquals(result, appFeatureFlagValue)
    }

    @Test
    fun `should return false value when user session check returns disabled`() = runTest {
        // Given
        coEvery { sessionFacade.isMailSessionInitialised() } returns true
        coEvery { sessionFacade.getUserId() } returns userId
        coEvery {
            sessionFacade.getIsUserSessionFeatureEnabled(
                key = testFeatureFlagKey,
                userId = userId
            )
        } returns MailUserSessionIsFeatureEnabledResult.Ok(v1 = false)

        // When
        val result = provider.getFeatureFlagValue(testFeatureFlagKey)

        // Then
        coVerify(exactly = 1) { sessionFacade.isMailSessionInitialised() }
        coVerify(exactly = 1) { sessionFacade.getUserId() }
        coVerify(exactly = 1) {
            sessionFacade
                .getIsUserSessionFeatureEnabled(key = testFeatureFlagKey, userId = userId)
        }
        assertNotNull(result)
        assertFalse(result)
    }

    @Test
    fun `should return null value when user session feature check returns error`() = runTest {
        // Given
        coEvery { sessionFacade.isMailSessionInitialised() } returns true
        coEvery { sessionFacade.getUserId() } returns userId
        coEvery {
            sessionFacade.getIsUserSessionFeatureEnabled(
                key = testFeatureFlagKey,
                userId = userId
            )
        } returns MailUserSessionIsFeatureEnabledResult.Error(
            ProtonError.Network
        )

        // When
        val result = provider.getFeatureFlagValue(testFeatureFlagKey)

        // Then
        coVerify(exactly = 1) { sessionFacade.isMailSessionInitialised() }
        coVerify(exactly = 1) { sessionFacade.getUserId() }
        coVerify(exactly = 1) {
            sessionFacade
                .getIsUserSessionFeatureEnabled(key = testFeatureFlagKey, userId = userId)
        }
        assertNull(result)
    }

    @Test
    fun `should return null value when user session feature check returns null result`() = runTest {
        // Given
        coEvery { sessionFacade.isMailSessionInitialised() } returns true
        coEvery { sessionFacade.getUserId() } returns userId
        coEvery { sessionFacade.getIsUserSessionFeatureEnabled(key = testFeatureFlagKey, userId = userId) } returns null

        // When
        val result = provider.getFeatureFlagValue(testFeatureFlagKey)

        // Then
        coVerify(exactly = 1) { sessionFacade.isMailSessionInitialised() }
        coVerify(exactly = 1) { sessionFacade.getUserId() }
        coVerify(exactly = 1) {
            sessionFacade
                .getIsUserSessionFeatureEnabled(key = testFeatureFlagKey, userId = userId)
        }
        assertNull(result)
    }
}
