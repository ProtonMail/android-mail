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

package ch.protonmail.android.testdata.label.rust

import uniffi.mail_uniffi.Id
import uniffi.mail_uniffi.IsSelected
import uniffi.mail_uniffi.LabelAsAction
import uniffi.mail_uniffi.LabelColor

object LocalLabelAsActionTestData {

    val unselectedLabelAsActionId = Id(1uL)
    val selectedLabelAsActionId = Id(2uL)
    val partiallySelectedLabelAsActionId = Id(3uL)

    val unselectedAction = LabelAsAction(
        unselectedLabelAsActionId,
        "unselected",
        LabelColor("#fff"),
        0.toUInt(),
        IsSelected.UNSELECTED
    )
    val selectedAction = LabelAsAction(
        selectedLabelAsActionId,
        "selected",
        LabelColor("#aaa"),
        0.toUInt(),
        IsSelected.SELECTED
    )
    val partiallySelectedAction = LabelAsAction(
        partiallySelectedLabelAsActionId,
        "partial",
        LabelColor("#000"),
        0.toUInt(),
        IsSelected.PARTIAL
    )
}
