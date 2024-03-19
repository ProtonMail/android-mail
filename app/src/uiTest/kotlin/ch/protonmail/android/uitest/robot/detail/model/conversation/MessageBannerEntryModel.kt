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

package ch.protonmail.android.uitest.robot.detail.model.conversation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import ch.protonmail.android.maildetail.presentation.ui.MessageBodyTestTags
import ch.protonmail.android.test.utils.ComposeTestRuleHolder
import ch.protonmail.android.uitest.util.awaitDisplayed
import ch.protonmail.android.uitest.util.child

internal class MessageBannerEntryModel(composeTestRule: ComposeTestRule = ComposeTestRuleHolder.rule) {

    private val rootItem = composeTestRule.onNodeWithTag(MessageBodyTestTags.MessageBodyBanner)

    private val icon = rootItem.child {
        hasTestTag(MessageBodyTestTags.MessageBodyBannerIcon)
    }

    private val text = rootItem.child {
        hasTestTag(MessageBodyTestTags.MessageBodyBannerText)
    }

    // region verification
    fun doesNotExist() {
        rootItem.assertDoesNotExist()
    }

    fun isDisplayedWithText(value: String) {
        rootItem.awaitDisplayed()
        icon.assertIsDisplayed()
        text.assertTextEquals(value)
    }
    // endregion
}
