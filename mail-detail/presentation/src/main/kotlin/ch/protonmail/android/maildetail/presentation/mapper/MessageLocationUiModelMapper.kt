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

import ch.protonmail.android.maildetail.presentation.model.MessageLocationUiModel
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.presentation.iconRes
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Inject

class MessageLocationUiModelMapper @Inject constructor() {

    operator fun invoke(labelIds: List<LabelId>): MessageLocationUiModel {
        exclusiveLocations().forEach { systemLabelId ->
            if (systemLabelId.labelId in labelIds) {
                return MessageLocationUiModel(
                    systemLabelId.name,
                    SystemLabelId.enumOf(systemLabelId.labelId.id).iconRes()
                )
            }
        }

        // Add logic for when the location is a custom folder here
        // after creating a domain model that contains the list of labels

        // If no location has been found, then the message is orphaned and is only in All Mail
        return MessageLocationUiModel(
            SystemLabelId.AllMail.name,
            SystemLabelId.enumOf(SystemLabelId.AllMail.labelId.id).iconRes()
        )
    }

    private fun exclusiveLocations() = listOf(
        SystemLabelId.Inbox,
        SystemLabelId.Trash,
        SystemLabelId.Spam,
        SystemLabelId.Archive,
        SystemLabelId.Sent,
        SystemLabelId.Drafts
    )
}
