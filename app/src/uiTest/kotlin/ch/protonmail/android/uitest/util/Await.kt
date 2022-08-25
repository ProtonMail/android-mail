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

package ch.protonmail.android.uitest.util

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import me.proton.core.compose.component.PROTON_PROGRESS_TEST_TAG
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun ComposeContentTestRule.awaitProgressIsHidden() {
    onNodeWithTag(PROTON_PROGRESS_TEST_TAG)
        .awaitHidden(this)
}

fun SemanticsNodeInteraction.awaitDisplayed(
    composeTestRule: ComposeContentTestRule,
    timeout: Duration = 1.seconds
): SemanticsNodeInteraction = also {
    composeTestRule.waitUntil(timeout.inWholeMilliseconds) { nodeIsDisplayed(this) }
}

fun SemanticsNodeInteraction.awaitHidden(composeTestRule: ComposeContentTestRule): SemanticsNodeInteraction =
    also { composeTestRule.waitUntil { nodeIsNotDisplayed(this) } }
