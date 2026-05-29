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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import ch.protonmail.android.maillabel.domain.model.CategorySystemLabelId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import kotlinx.collections.immutable.ImmutableList

sealed interface MoveToState {

    data object Loading : MoveToState

    data class Data(
        val systemDestinations: ImmutableList<MoveToBottomSheetDestinationUiModel.System>,
        val customDestinations: ImmutableList<MoveToBottomSheetDestinationUiModel.Custom>,
        val inboxDestination: MoveToBottomSheetDestinationUiModel.Inbox? = null,
        val entryPoint: MoveToBottomSheetEntryPoint,
        val shouldDismissEffect: Effect<MoveToDismissData>,
        val errorEffect: Effect<TextUiModel>
    ) : MoveToState

    data object Error : MoveToState

    data class MoveToDismissData(val mailLabelText: MailLabelText)
}

sealed class MoveToBottomSheetDestinationUiModel(
    open val id: MailLabelId,
    open val text: TextUiModel,
    open val icon: Int,
    open val iconTint: Color?
) {

    data class System(
        override val id: MailLabelId.System,
        override val text: TextUiModel,
        override val icon: Int,
        override val iconTint: Color?
    ) : MoveToBottomSheetDestinationUiModel(id, text, icon, iconTint)

    data class Custom(
        override val id: MailLabelId.Custom,
        override val text: TextUiModel,
        override val icon: Int,
        override val iconTint: Color?,
        val iconPaddingStart: Dp
    ) : MoveToBottomSheetDestinationUiModel(id, text, icon, iconTint)

    data class Inbox(
        override val id: MailLabelId.System,
        override val text: TextUiModel,
        override val icon: Int,
        override val iconTint: Color?,
        val categories: List<Category>
    ) : MoveToBottomSheetDestinationUiModel(id, text, icon, iconTint) {

        data class Category(
            val id: CategorySystemLabelId,
            val text: TextUiModel,
            val icon: Int,
            val iconTint: Color?
        )
    }
}

sealed interface MoveToOperation {

    sealed interface MoveToEvent : MoveToOperation {

        data object LoadingError : MoveToEvent

        @Immutable
        data class InitialData(
            val moveToDestinations: List<MailLabel>,
            val entryPoint: MoveToBottomSheetEntryPoint
        ) : MoveToEvent

        data object ErrorMoving : MoveToEvent
        data class MoveComplete(val mailLabelText: MailLabelText) : MoveToEvent
    }

    sealed interface MoveToAction : MoveToOperation {
        data class MoveToDestinationSelected(
            val mailLabelId: MailLabelId,
            val mailLabelText: MailLabelText
        ) : MoveToAction
    }
}
