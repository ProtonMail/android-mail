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

package ch.protonmail.android.mailmessage.presentation.mapper

import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.presentation.model.AttachmentUiModel

object AttachmentUiModelMapper2 {

    fun toUiModel(attachment: MessageAttachment, deletable: Boolean = false) = AttachmentUiModel(
        attachmentId = attachment.attachmentId.id,
        fileName = attachment.name.split(".").dropLast(1).joinToString("."),
        extension = attachment.name.split(".").last(),
        size = attachment.size,
        mimeType = attachment.mimeType,
        deletable = deletable
    )
}
