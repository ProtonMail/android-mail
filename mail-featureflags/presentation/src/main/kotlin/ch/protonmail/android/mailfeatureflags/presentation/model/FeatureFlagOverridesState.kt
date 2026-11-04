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

package ch.protonmail.android.mailfeatureflags.presentation.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

sealed interface FeatureFlagOverridesState {
    data object Loading : FeatureFlagOverridesState
    data class Loaded(val featureFlagListItems: ImmutableList<FeatureFlagListItem>) : FeatureFlagOverridesState
}

data class FeatureFlagUiModel(
    val key: String,
    val name: String,
    val description: String,
    val enabled: Boolean,
    val overridden: Boolean,
    val variants: ImmutableList<String> = persistentListOf(),
    val selectedVariant: String? = null
)

sealed class FeatureFlagListItem {
    data class Header(val categoryName: String) : FeatureFlagListItem()
    data class FeatureFlag(val model: FeatureFlagUiModel) : FeatureFlagListItem()
}
