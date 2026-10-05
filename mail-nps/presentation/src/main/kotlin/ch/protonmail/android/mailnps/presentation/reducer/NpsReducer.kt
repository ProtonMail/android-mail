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

package ch.protonmail.android.mailnps.presentation.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailnps.presentation.model.NpsOperation
import ch.protonmail.android.mailnps.presentation.model.NpsOperation.NpsAction
import ch.protonmail.android.mailnps.presentation.model.NpsOperation.NpsEvent
import ch.protonmail.android.mailnps.presentation.model.NpsState
import javax.inject.Inject

class NpsReducer @Inject constructor() {

    fun newStateFrom(current: NpsState, operation: NpsOperation): NpsState = when (operation) {
        is NpsAction.ScoreSelected -> current.copy(selectedScore = operation.score)
        is NpsAction.CommentChanged -> current.copy(comment = operation.comment)
        // Answered right away, so the response is only recorded once while it is being sent.
        is NpsAction.ShareClicked,
        is NpsAction.SheetDismissed -> current.copy(isAnswered = true)

        is NpsEvent.Submitted -> current.copy(
            showThankYou = Effect.of(Unit),
            dismiss = Effect.of(Unit)
        )

        // The host already closed the sheet, so there is nothing to dismiss.
        is NpsEvent.Dismissed -> current
    }
}
