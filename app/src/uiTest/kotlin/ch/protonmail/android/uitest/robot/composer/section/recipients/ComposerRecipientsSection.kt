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

package ch.protonmail.android.uitest.robot.composer.section.recipients

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.composer.model.ComposerRecipientsEntryModel
import ch.protonmail.android.uitest.robot.composer.model.chips.ChipsCreationTrigger
import ch.protonmail.android.uitest.robot.composer.model.chips.RecipientChipEntry

internal abstract class ComposerRecipientsSection(
    private val entryModel: ComposerRecipientsEntryModel
) : ComposeSectionRobot() {

    fun focusField() {
        entryModel.focus()
    }

    fun typeRecipient(value: String, autoConfirm: Boolean = false) = apply {
        entryModel.typeValue(value)
        if (autoConfirm) triggerChipCreation(ChipsCreationTrigger.Spacebar)
    }

    fun typeMultipleRecipients(vararg values: String) {
        values.forEach {
            typeRecipient(it)
            triggerChipCreation(ChipsCreationTrigger.Spacebar)
        }
    }

    fun tapRecipientField() = apply {
        entryModel.focus()
    }

    fun triggerChipCreation(trigger: ChipsCreationTrigger = ChipsCreationTrigger.ImeAction) = apply {
        when (trigger) {
            ChipsCreationTrigger.ImeAction -> tapImeAction()
            ChipsCreationTrigger.Spacebar -> tapSpacebar()
        }
    }

    fun deleteChipAt(position: Int) = apply {
        entryModel.tapChipDeletionIconAt(position)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    fun tapBackspace() = apply {
        entryModel.tapKey(Key.Backspace)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun tapSpacebar() = apply {
        entryModel.tapKey(Key.Spacebar)
    }

    private fun tapImeAction() = apply {
        entryModel.performImeAction()
    }

    @VerifiesOuter
    inner class Verify {

        fun isFieldFocused() = apply {
            entryModel.isFocused()
        }

        fun isHidden() = apply {
            entryModel.isHidden()
        }

        fun isEmptyField() = apply {
            entryModel.hasEmptyValue()
        }

        fun hasRecipient(value: String) = apply {
            entryModel.hasValue(value)
        }

        fun hasRecipientChips(vararg chips: RecipientChipEntry) = apply {
            entryModel.hasChips(*chips)
        }

        fun recipientChipIsNotDisplayed(chip: RecipientChipEntry) = apply {
            entryModel.hasNoChip(chip)
        }
    }
}
