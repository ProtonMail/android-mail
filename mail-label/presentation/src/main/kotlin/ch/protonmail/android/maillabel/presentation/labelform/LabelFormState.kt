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

import ch.protonmail.android.mailcommon.presentation.Effect
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.NewLabel

data class LabelFormState(
    val isLoading: Boolean,
    val isSaveEnabled: Boolean,
    val label: Label?,
    val newLabel: NewLabel?,
    val close: Effect<Unit>,
    val closeWithSave: Effect<Unit>,
    val closeWithDelete: Effect<Unit>,
) {

    companion object {

        fun initial(): LabelFormState = LabelFormState(
            isLoading = true,
            isSaveEnabled = false,
            label = null,
            newLabel = null,
            close = Effect.empty(),
            closeWithSave = Effect.empty(),
            closeWithDelete = Effect.empty()
        )
    }
}
