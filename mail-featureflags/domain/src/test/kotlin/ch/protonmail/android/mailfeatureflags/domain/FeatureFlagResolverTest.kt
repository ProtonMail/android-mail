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

package ch.protonmail.android.mailfeatureflags.domain

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class FeatureFlagResolverTest {

    @Test
    fun `should default to the default value when no providers are present`() = runTest {
        // Given
        val resolver = FeatureFlagResolver(emptySet())

        // When + Then
        val actual = resolver.getFeatureFlag(FeatureFlagKey, false)
        assertFalse(actual)
    }

    @Test
    fun `should return default value when no provider is enabled`() = runTest {
        // Given
        val notEnabledProvider1 = mockk<FeatureFlagValueProvider> {
            coEvery { this@mockk.getFeatureFlagValue(FeatureFlagKey) } returns true
            every { name } returns "name"
            every { priority } returns 0
            every { isEnabled() } returns false
        }

        val notEnabledProvider2 = mockk<FeatureFlagValueProvider> {
            coEvery { this@mockk.getFeatureFlagValue(FeatureFlagKey) } returns true
            every { name } returns "name"
            every { priority } returns 10
            every { isEnabled() } returns false
        }
        val resolver = FeatureFlagResolver(setOf(notEnabledProvider1, notEnabledProvider2))

        // When
        val actual = resolver.getFeatureFlag(FeatureFlagKey, false)

        // Then
        assertFalse(actual)
        coVerify(exactly = 0) { notEnabledProvider1.getFeatureFlagValue(FeatureFlagKey) }
        coVerify(exactly = 0) { notEnabledProvider2.getFeatureFlagValue(FeatureFlagKey) }
    }

    @Test
    fun `should ignore higher priority providers that are not enabled`() = runTest {
        // Given
        val enabledProvider = mockk<FeatureFlagValueProvider> {
            coEvery { this@mockk.getFeatureFlagValue(FeatureFlagKey) } returns true
            every { name } returns "name"
            every { priority } returns 0
            every { isEnabled() } returns true
        }

        val notEnabledProvider = mockk<FeatureFlagValueProvider> {
            coEvery { this@mockk.getFeatureFlagValue(FeatureFlagKey) } returns false
            every { name } returns "name"
            every { priority } returns 10
            every { isEnabled() } returns false
        }
        val resolver = FeatureFlagResolver(setOf(enabledProvider, notEnabledProvider))

        // When + Then
        val actual = resolver.getFeatureFlag(FeatureFlagKey, false)
        assertTrue(actual)

        coVerify(exactly = 1) { enabledProvider.getFeatureFlagValue(FeatureFlagKey) }
        coVerify(exactly = 0) { notEnabledProvider.getFeatureFlagValue(FeatureFlagKey) }
    }

    @Test
    fun `should return default when the feature key is unknown to the given providers`() = runTest {
        // Given
        val provider = mockk<FeatureFlagValueProvider> {
            coEvery { this@mockk.getFeatureFlagValue(FeatureFlagKey) } returns null
            every { name } returns "name"
            every { priority } returns 0
            every { isEnabled() } returns true
        }
        val resolver = FeatureFlagResolver(setOf(provider))

        // When
        val actual = resolver.getFeatureFlag(FeatureFlagKey, false)

        // Then
        assertFalse(actual)
        coVerify(exactly = 1) { provider.getFeatureFlagValue(FeatureFlagKey) }
    }

    @Test
    fun `should respect providers priority when resolving a feature flag`() = runTest {
        // Given
        val lowPriorityProvider = mockk<FeatureFlagValueProvider> {
            coEvery { this@mockk.getFeatureFlagValue(any<String>()) } returns false
            every { name } returns "name"
            every { priority } returns 0
            every { isEnabled() } returns true
        }

        val topPriorityProvider = mockk<FeatureFlagValueProvider> {
            coEvery { this@mockk.getFeatureFlagValue(any<String>()) } returns true
            every { name } returns "name"
            every { priority } returns 10
            every { isEnabled() } returns true
        }

        val resolver = FeatureFlagResolver(setOf(lowPriorityProvider, topPriorityProvider))

        // When
        val actual = resolver.getFeatureFlag(FeatureFlagKey, false)

        // Then
        assertTrue(actual)
        coVerify(exactly = 1) { topPriorityProvider.getFeatureFlagValue(FeatureFlagKey) }
        coVerify(exactly = 0) { lowPriorityProvider.getFeatureFlagValue(FeatureFlagKey) }
    }

    private companion object {

        const val FeatureFlagKey = "ff-key"
    }
}
