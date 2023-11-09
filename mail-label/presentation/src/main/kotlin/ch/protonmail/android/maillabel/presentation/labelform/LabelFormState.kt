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
import me.proton.core.label.domain.entity.LabelId

sealed class LabelFormState {

    object Loading : LabelFormState()

    data class Create(
        val isSaveEnabled: Boolean,
        val name: String,
        val color: String,
        val colorList: List<Color>,
        val close: Effect<Unit>,
        val closeWithSave: Effect<Unit>
    ) : LabelFormState()

    data class Update(
        val isSaveEnabled: Boolean,
        val labelId: LabelId,
        val name: String,
        val color: String,
        val colorList: List<Color>,
        val close: Effect<Unit>,
        val closeWithSave: Effect<Unit>,
        val closeWithDelete: Effect<Unit>
    ) : LabelFormState()
}


