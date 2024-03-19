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

package ch.protonmail.android.test.utils

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule

object ComposeTestRuleHolder {

    @PublishedApi
    internal val composeContentTestRule: Lazy<ComposeContentTestRule> = lazy {
        createAndroidComposeRule<HiltComponentActivity>()
    }

    @PublishedApi
    internal val composeTestRule: Lazy<ComposeTestRule> = lazy { createEmptyComposeRule() }

    val rule: ComposeTestRule
        get() = when {
            composeTestRule.isInitialized() -> composeTestRule.value
            composeContentTestRule.isInitialized() -> composeContentTestRule.value
            else -> throw IllegalStateException(
                """
                    No ComposeTestRule has been initialized. 
                    Did you forget to call ComposeTestRuleHolder#createAndGetComposeRule?
                """.trimMargin()
            )
        }

    inline fun <reified T : ComposeTestRule> createAndGetComposeRule(): T {
        if (composeContentTestRule.isInitialized() || composeTestRule.isInitialized()) {
            throw IllegalStateException("Cannot instantiate more than one ComposeTestRule at the same time.")
        }

        val rule = when (T::class) {
            ComposeContentTestRule::class -> composeContentTestRule.value
            ComposeTestRule::class -> composeTestRule.value
            else -> throw IllegalStateException("Unable to instantiate a ComposeTestRule. Invalid class type provided.")
        }

        return rule as T
    }
}
