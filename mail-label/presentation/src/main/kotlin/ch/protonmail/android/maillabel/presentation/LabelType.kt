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

package ch.protonmail.android.maillabel.presentation

import androidx.annotation.StringRes
import me.proton.core.label.domain.entity.LabelType

@StringRes
fun LabelType.labelTitleRes() = when (this) {
    LabelType.ContactGroup -> 0
    LabelType.MessageLabel -> R.string.label_title_labels
    LabelType.MessageFolder -> R.string.label_title_folders
    LabelType.SystemFolder -> throw UnsupportedOperationException()
}

@StringRes
fun LabelType.labelAddTitleRes() = when (this) {
    LabelType.ContactGroup -> 0
    LabelType.MessageLabel -> R.string.label_title_create_label
    LabelType.MessageFolder -> R.string.label_title_create_folder
    LabelType.SystemFolder -> throw UnsupportedOperationException()
}
