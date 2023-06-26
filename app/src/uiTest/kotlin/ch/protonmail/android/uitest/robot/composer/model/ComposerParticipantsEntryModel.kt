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

package ch.protonmail.android.uitest.robot.composer.model

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerTestTags
import ch.protonmail.android.uitest.util.ComposeTestRuleHolder

internal sealed class ComposerParticipantsEntryModel(
    protected val parentMatcher: SemanticsMatcher,
    protected val composeTestRule: ComposeTestRule = ComposeTestRuleHolder.rule
) {

    // To be made private after the Sender Field has been finalized.
    protected val parent = composeTestRule.onNode(parentMatcher, useUnmergedTree = true)

    private val prefix = composeTestRule.onNode(
        matcher = hasTestTag(ComposerTestTags.FieldPrefix) and hasAnyAncestor(parentMatcher),
        useUnmergedTree = true
    )

    // region actions
    abstract fun typeValue(value: String): ComposerParticipantsEntryModel
    //endregion

    // region verification
    abstract fun isFocused(): ComposerParticipantsEntryModel

    abstract fun hasValue(value: String): ComposerParticipantsEntryModel

    abstract fun hasEmptyValue(): ComposerParticipantsEntryModel

    fun hasPrefix(value: String): ComposerParticipantsEntryModel = apply {
        prefix.assertTextEquals(value)
    }
    //endregion

    // region utility methods
    protected fun withParentFocused(block: ComposerParticipantsEntryModel.() -> Unit) = apply {
        // This is needed as even though the correct textField is located, the automation might fill the wrong field.
        parent.performClick()
        block()
    }
    // endregion
}
