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

package ch.protonmail.android.uitest.e2e.mailbox.drafts

import ch.protonmail.android.uitest.robot.composer.ComposerRobot
import ch.protonmail.android.uitest.robot.composer.model.chips.RecipientChipEntry
import ch.protonmail.android.uitest.robot.composer.section.messageBodySection
import ch.protonmail.android.uitest.robot.composer.section.recipients.bccRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.ccRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.toRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.verify
import ch.protonmail.android.uitest.robot.composer.section.subjectSection
import ch.protonmail.android.uitest.robot.composer.section.verify

internal interface OpenExistingDraftsTest {

    fun ComposerRobot.verifyPrefilledFields(
        toRecipientChip: RecipientChipEntry,
        ccRecipientChip: RecipientChipEntry? = null,
        bccRecipientChip: RecipientChipEntry? = null,
        subject: String,
        messageBody: String? = null
    ) {
        toRecipientSection {
            verify { hasRecipientChips(toRecipientChip) }
        }

        if (ccRecipientChip != null && bccRecipientChip != null) {
            toRecipientSection { verify { chevronNotVisible() } }
            ccRecipientSection { verify { hasRecipientChips(ccRecipientChip) } }
            bccRecipientSection { verify { hasRecipientChips(bccRecipientChip) } }
        } else {
            toRecipientSection { expandCcAndBccFields() }
            ccRecipientSection { verify { isEmptyField() } }
            bccRecipientSection { verify { isEmptyField() } }
        }

        subjectSection { verify { hasSubject(subject) } }
        messageBodySection { verify { messageBody?.let { hasText(it) } ?: hasPlaceholderText() } }
    }

    fun ComposerRobot.verifyEmptyFields() {
        toRecipientSection {
            verify { isEmptyField() }
            expandCcAndBccFields()
        }
        ccRecipientSection { verify { isEmptyField() } }
        bccRecipientSection { verify { isEmptyField() } }
        subjectSection { verify { hasEmptySubject() } }
        messageBodySection { verify { hasPlaceholderText() } }
    }
}
