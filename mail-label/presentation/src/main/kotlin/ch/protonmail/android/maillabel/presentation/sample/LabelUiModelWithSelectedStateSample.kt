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
import androidx.compose.ui.unit.dp
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.presentation.MailLabelUiModel
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.maillabel.presentation.model.LabelSelectedState
import ch.protonmail.android.maillabel.presentation.model.LabelUiModelWithSelectedState
import kotlinx.collections.immutable.toImmutableList
import me.proton.core.label.domain.entity.LabelId

object LabelUiModelWithSelectedStateSample {

    val customLabelListWithoutSelection = listOf(
        LabelUiModelWithSelectedState(
            labelUiModel = MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Label(LabelId("label1")),
                key = "label1",
                text = TextUiModel.Text("Label1"),
                icon = R.drawable.ic_proton_circle_filled,
                iconTint = Color.Blue,
                isSelected = false,
                count = 1,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.dp
            ),
            selectedState = LabelSelectedState.NotSelected
        ),
        LabelUiModelWithSelectedState(
            labelUiModel = MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelId("label2")),
                key = "label2",
                text = TextUiModel.Text("This is a label with a really unnecessary long name"),
                icon = R.drawable.ic_proton_circle_filled,
                iconTint = Color.Red,
                isSelected = false,
                count = 2,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.dp
            ),
            selectedState = LabelSelectedState.NotSelected
        ),
        LabelUiModelWithSelectedState(
            labelUiModel = MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelId("label3")),
                key = "label3",
                text = TextUiModel.Text("Label3"),
                icon = R.drawable.ic_proton_circle_filled,
                iconTint = Color.Green,
                isSelected = false,
                count = 3,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.dp
            ),
            selectedState = LabelSelectedState.NotSelected
        )
    ).toImmutableList()

    val customLabelListWithSelection = listOf(
        LabelUiModelWithSelectedState(
            labelUiModel = MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Label(LabelId("label1")),
                key = "label1",
                text = TextUiModel.Text("Label1"),
                icon = R.drawable.ic_proton_circle_filled,
                iconTint = Color.Blue,
                isSelected = false,
                count = 1,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.dp
            ),
            selectedState = LabelSelectedState.Selected
        ),
        LabelUiModelWithSelectedState(
            labelUiModel = MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelId("label2")),
                key = "label2",
                text = TextUiModel.Text("This is a label with a really unnecessary long name"),
                icon = R.drawable.ic_proton_circle_filled,
                iconTint = Color.Red,
                isSelected = false,
                count = 2,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.dp
            ),
            selectedState = LabelSelectedState.NotSelected
        ),
        LabelUiModelWithSelectedState(
            labelUiModel = MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelId("label3")),
                key = "label3",
                text = TextUiModel.Text("Label3"),
                icon = R.drawable.ic_proton_circle_filled,
                iconTint = Color.Green,
                isSelected = false,
                count = 3,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.dp
            ),
            selectedState = LabelSelectedState.NotSelected
        )
    ).toImmutableList()

    val customLabelListWithPartialSelection = listOf(
        LabelUiModelWithSelectedState(
            labelUiModel = MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Label(LabelId("label1")),
                key = "label1",
                text = TextUiModel.Text("Label1"),
                icon = R.drawable.ic_proton_circle_filled,
                iconTint = Color.Blue,
                isSelected = false,
                count = 1,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.dp
            ),
            selectedState = LabelSelectedState.PartiallySelected
        ),
        LabelUiModelWithSelectedState(
            labelUiModel = MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelId("label2")),
                key = "label2",
                text = TextUiModel.Text("This is a label with a really unnecessary long name"),
                icon = R.drawable.ic_proton_circle_filled,
                iconTint = Color.Red,
                isSelected = false,
                count = 2,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.dp
            ),
            selectedState = LabelSelectedState.NotSelected
        ),
        LabelUiModelWithSelectedState(
            labelUiModel = MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelId("label3")),
                key = "label3",
                text = TextUiModel.Text("Label3"),
                icon = R.drawable.ic_proton_circle_filled,
                iconTint = Color.Green,
                isSelected = false,
                count = 3,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.dp
            ),
            selectedState = LabelSelectedState.NotSelected
        )
    ).toImmutableList()

    val customLabelListWithDocumentSelected = listOf(
        LabelUiModelWithSelectedState(
            labelUiModel = MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Label(LabelIdSample.Document),
                key = LabelIdSample.Document.id,
                text = TextUiModel.Text("Label1"),
                icon = R.drawable.ic_proton_circle_filled,
                iconTint = Color.Blue,
                isSelected = false,
                count = 1,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.dp
            ),
            selectedState = LabelSelectedState.Selected
        ),
        LabelUiModelWithSelectedState(
            labelUiModel = MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelIdSample.Label2021),
                key = LabelIdSample.Label2021.id,
                text = TextUiModel.Text("This is a label with a really unnecessary long name"),
                icon = R.drawable.ic_proton_circle_filled,
                iconTint = Color.Red,
                isSelected = false,
                count = 2,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.dp
            ),
            selectedState = LabelSelectedState.NotSelected
        ),
        LabelUiModelWithSelectedState(
            labelUiModel = MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelIdSample.Label2022),
                key = LabelIdSample.Label2022.id,
                text = TextUiModel.Text("Label3"),
                icon = R.drawable.ic_proton_circle_filled,
                iconTint = Color.Green,
                isSelected = false,
                count = 3,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.dp
            ),
            selectedState = LabelSelectedState.NotSelected
        )
    ).toImmutableList()

    val customLabelListWithFirstTwoSelected = listOf(
        LabelUiModelWithSelectedState(
            labelUiModel = MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Label(LabelIdSample.Document),
                key = LabelIdSample.Document.id,
                text = TextUiModel.Text("document"),
                icon = R.drawable.ic_proton_circle_filled,
                iconTint = Color.Transparent,
                isSelected = false,
                count = null,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.dp
            ),
            selectedState = LabelSelectedState.Selected
        ),
        LabelUiModelWithSelectedState(
            labelUiModel = MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Label(LabelIdSample.Label2021),
                key = LabelIdSample.Label2021.id,
                text = TextUiModel.Text("Label2021"),
                icon = R.drawable.ic_proton_circle_filled,
                iconTint = Color.Transparent,
                isSelected = false,
                count = null,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.dp
            ),
            selectedState = LabelSelectedState.Selected
        ),
        LabelUiModelWithSelectedState(
            labelUiModel = MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Label(LabelIdSample.Label2022),
                key = LabelIdSample.Label2022.id,
                text = TextUiModel.Text("Label2022"),
                icon = R.drawable.ic_proton_circle_filled,
                iconTint = Color.Transparent,
                isSelected = false,
                count = null,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.dp
            ),
            selectedState = LabelSelectedState.NotSelected
        )
    ).toImmutableList()

    val customLabelListWithVariousStates = listOf(
        LabelUiModelWithSelectedState(
            labelUiModel = MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Label(LabelIdSample.Document),
                key = LabelIdSample.Document.id,
                text = TextUiModel.Text("Label1"),
                icon = R.drawable.ic_proton_circle_filled,
                iconTint = Color.Blue,
                isSelected = false,
                count = 1,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.dp
            ),
            selectedState = LabelSelectedState.Selected
        ),
        LabelUiModelWithSelectedState(
            labelUiModel = MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelIdSample.Label2021),
                key = LabelIdSample.Label2021.id,
                text = TextUiModel.Text("This is a label with a really unnecessary long name"),
                icon = R.drawable.ic_proton_circle_filled,
                iconTint = Color.Red,
                isSelected = false,
                count = 2,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.dp
            ),
            selectedState = LabelSelectedState.Selected
        ),
        LabelUiModelWithSelectedState(
            labelUiModel = MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelIdSample.Label2022),
                key = LabelIdSample.Label2022.id,
                text = TextUiModel.Text("Label3"),
                icon = R.drawable.ic_proton_circle_filled,
                iconTint = Color.Green,
                isSelected = false,
                count = 3,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.dp
            ),
            selectedState = LabelSelectedState.PartiallySelected
        )
    ).toImmutableList()

    val customLabelListAllSelected = listOf(
        LabelUiModelWithSelectedState(
            labelUiModel = MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Label(LabelIdSample.Document),
                key = LabelIdSample.Document.id,
                text = TextUiModel.Text("Label1"),
                icon = R.drawable.ic_proton_circle_filled,
                iconTint = Color.Blue,
                isSelected = false,
                count = 1,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.dp
            ),
            selectedState = LabelSelectedState.Selected
        ),
        LabelUiModelWithSelectedState(
            labelUiModel = MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelIdSample.Label2021),
                key = LabelIdSample.Label2021.id,
                text = TextUiModel.Text("This is a label with a really unnecessary long name"),
                icon = R.drawable.ic_proton_circle_filled,
                iconTint = Color.Red,
                isSelected = false,
                count = 2,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.dp
            ),
            selectedState = LabelSelectedState.Selected
        ),
        LabelUiModelWithSelectedState(
            labelUiModel = MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelIdSample.Label2022),
                key = LabelIdSample.Label2022.id,
                text = TextUiModel.Text("Label3"),
                icon = R.drawable.ic_proton_circle_filled,
                iconTint = Color.Green,
                isSelected = false,
                count = 3,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.dp
            ),
            selectedState = LabelSelectedState.Selected
        )
    ).toImmutableList()
}
