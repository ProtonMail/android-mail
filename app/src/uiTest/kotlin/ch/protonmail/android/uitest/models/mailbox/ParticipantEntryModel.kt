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

package ch.protonmail.android.uitest.models.mailbox

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import ch.protonmail.android.mailcommon.presentation.compose.OfficialBadgeTestTags
import ch.protonmail.android.mailmailbox.presentation.mailbox.ParticipantsListTestTags
import ch.protonmail.android.uitest.util.child
import ch.protonmail.android.uitest.util.children
import ch.protonmail.android.uitest.util.getTestString
import ch.protonmail.android.test.R as testR

internal class ParticipantEntryModel(
    val index: Int,
    val parent: SemanticsNodeInteraction
) {

    private val participantRow: SemanticsNodeInteraction by lazy {
        parent.children { hasTestTag(ParticipantsListTestTags.ParticipantRow) }[index]
    }

    private val participant: SemanticsNodeInteraction by lazy {
        participantRow.child { hasTestTag(ParticipantsListTestTags.Participant) }
    }

    private val badge: SemanticsNodeInteraction by lazy {
        participantRow.child { hasTestTag(OfficialBadgeTestTags.Item) }
    }

    // If the model has no participants, the field is a direct child of the parent (has no row as ancestor).
    private val noParticipant: SemanticsNodeInteraction by lazy {
        parent.child { hasTestTag(ParticipantsListTestTags.NoParticipant) }
    }

    fun hasParticipant(value: String) = apply {
        participant.assertTextEquals(value)
        noParticipant.assertDoesNotExist()
    }

    fun hasNoParticipant(value: String) {
        noParticipant.assertTextEquals(value)
        participantRow.assertDoesNotExist()
    }

    fun isProton(value: Boolean) {
        if (value) {
            badge.assertIsDisplayed()
            badge.assertTextEquals(getTestString(testR.string.test_auth_badge_official))
        } else {
            badge.assertDoesNotExist()
        }
    }
}
