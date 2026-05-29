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

package ch.protonmail.android.maillabel.presentation.bottomsheet.moveto

import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.maillabel.presentation.iconRes
import ch.protonmail.android.maillabel.presentation.iconTintColor
import ch.protonmail.android.maillabel.presentation.text
import ch.protonmail.android.maillabel.presentation.toMoveToInboxCategories
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

internal class MoveToReducer @Inject constructor() {

    fun newStateFrom(contentState: MoveToState, event: MoveToOperation) = when (contentState) {
        is MoveToState.Data -> when (event) {
            is MoveToOperation.MoveToEvent.MoveComplete -> reduceMoveComplete(contentState, event)
            MoveToOperation.MoveToEvent.ErrorMoving -> reduceMoveError(contentState)
            else -> contentState
        }

        MoveToState.Error,
        MoveToState.Loading -> when (event) {
            is MoveToOperation.MoveToEvent.InitialData -> reduceInitialState(event)
            is MoveToOperation.MoveToEvent.LoadingError -> MoveToState.Error
            else -> contentState
        }
    }

    private fun reduceMoveComplete(
        state: MoveToState.Data,
        event: MoveToOperation.MoveToEvent.MoveComplete
    ): MoveToState {
        val dismissData = MoveToState.MoveToDismissData(event.mailLabelText)
        return state.copy(shouldDismissEffect = Effect.of(dismissData))
    }

    private fun reduceInitialState(event: MoveToOperation.MoveToEvent.InitialData): MoveToState {
        val system = event.moveToDestinations.filterIsInstance<MailLabel.System>()
        val customFolders = event.moveToDestinations.filterIsInstance<MailLabel.Custom>()
        val inbox = system.firstOrNull { it.systemLabelId == SystemLabelId.Inbox }
        val mailLabels = MailLabels(system = system, folders = customFolders, labels = emptyList())

        val systemDestinations = mailLabels.system.map {
            if (it.systemLabelId == SystemLabelId.Inbox) {
                null
            } else {
                MoveToBottomSheetDestinationUiModel.System(
                    it.id,
                    text = it.text(),
                    icon = it.iconRes(),
                    iconTint = it.iconTintColor()
                )
            }
        }.filterNotNull()

        val inboxDestination = inbox?.let {
            MoveToBottomSheetDestinationUiModel.Inbox(
                id = it.id,
                text = it.text(),
                icon = it.iconRes(),
                iconTint = it.iconTintColor(),
                categories = it.categories.toMoveToInboxCategories()
            )
        }


        val customDestinationsUi = mailLabels.folders.map {
            MoveToBottomSheetDestinationUiModel.Custom(
                it.id,
                text = it.text(),
                icon = it.iconRes(),
                iconTint = it.iconTintColor(),
                iconPaddingStart = ProtonDimens.Spacing.Large * it.level
            )
        }

        return MoveToState.Data(
            entryPoint = event.entryPoint,
            systemDestinations = systemDestinations.toImmutableList(),
            customDestinations = customDestinationsUi.toImmutableList(),
            inboxDestination = inboxDestination,
            shouldDismissEffect = Effect.empty(),
            errorEffect = Effect.empty()
        )
    }

    private fun reduceMoveError(contentState: MoveToState.Data) = contentState.copy(
        errorEffect = Effect.of(TextUiModel.TextRes(R.string.bottom_sheet_move_to_action_error))
    )
}
