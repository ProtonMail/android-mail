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

package ch.protonmail.android.mailfeatureflags.presentation.mapper

import arrow.fx.coroutines.parMap
import ch.protonmail.android.mailfeatureflags.domain.FeatureFlagResolver
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlagCategory
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlagDefinition
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlagOverride
import ch.protonmail.android.mailfeatureflags.presentation.model.FeatureFlagListItem
import ch.protonmail.android.mailfeatureflags.presentation.model.FeatureFlagUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

data class FeatureFlagsDefinitionsMapper @Inject constructor(
    private val resolver: FeatureFlagResolver
) {

    suspend fun toFlattenedListUiModel(
        groupedDefinitions: Map<FeatureFlagCategory, List<@JvmSuppressWildcards FeatureFlagDefinition>>,
        overrides: Map<String, FeatureFlagOverride>
    ): ImmutableList<FeatureFlagListItem> {
        return groupedDefinitions.flatMap { (category, flags) ->
            buildList {
                add(FeatureFlagListItem.Header(category.name))
                addAll(
                    flags.mapToUiModel(overrides).map { uiModel ->
                        FeatureFlagListItem.FeatureFlag(uiModel)
                    }
                )
            }
        }.toImmutableList()
    }

    private suspend fun List<@JvmSuppressWildcards FeatureFlagDefinition>.mapToUiModel(
        overrides: Map<String, FeatureFlagOverride>
    ) = this.parMap { definition ->
        val override = overrides[definition.key]
        val isEnabled = if (override != null) {
            override.enabled ?: definition.defaultValue
        } else {
            resolver.getFeatureFlag(definition.key, definition.defaultValue)
        }

        FeatureFlagUiModel(
            key = definition.key,
            name = definition.name,
            description = definition.description,
            enabled = isEnabled,
            overridden = override != null,
            variants = definition.variants.toImmutableList(),
            selectedVariant = override?.variant
        )
    }
}
