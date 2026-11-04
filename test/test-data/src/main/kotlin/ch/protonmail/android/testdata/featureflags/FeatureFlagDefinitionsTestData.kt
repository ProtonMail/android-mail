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

package ch.protonmail.android.testdata.featureflags

import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlagCategory
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlagDefinition

object FeatureFlagDefinitionsTestData {

    fun buildSystemFeatureFlagDefinition(
        key: String = "key",
        name: String = "name",
        description: String = "description",
        defaultValue: Boolean = false
    ) = FeatureFlagDefinition(
        key = key,
        name = name,
        category = FeatureFlagCategory.Global,
        description = description,
        defaultValue = defaultValue
    )

    fun buildFeatureFlagDefinition(
        key: String = "key",
        name: String = "name",
        description: String = "description",
        category: FeatureFlagCategory,
        defaultValue: Boolean = false,
        variants: List<String> = emptyList()
    ) = FeatureFlagDefinition(
        key = key,
        name = name,
        category = category,
        description = description,
        defaultValue = defaultValue,
        variants = variants
    )
}
