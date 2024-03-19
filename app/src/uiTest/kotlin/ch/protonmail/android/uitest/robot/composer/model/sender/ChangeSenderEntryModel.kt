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

package ch.protonmail.android.uitest.robot.composer.model.sender

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailcomposer.presentation.ui.ChangeSenderBottomSheetTestTags
import ch.protonmail.android.test.utils.ComposeTestRuleHolder
import ch.protonmail.android.uitest.util.awaitDisplayed
import ch.protonmail.android.uitest.util.awaitHidden

internal class ChangeSenderEntryModel(index: Int, composeTestRule: ComposeTestRule = ComposeTestRuleHolder.rule) {

    private val item = composeTestRule.onNodeWithTag("${ChangeSenderBottomSheetTestTags.Item}$index")

    // region action
    fun selectSender() {
        item.awaitDisplayed().performClick().awaitHidden()
    }
    // endregion

    // region verification
    fun doesNotExist() {
        item.assertDoesNotExist()
    }

    fun hasText(value: String) {
        item.awaitDisplayed().assertTextEquals(value)
    }
    // endregion
}
