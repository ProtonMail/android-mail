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

package ch.protonmail.android.mailmessage.data.sample

import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentDisposition
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentId
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentMetadata
import ch.protonmail.android.mailcommon.data.mapper.LocalMimeTypeCategory
import uniffi.mail_uniffi.AttachmentMimeType

object LocalAttachmentMetadataSample {

    val Calendar = LocalAttachmentMetadata(
        id = LocalAttachmentId(1uL),
        name = "calendar.ics",
        mimeType = AttachmentMimeType("text/calendar", LocalMimeTypeCategory.CALENDAR),
        size = 1024uL,
        disposition = LocalAttachmentDisposition.ATTACHMENT,
        isListable = false
    )

    val Audio = LocalAttachmentMetadata(
        id = LocalAttachmentId(2uL),
        name = "audio.mp3",
        mimeType = AttachmentMimeType("audio/mpeg", LocalMimeTypeCategory.AUDIO),
        size = 2048uL,
        disposition = LocalAttachmentDisposition.ATTACHMENT,
        isListable = true
    )

    val Code = LocalAttachmentMetadata(
        id = LocalAttachmentId(3uL),
        name = "script.js",
        mimeType = AttachmentMimeType("application/javascript", LocalMimeTypeCategory.CODE),
        size = 512uL,
        disposition = LocalAttachmentDisposition.INLINE,
        isListable = false
    )

    val Compressed = LocalAttachmentMetadata(
        id = LocalAttachmentId(4uL),
        name = "archive.zip",
        mimeType = AttachmentMimeType("application/zip", LocalMimeTypeCategory.COMPRESSED),
        size = 4096uL,
        disposition = LocalAttachmentDisposition.ATTACHMENT,
        isListable = true
    )

    val Image = LocalAttachmentMetadata(
        id = LocalAttachmentId(5uL),
        name = "image.png",
        mimeType = AttachmentMimeType("image/png", LocalMimeTypeCategory.IMAGE),
        size = 3072uL,
        disposition = LocalAttachmentDisposition.ATTACHMENT,
        isListable = true
    )

    val Pdf = LocalAttachmentMetadata(
        id = LocalAttachmentId(6uL),
        name = "document.pdf",
        mimeType = AttachmentMimeType("application/pdf", LocalMimeTypeCategory.PDF),
        size = 8192uL,
        disposition = LocalAttachmentDisposition.ATTACHMENT,
        isListable = true
    )

    val Text = LocalAttachmentMetadata(
        id = LocalAttachmentId(7uL),
        name = "notes.txt",
        mimeType = AttachmentMimeType("text/plain", LocalMimeTypeCategory.TEXT),
        size = 256uL,
        disposition = LocalAttachmentDisposition.ATTACHMENT,
        isListable = true
    )

    val Video = LocalAttachmentMetadata(
        id = LocalAttachmentId(8uL),
        name = "video.mp4",
        mimeType = AttachmentMimeType("video/mp4", LocalMimeTypeCategory.VIDEO),
        size = 16384uL,
        disposition = LocalAttachmentDisposition.ATTACHMENT,
        isListable = true
    )

    val Word = LocalAttachmentMetadata(
        id = LocalAttachmentId(9uL),
        name = "document.docx",
        mimeType = AttachmentMimeType("application/application/vnd.openxmlformats", LocalMimeTypeCategory.WORD),
        size = 4096uL,
        disposition = LocalAttachmentDisposition.ATTACHMENT,
        isListable = true
    )

    val InlineImage = LocalAttachmentMetadata(
        id = LocalAttachmentId(5uL),
        name = "image.png",
        mimeType = AttachmentMimeType("image/png", LocalMimeTypeCategory.IMAGE),
        size = 3072uL,
        disposition = LocalAttachmentDisposition.INLINE,
        isListable = false
    )
}
