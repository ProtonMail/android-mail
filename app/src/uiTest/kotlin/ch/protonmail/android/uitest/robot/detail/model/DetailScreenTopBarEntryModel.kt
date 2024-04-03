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

package ch.protonmail.android.uitest.robot.detail.model

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.protonmail.android.maildetail.presentation.ui.DetailScreenTopBarTestTags
import ch.protonmail.android.uitest.util.child

internal class DetailScreenTopBarEntryModel(composeTestRule: ComposeTestRule) {

    private val rootItem = composeTestRule
        .onNodeWithTag(
            testTag = DetailScreenTopBarTestTags.RootItem,
            useUnmergedTree = true
        )

    private val backButton = composeTestRule.onNodeWithTag(
        testTag = DetailScreenTopBarTestTags.BackButton,
        useUnmergedTree = true
    )

    private val subject = rootItem.child {
        hasTestTag(DetailScreenTopBarTestTags.Subject)
    }

    // region actions
    fun tapBack() {
        backButton.performClick()
    }
    // endregion

    // region verification
    fun hasSubject(value: String) = apply {
        subject.onChild().assertTextEquals(value)
    }
    // endregion
}
