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

package ch.protonmail.android.featureflags.presentation.viewmodel

import app.cash.turbine.test
import ch.protonmail.android.mailfeatureflags.domain.FeatureFlagOverrideManager
import ch.protonmail.android.mailfeatureflags.domain.FeatureFlagResolver
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlagCategory
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlagDefinition
import ch.protonmail.android.mailfeatureflags.presentation.mapper.FeatureFlagsDefinitionsMapper
import ch.protonmail.android.mailfeatureflags.presentation.model.FeatureFlagListItem
import ch.protonmail.android.mailfeatureflags.presentation.model.FeatureFlagOverridesState
import ch.protonmail.android.mailfeatureflags.presentation.viewmodel.FeatureFlagOverridesViewModel
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.featureflags.FeatureFlagDefinitionsTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class FeatureFlagOverridesViewModelTest {

    private val mapper = mockk<FeatureFlagsDefinitionsMapper>()
    private val overrideManager = mockk<FeatureFlagOverrideManager>(relaxUnitFun = true)
    private val resolver = mockk<FeatureFlagResolver>()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should expose a loaded state with the mapped list items`() = runTest {
        // Given
        val systemFlag = FeatureFlagDefinitionsTestData.buildSystemFeatureFlagDefinition(key = "1")
        val testFlag = FeatureFlagDefinitionsTestData.buildFeatureFlagDefinition(
            key = "2",
            category = FeatureFlagCategory.Test
        )

        val definitions = setOf(systemFlag, testFlag)
        val expectedOverrides = mapOf(systemFlag to false)
        val expectedUiModelsList = mockk<ImmutableList<FeatureFlagListItem>>()

        coEvery { overrideManager.overriddenDebugFlags() } returns mapOf(systemFlag.key to false)
        coEvery { mapper.toFlattenedListUiModel(any(), expectedOverrides) } returns expectedUiModelsList

        // When + Then
        viewModel(definitions).state.test {
            assertEquals(FeatureFlagOverridesState.Loaded(expectedUiModelsList), awaitItem())
        }
    }

    @Test
    fun `should do nothing when the key is not within the definitions`() = runTest {
        // Given
        val flag = FeatureFlagDefinitionsTestData.buildSystemFeatureFlagDefinition(key = "1")
        val definitions = setOf(flag)
        coEvery { overrideManager.overriddenDebugFlags() } returns emptyMap()

        // When
        viewModel(definitions).toggleKey("unknownKey")

        // Then
        coVerify(exactly = 0) { overrideManager.setDebugOverride(any(), any()) }
    }

    @Test
    fun `should flip the resolved value when toggling a non-overridden key`() = runTest {
        // Given
        val flag = FeatureFlagDefinitionsTestData.buildSystemFeatureFlagDefinition(key = "1")
        val definitions = setOf(flag)
        coEvery { overrideManager.overriddenDebugFlags() } returns emptyMap()
        // Resolved value differs from the hardcoded default: the toggle must flip away from what's shown.
        coEvery { resolver.getFeatureFlag(flag.key, flag.defaultValue) } returns true

        // When
        viewModel(definitions).toggleKey(flag.key)

        // Then
        coVerify(exactly = 1) { overrideManager.setDebugOverride(flag.key, false) }
    }

    @Test
    fun `should flip the existing override value when toggling an overridden key`() = runTest {
        // Given
        val flag = FeatureFlagDefinitionsTestData.buildSystemFeatureFlagDefinition(key = "1")
        val definitions = setOf(flag)
        coEvery { overrideManager.overriddenDebugFlags() } returns mapOf(flag.key to true)

        // When
        viewModel(definitions).toggleKey(flag.key)

        // Then
        coVerify(exactly = 1) { overrideManager.setDebugOverride(flag.key, false) }
        coVerify(exactly = 0) { resolver.getFeatureFlag(any(), any()) }
    }

    @Test
    fun `should clear all overrides on the manager when resetting`() = runTest {
        // Given
        val flag = FeatureFlagDefinitionsTestData.buildSystemFeatureFlagDefinition(key = "1")
        val definitions = setOf(flag)
        coEvery { overrideManager.overriddenDebugFlags() } returns emptyMap()
        coEvery { overrideManager.clearAllDebugOverrides() } just runs

        // When
        viewModel(definitions).resetAll()

        // Then
        coVerify(exactly = 1) { overrideManager.clearAllDebugOverrides() }
    }

    private fun viewModel(definitions: Set<FeatureFlagDefinition>) = FeatureFlagOverridesViewModel(
        definitions,
        overrideManager,
        resolver,
        mapper
    )
}
