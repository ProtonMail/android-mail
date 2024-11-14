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

package ch.protonmail.android.uitest.e2e.composer.chips

import ch.protonmail.android.uitest.e2e.composer.ComposerTests
import ch.protonmail.android.uitest.robot.composer.model.chips.ChipsCreationTrigger
import ch.protonmail.android.uitest.robot.composer.model.chips.RecipientChipEntry
import ch.protonmail.android.uitest.robot.composer.model.chips.RecipientChipValidationState
import ch.protonmail.android.uitest.robot.composer.section.recipients.ComposerRecipientsSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.verify

internal interface ComposerChipsTests : ComposerTests {

    fun ComposerRecipientsSection.createAndVerifyChip(
        state: RecipientChipValidationState,
        trigger: ChipsCreationTrigger = ChipsCreationTrigger.ImeAction
    ) {
        val text = when (state) {
            is RecipientChipValidationState.Valid -> "rec@ipient.com"
            is RecipientChipValidationState.Invalid -> "test"
        }

        val chip = RecipientChipEntry(
            index = 0, text = text, state = state
        )

        typeRecipient(chip.text)
        triggerChipCreation(trigger)

        verify {
            hasRecipientChips(
                chip.copy(hasDeleteIcon = true)
            )
        }
    }

    fun withMultipleRecipients(
        size: Int,
        state: RecipientChipValidationState,
        block: (RecipientChipEntry) -> Any
    ) {
        (0 until size).forEach { index ->
            val recipient = StringBuilder("test$index").apply {
                if (state == RecipientChipValidationState.Valid) append("@email.com")
            }.toString()

            val recipientChipEntry = RecipientChipEntry(
                index = index,
                text = recipient,
                hasDeleteIcon = true,
                state = state
            )

            block(recipientChipEntry)
        }
    }
}
