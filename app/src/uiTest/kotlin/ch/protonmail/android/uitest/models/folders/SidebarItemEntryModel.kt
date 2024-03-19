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

package ch.protonmail.android.uitest.models.folders

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import ch.protonmail.android.maillabel.presentation.sidebar.SidebarCustomLabelTestTags
import ch.protonmail.android.test.utils.ComposeTestRuleHolder
import ch.protonmail.android.uitest.util.assertions.assertTintColor
import ch.protonmail.android.uitest.util.child

internal sealed class SidebarItemEntryModel(
    position: Int,
    matcher: SemanticsMatcher,
    composeTestRule: ComposeTestRule = ComposeTestRuleHolder.rule
) {

    private val rootItem = composeTestRule.onAllNodes(
        matcher = matcher,
        useUnmergedTree = true
    )[position]

    private val icon = rootItem.child {
        hasTestTag(SidebarCustomLabelTestTags.Icon)
    }

    private val text = rootItem.child {
        hasTestTag(SidebarCustomLabelTestTags.Text)
    }

    fun hasText(value: String) = apply {
        text.assertTextEquals(value)
    }

    fun withIconTint(tint: Tint) = apply {
        icon.assertTintColor(tint)
    }
}
