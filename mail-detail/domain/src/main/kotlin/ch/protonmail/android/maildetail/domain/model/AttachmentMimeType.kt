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

package ch.protonmail.android.maildetail.domain.model

import androidx.annotation.DrawableRes
import me.proton.core.presentation.R.drawable

sealed class AttachmentMimeType(val mimeTypes: List<String>) {
    object Audio : AttachmentMimeType(
        listOf(
            "audio/x-m4a",
            "audio/mpeg3",
            "audio/x-mpeg-3",
            "video/mpeg",
            "video/x-mpeg",
            "audio/aac",
            "audio/x-hx-aac-adts"
        )
    )

    object Doc : AttachmentMimeType(
        listOf(
            "application/doc",
            "application/ms-doc",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
    )

    object Image : AttachmentMimeType(
        listOf(
            "image/jpg",
            "image/jpeg",
            "image/png"
        )
    )

    object Pdf : AttachmentMimeType(
        listOf(
            "application/pdf"
        )
    )

    object Ppt : AttachmentMimeType(
        listOf(
            "application/mspowerpoint",
            "application/powerpoint",
            "application/vnd.ms-powerpoint",
            "application/x-mspowerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        )
    )

    object Txt : AttachmentMimeType(
        listOf(
            "text/plain"
        )
    )

    object Video : AttachmentMimeType(
        listOf(
            "video/quicktime",
            "video/x-quicktime",
            "image/mov",
            "audio/aiff",
            "audio/x-midi",
            "audio/x-wav",
            "video/avi",
            "video/mp4",
            "video/x-matroska"
        )
    )

    object Xls : AttachmentMimeType(
        listOf(
            "application/excel",
            "application/vnd.ms-excel",
            "application/x-excel",
            "application/x-msexcel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    )

    object Zip : AttachmentMimeType(
        listOf(
            "application/zip"
        )
    )

}

@DrawableRes
fun getDrawableForMimeType(mimeType: String) = when {
    AttachmentMimeType.Doc.mimeTypes.contains(mimeType) -> drawable.ic_proton_file_word_24
    AttachmentMimeType.Pdf.mimeTypes.contains(mimeType) -> drawable.ic_proton_file_pdf_24
    AttachmentMimeType.Zip.mimeTypes.contains(mimeType) -> drawable.ic_proton_file_rar_zip_24
    AttachmentMimeType.Image.mimeTypes.contains(mimeType) -> drawable.ic_proton_file_image_24
    AttachmentMimeType.Txt.mimeTypes.contains(mimeType) -> drawable.ic_proton_file_attachment_24
    AttachmentMimeType.Audio.mimeTypes.contains(mimeType) ||
        AttachmentMimeType.Ppt.mimeTypes.contains(mimeType) ||
        AttachmentMimeType.Video.mimeTypes.contains(mimeType) ||
        AttachmentMimeType.Xls.mimeTypes.contains(mimeType) -> drawable.ic_proton_file_attachment_24
    else -> drawable.ic_proton_file_attachment_24
}
