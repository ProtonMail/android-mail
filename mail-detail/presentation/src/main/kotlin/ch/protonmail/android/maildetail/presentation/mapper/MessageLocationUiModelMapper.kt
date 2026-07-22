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

package ch.protonmail.android.maildetail.presentation.mapper

import ch.protonmail.android.mailcategory.presentation.design.activeCategoryColor
import ch.protonmail.android.mailcategory.presentation.mapper.categoryIconRes
import ch.protonmail.android.mailcategory.presentation.mapper.categoryTextRes
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageLocationUiModel
import ch.protonmail.android.maillabel.domain.model.CategoryLocation
import ch.protonmail.android.maillabel.domain.model.ExclusiveLocation
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.presentation.iconRes
import ch.protonmail.android.maillabel.presentation.textRes
import javax.inject.Inject

class MessageLocationUiModelMapper @Inject constructor(
    private val colorMapper: ColorMapper
) {

    operator fun invoke(messageLocation: ExclusiveLocation): MessageLocationUiModel {

        return when (messageLocation) {
            is ExclusiveLocation.System -> {
                MessageLocationUiModel(
                    name = TextUiModel(messageLocation.systemLabelId.textRes()),
                    icon = messageLocation.systemLabelId.iconRes()
                )
            }

            is ExclusiveLocation.Folder -> {
                MessageLocationUiModel(
                    name = TextUiModel.Text(messageLocation.name),
                    icon = when {
                        // This is temporary, see ET-2268 to handle folder colors properly.
                        true -> R.drawable.ic_proton_folder_filled
                        else -> R.drawable.ic_proton_folder
                    },
                    color = colorMapper.toColor(messageLocation.color).getOrNull()
                )
            }

            is ExclusiveLocation.NoLocation -> allMailLocation()
        }
    }

    private fun allMailLocation() = MessageLocationUiModel(
        name = TextUiModel(SystemLabelId.AllMail.textRes()),
        icon = SystemLabelId.enumOf(SystemLabelId.AllMail.labelId.id).iconRes()
    )

    /**
     * Resolves the optional category row shown underneath the location. Rust only provides a
     * category for categorised inbox messages, so non-inbox and non-category-view messages return
     * `null` and no category row is displayed.
     */
    operator fun invoke(category: CategoryLocation): MessageLocationUiModel? = when (category) {
        is CategoryLocation.Value -> MessageLocationUiModel(
            name = TextUiModel(category.location.categoryTextRes()),
            icon = category.location.categoryIconRes(),
            color = category.location.activeCategoryColor()
        )

        CategoryLocation.NoValue -> null
    }
}
