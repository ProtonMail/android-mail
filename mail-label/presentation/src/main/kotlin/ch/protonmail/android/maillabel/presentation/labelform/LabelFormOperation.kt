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

sealed interface LabelFormOperation

internal sealed interface LabelFormViewAction : LabelFormOperation {
    data class LabelNameChanged(val labelName: String) : LabelFormViewAction
    data class LabelColorChanged(val labelColor: Color) : LabelFormViewAction
    object OnSaveClick : LabelFormViewAction
    object OnDeleteClick : LabelFormViewAction
    object OnCloseLabelForm : LabelFormViewAction
}

sealed interface LabelFormEvent : LabelFormOperation {
    object LabelCreated : LabelFormEvent
    object LabelUpdated : LabelFormEvent
    object LabelDeleted : LabelFormEvent
}
