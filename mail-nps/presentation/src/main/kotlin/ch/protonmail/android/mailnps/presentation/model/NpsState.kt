/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailnps.presentation.model

import androidx.compose.runtime.Immutable
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailnps.domain.model.NpsScore

/**
 * State of the NPS bottom sheet. The comment field and the enabled "Share" button only appear once a score
 * is picked, so both are derived from [selectedScore]. [isAnswered] is set once a score is shared or the
 * sheet is dismissed, so closing the sheet after sharing is not recorded as a dismissal. [dismiss] and
 * [showThankYou] are one-shot effects consumed by the host (close the sheet, show the thank-you toast).
 */
@Immutable
data class NpsState(
    val selectedScore: NpsScore?,
    val comment: String,
    val isAnswered: Boolean,
    val dismiss: Effect<Unit>,
    val showThankYou: Effect<Unit>
) {

    val isScoreSelected: Boolean = selectedScore != null

    companion object {

        val Initial = NpsState(
            selectedScore = null,
            comment = "",
            isAnswered = false,
            dismiss = Effect.empty(),
            showThankYou = Effect.empty()
        )
    }
}
