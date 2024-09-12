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

package ch.protonmail.android.uitest.robot.composer.model.chips

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import ch.protonmail.android.test.utils.ComposeTestRuleHolder
import ch.protonmail.android.uicomponents.chips.ChipsTestTags
import ch.protonmail.android.uitest.util.assertions.CustomSemanticsPropertyKeyNames
import ch.protonmail.android.uitest.util.child
import ch.protonmail.android.uitest.util.extensions.getKeyValueByName
import org.junit.Assert.assertEquals

internal class RecipientChipEntryModel(
    index: Int,
    parentMatcher: SemanticsMatcher,
    composeTestRule: ComposeTestRule = ComposeTestRuleHolder.rule
) {

    private val parent = composeTestRule.onNode(
        matcher = hasTestTag("${ChipsTestTags.InputChip}$index") and hasAnyAncestor(parentMatcher),
        useUnmergedTree = true
    )
    private val text = parent.child { hasTestTag(ChipsTestTags.InputChipText) }
    private val errorIcon = parent.child { hasTestTag(ChipsTestTags.InputChipLeadingIcon) }
    private val deleteIcon = parent.child { hasTestTag(ChipsTestTags.InputChipTrailingIcon) }

    // region actions
    fun tapDeleteIcon() = withParentDisplayed {
        deleteIcon.performScrollTo().performClick()
    }
    // endregion

    // region verification
    fun hasText(value: String): RecipientChipEntryModel = withParentDisplayed {
        text.assertTextEquals(value)
    }

    fun hasEmailValidationState(state: RecipientChipValidationState) = withParentDisplayed {
        parent.assertFieldState(state.value)
    }

    fun hasDeleteIcon() = withParentDisplayed {
        deleteIcon.performScrollTo().assertExists()
    }

    fun hasNoDeleteIcon() = withParentDisplayed {
        deleteIcon.assertDoesNotExist()
    }

    fun hasErrorIcon() = withParentDisplayed {
        errorIcon.assertExists()
    }

    fun hasNoErrorIcon() = withParentDisplayed {
        errorIcon.assertDoesNotExist()
    }

    fun doesNotExist() {
        parent.assertDoesNotExist()
    }
    // endregion

    // On some configurations, the transition from raw text to chip might take a bit and make the test fail.
    private fun withParentDisplayed(block: RecipientChipEntryModel.() -> Unit) = apply {
        parent.performScrollTo()
        block()
    }

    private fun SemanticsNodeInteraction.assertFieldState(isValid: Boolean) = apply {
        val isValidProperty = requireNotNull(getKeyValueByName(CustomSemanticsPropertyKeyNames.IsValidFieldKey)) {
            "IsValidFieldKey property was not found on this node. Did you forget to set it?"
        }

        assertEquals(isValid, isValidProperty.value)
    }
}
