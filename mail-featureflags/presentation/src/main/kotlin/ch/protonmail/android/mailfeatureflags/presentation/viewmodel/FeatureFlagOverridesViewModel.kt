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

package ch.protonmail.android.mailfeatureflags.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailfeatureflags.domain.FeatureFlagOverrideManager
import ch.protonmail.android.mailfeatureflags.domain.FeatureFlagResolver
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlagDefinition
import ch.protonmail.android.mailfeatureflags.presentation.mapper.FeatureFlagsDefinitionsMapper
import ch.protonmail.android.mailfeatureflags.presentation.model.FeatureFlagOverridesState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeatureFlagOverridesViewModel @Inject constructor(
    private val definitions: Set<@JvmSuppressWildcards FeatureFlagDefinition>,
    private val overrideManager: FeatureFlagOverrideManager,
    private val resolver: FeatureFlagResolver,
    private val mapper: FeatureFlagsDefinitionsMapper
) : ViewModel() {

    // Rust exposes overrides as a one-shot list (not a Flow), so we re-read on each refresh tick.
    private val refreshTrigger = MutableStateFlow(0)

    val state: StateFlow<FeatureFlagOverridesState> =
        refreshTrigger.mapLatest {
            val overrides = currentOverrides()
            val groupedDefinitions = definitions.groupBy { it.category }
            FeatureFlagOverridesState.Loaded(mapper.toFlattenedListUiModel(groupedDefinitions, overrides))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FeatureFlagOverridesState.Loading
        )

    fun toggleKey(key: String) {
        val definition = definitions.firstOrNull { it.key == key } ?: return
        viewModelScope.launch {
            overrideManager.setDebugOverride(key, !currentValue(definition))
            refresh()
        }
    }

    fun resetAll() {
        viewModelScope.launch {
            overrideManager.clearAllDebugOverrides()
            refresh()
        }
    }

    /**
     * The value currently shown for [definition] in the list: the override when one is set,
     * otherwise the resolved flag value. Mirrors [FeatureFlagsDefinitionsMapper] so a toggle always
     * flips away from what the user sees rather than from the hardcoded default.
     */
    private suspend fun currentValue(definition: FeatureFlagDefinition): Boolean {
        val overrides = overrideManager.overriddenDebugFlags()
        return if (overrides.containsKey(definition.key)) {
            overrides[definition.key] ?: definition.defaultValue
        } else {
            resolver.getFeatureFlag(definition.key, definition.defaultValue)
        }
    }

    private suspend fun currentOverrides(): Map<FeatureFlagDefinition, Boolean> {
        val overriddenByName = overrideManager.overriddenDebugFlags()
        return definitions.mapNotNull { definition ->
            if (!overriddenByName.containsKey(definition.key)) return@mapNotNull null
            definition to (overriddenByName[definition.key] ?: definition.defaultValue)
        }.toMap()
    }

    private fun refresh() {
        refreshTrigger.update { it + 1 }
    }
}
