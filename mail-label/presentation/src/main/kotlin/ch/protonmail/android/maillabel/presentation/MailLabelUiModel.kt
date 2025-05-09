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

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.toMailLabelSystem
import me.proton.core.label.domain.entity.LabelId

@Immutable
sealed interface MailLabelUiModel {

    val id: MailLabelId
    val key: String
    val text: TextUiModel
    val icon: Int
    val iconTint: Color?
    val isSelected: Boolean
    val count: Int?

    @Immutable
    data class System(
        override val id: MailLabelId.System,
        override val key: String,
        override val text: TextUiModel.TextRes,
        override val icon: Int,
        override val iconTint: Color?,
        override val isSelected: Boolean,
        override val count: Int?
    ) : MailLabelUiModel

    @Immutable
    data class Custom(
        override val id: MailLabelId.Custom,
        override val key: String,
        override val text: TextUiModel.Text,
        override val icon: Int,
        override val iconTint: Color?,
        override val isSelected: Boolean,
        override val count: Int?,
        val isVisible: Boolean,
        val isExpanded: Boolean,
        val iconPaddingStart: Dp
    ) : MailLabelUiModel
}

val MailLabelUiModel.Custom.testTag: String
    get() = when (this.id) {
        is MailLabelId.Custom.Folder -> "CustomFolder"
        is MailLabelId.Custom.Label -> "CustomLabel"
    }

@Immutable
data class MailLabelsUiModel(
    val systems: List<MailLabelUiModel.System>,
    val folders: List<MailLabelUiModel.Custom>,
    val labels: List<MailLabelUiModel.Custom>
) {

    companion object {

        val Loading = MailLabelsUiModel(
            systems = emptyList(),
            folders = emptyList(),
            labels = emptyList()
        )

        @VisibleForTesting
        val PreviewForTesting by lazy {
            MailLabelsUiModel(
                systems = SystemLabelId.displayedList.map {
                    it.toMailLabelSystem().toSystemUiModel(
                        settings = ch.protonmail.android.mailsettings.domain.model.FolderColorSettings(),
                        counters = mapOf(LabelId("0") to 12),
                        selected = MailLabelId.System.Trash
                    )
                },
                folders = emptyList(),
                labels = emptyList()
            )
        }
    }
}
