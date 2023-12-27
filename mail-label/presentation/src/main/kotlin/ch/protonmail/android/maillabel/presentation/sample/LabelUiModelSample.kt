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

package ch.protonmail.android.maillabel.presentation.sample

import androidx.compose.ui.graphics.Color
import ch.protonmail.android.mailcommon.domain.sample.LabelSample
import ch.protonmail.android.maillabel.presentation.model.LabelUiModel
import me.proton.core.label.domain.entity.Label

object LabelUiModelSample {

    val Document = build(LabelSample.Document)
    val News = build(LabelSample.News)
    val Starred = build(LabelSample.Starred)

    fun build(label: Label) = LabelUiModel(
        name = label.name,
        color = Color.Red,
        id = label.labelId.id
    )
}
