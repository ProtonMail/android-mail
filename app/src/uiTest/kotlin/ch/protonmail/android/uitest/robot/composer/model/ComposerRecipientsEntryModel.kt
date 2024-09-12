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

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerTestTags
import ch.protonmail.android.test.utils.ComposeTestRuleHolder
import ch.protonmail.android.uicomponents.chips.ChipsTestTags
import ch.protonmail.android.uitest.robot.composer.model.chips.RecipientChipEntry
import ch.protonmail.android.uitest.robot.composer.model.chips.RecipientChipEntryModel
import ch.protonmail.android.uitest.robot.composer.model.chips.RecipientChipValidationState.Invalid
import ch.protonmail.android.uitest.util.assertions.assertEmptyText
import ch.protonmail.android.uitest.util.awaitHidden

internal sealed class ComposerRecipientsEntryModel(
    private val parentMatcher: SemanticsMatcher,
    private val prefix: Prefix,
    composeTestRule: ComposeTestRule = ComposeTestRuleHolder.rule
) {

    private val parent = composeTestRule.onNode(parentMatcher, useUnmergedTree = true)

    private val prefixField = composeTestRule.onNode(
        matcher = hasTestTag(ComposerTestTags.FieldPrefix) and hasAnyAncestor(parentMatcher),
        useUnmergedTree = true
    )

    private val textField = composeTestRule.onNode(
        // use hasAnyAncestor as the TextField is not a direct child of the parent.
        matcher = hasTestTag(ChipsTestTags.BasicTextField) and hasAnyAncestor(parentMatcher)
    )

    // region actions
    fun typeValue(value: String) = withParentFocused {
        textField.performTextInput(value)
    }

    @OptIn(ExperimentalTestApi::class)
    fun tapKey(key: Key) {
        textField.performKeyInput { this.pressKey(key) }
    }

    fun performImeAction() {
        textField.performImeAction()
    }

    fun focus() {
        parent.performClick()
    }

    fun tapChipDeletionIconAt(position: Int) {
        val model = RecipientChipEntryModel(position, parentMatcher)
        model.tapDeleteIcon()
    }
    // endregion

    // region verification
    fun isHidden() {
        parent.awaitHidden().assertDoesNotExist()
    }

    fun isFocused() = apply {
        textField.assertIsFocused()
    }

    fun hasEmptyValue() = withParentFocused {
        hasPrefix()
        textField.assertEmptyText()

        // Check that chip at index 0 does not exist, as recomposition will always populate index 0 if there's a chip.
        RecipientChipEntryModel(0, parentMatcher).doesNotExist()
    }

    fun hasValue(value: String) = withParentFocused {
        hasPrefix()
        textField.assertTextEquals(value)
    }

    fun hasChips(vararg chips: RecipientChipEntry) {
        for (chip in chips) {
            val model = RecipientChipEntryModel(chip.index, parentMatcher)
            model
                .hasText(chip.text)
                .hasEmailValidationState(chip.state)
                .also {
                    if (chip.hasDeleteIcon) model.hasDeleteIcon() else model.hasNoDeleteIcon()
                }
                .also {
                    if (chip.state is Invalid) model.hasErrorIcon() else model.hasNoErrorIcon()
                }
        }
    }

    fun hasNoChip(chip: RecipientChipEntry) {
        val model = RecipientChipEntryModel(chip.index, parentMatcher)
        model.doesNotExist()
    }

    private fun hasPrefix() = apply {
        prefixField.assertTextEquals(prefix.value)
    }
    // endregion

    // region utility methods
    private fun withParentFocused(block: ComposerRecipientsEntryModel.() -> Unit) = apply {
        // This is needed as even though the correct textField is located, the automation might fill the wrong field.
        parent.performScrollTo()

        if (!peekIsFocused()) {
            parent.performClick()
        }
        block()
    }

    private fun peekIsFocused(): Boolean = runCatching { isFocused() }.isSuccess
    // endregion
}
