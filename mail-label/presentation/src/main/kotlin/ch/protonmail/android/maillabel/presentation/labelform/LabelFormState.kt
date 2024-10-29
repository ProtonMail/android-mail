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

package ch.protonmail.android.maillabel.presentation.labelform

import androidx.compose.ui.graphics.Color
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.mailupselling.presentation.model.BottomSheetVisibilityEffect
import me.proton.core.label.domain.entity.LabelId

sealed interface LabelFormState {

    val close: Effect<Unit>
    val isSaveEnabled: Boolean

    data class Loading(
        override val close: Effect<Unit> = Effect.empty(),
        override val isSaveEnabled: Boolean = false
    ) : LabelFormState

    sealed interface Data : LabelFormState {

        val name: String
        val color: String
        val colorList: List<Color>
        val closeWithSave: Effect<Unit>
        val showLabelAlreadyExistsSnackbar: Effect<Unit>
        val showLabelLimitReachedSnackbar: Effect<Unit>
        val showSaveLabelErrorSnackbar: Effect<Unit>

        data class Create(
            override val isSaveEnabled: Boolean,
            override val name: String,
            override val color: String,
            override val colorList: List<Color>,
            override val close: Effect<Unit> = Effect.empty(),
            override val closeWithSave: Effect<Unit> = Effect.empty(),
            override val showLabelAlreadyExistsSnackbar: Effect<Unit> = Effect.empty(),
            override val showLabelLimitReachedSnackbar: Effect<Unit> = Effect.empty(),
            override val showSaveLabelErrorSnackbar: Effect<Unit> = Effect.empty(),
            val displayCreateLoader: Boolean = false,
            val upsellingVisibility: Effect<BottomSheetVisibilityEffect> = Effect.empty(),
            val upsellingInProgress: Effect<TextUiModel> = Effect.empty()
        ) : Data

        data class Update(
            override val isSaveEnabled: Boolean,
            override val name: String,
            override val color: String,
            override val colorList: List<Color>,
            override val close: Effect<Unit> = Effect.empty(),
            override val closeWithSave: Effect<Unit> = Effect.empty(),
            override val showLabelAlreadyExistsSnackbar: Effect<Unit> = Effect.empty(),
            override val showLabelLimitReachedSnackbar: Effect<Unit> = Effect.empty(),
            override val showSaveLabelErrorSnackbar: Effect<Unit> = Effect.empty(),
            val labelId: LabelId,
            val closeWithDelete: Effect<Unit> = Effect.empty(),
            val confirmDeleteDialogState: DeleteDialogState = DeleteDialogState.Hidden
        ) : Data
    }
}


