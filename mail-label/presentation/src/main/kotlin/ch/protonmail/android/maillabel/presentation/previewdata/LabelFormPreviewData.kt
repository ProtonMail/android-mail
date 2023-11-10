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

package ch.protonmail.android.maillabel.presentation.previewdata

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import ch.protonmail.android.maillabel.presentation.labelform.LabelFormState
import ch.protonmail.android.maillabel.presentation.sample.LabelColorListSample.colorListSample
import me.proton.core.label.domain.entity.LabelId

object LabelFormPreviewData {

    val createLabelFormState = LabelFormState.Data.Create(
        isSaveEnabled = true,
        name = "Label name",
        color = colorListSample().random().getHexStringFromColor(),
        colorList = colorListSample(),
        close = Effect.empty(),
        closeWithSave = Effect.empty()
    )

    val editLabelFormState = LabelFormState.Data.Update(
        isSaveEnabled = true,
        name = "Label name",
        color = colorListSample().random().getHexStringFromColor(),
        colorList = colorListSample(),
        close = Effect.empty(),
        closeWithSave = Effect.empty(),
        labelId = LabelId("Label Id"),
        closeWithDelete = Effect.empty()
    )
}
