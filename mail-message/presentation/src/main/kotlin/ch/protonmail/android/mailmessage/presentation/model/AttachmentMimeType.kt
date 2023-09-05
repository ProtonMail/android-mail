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

package ch.protonmail.android.mailmessage.presentation.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ch.protonmail.android.mailmessage.presentation.R
import me.proton.core.presentation.R.drawable

sealed class AttachmentMimeType(@StringRes val contentDescription: Int, val mimeTypes: List<String>) {
    object Archive : AttachmentMimeType(
        contentDescription = R.string.attachment_type_archive,
        mimeTypes = listOf(
            "application/gzip",
            "application/x-7z-compressed",
            "application/x-bzip",
            "application/x-bzip2",
            "application/vnd.rar",
            "application/zip"
        )
    )

    object Audio : AttachmentMimeType(
        contentDescription = R.string.attachment_type_audio,
        mimeTypes = listOf(
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
        contentDescription = R.string.attachment_type_document,
        mimeTypes = listOf(
            "application/doc",
            "application/ms-doc",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
    )

    object Image : AttachmentMimeType(
        contentDescription = R.string.attachment_type_image,
        mimeTypes = listOf(
            "image/bmp",
            "image/gif",
            "image/jpg",
            "image/jpeg",
            "image/heic",
            "image/png",
            "image/svg+xml",
            "image/tiff",
            "image/x-icon",
            "image/webp"
        )
    )

    object Pdf : AttachmentMimeType(
        contentDescription = R.string.attachment_type_pdf,
        mimeTypes = listOf(
            "application/pdf"
        )
    )

    object Ppt : AttachmentMimeType(
        contentDescription = R.string.attachment_type_presentation,
        mimeTypes = listOf(
            "application/mspowerpoint",
            "application/powerpoint",
            "application/vnd.ms-powerpoint",
            "application/x-mspowerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        )
    )

    object Txt : AttachmentMimeType(
        contentDescription = R.string.attachment_type_text,
        mimeTypes = listOf(
            "application/rtf",
            "text/plain"
        )
    )

    object Video : AttachmentMimeType(
        contentDescription = R.string.attachment_type_video,
        mimeTypes = listOf(
            "video/quicktime",
            "video/x-quicktime",
            "image/mov",
            "audio/aiff",
            "audio/x-midi",
            "audio/x-wav",
            "video/avi",
            "video/mp4",
            "video/webm",
            "video/x-matroska"
        )
    )

    object Xls : AttachmentMimeType(
        contentDescription = R.string.attachment_type_spreadsheet,
        mimeTypes = listOf(
            "application/excel",
            "application/vnd.ms-excel",
            "application/x-excel",
            "application/x-msexcel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    )
}

@DrawableRes
fun getDrawableForMimeType(mimeType: String) = when {
    AttachmentMimeType.Doc.mimeTypes.contains(mimeType) -> drawable.ic_proton_file_type_word_24
    AttachmentMimeType.Pdf.mimeTypes.contains(mimeType) -> drawable.ic_proton_file_type_pdf_24
    AttachmentMimeType.Archive.mimeTypes.contains(mimeType) -> drawable.ic_proton_file_type_zip_24
    AttachmentMimeType.Image.mimeTypes.contains(mimeType) -> drawable.ic_proton_file_type_image_24
    AttachmentMimeType.Txt.mimeTypes.contains(mimeType) -> drawable.ic_proton_file_type_text_24
    AttachmentMimeType.Audio.mimeTypes.contains(mimeType) -> drawable.ic_proton_file_type_audio_24
    AttachmentMimeType.Ppt.mimeTypes.contains(mimeType) -> drawable.ic_proton_file_type_powerpoint_24
    AttachmentMimeType.Video.mimeTypes.contains(mimeType) -> drawable.ic_proton_file_type_video_24
    AttachmentMimeType.Xls.mimeTypes.contains(mimeType) -> drawable.ic_proton_file_type_excel_24
    else -> drawable.ic_proton_file_type_default_24
}

@StringRes
fun getContentDescriptionForMimeType(mimeType: String) = when {
    AttachmentMimeType.Doc.mimeTypes.contains(mimeType) -> AttachmentMimeType.Doc.contentDescription
    AttachmentMimeType.Pdf.mimeTypes.contains(mimeType) -> AttachmentMimeType.Pdf.contentDescription
    AttachmentMimeType.Archive.mimeTypes.contains(mimeType) -> AttachmentMimeType.Archive.contentDescription
    AttachmentMimeType.Image.mimeTypes.contains(mimeType) -> AttachmentMimeType.Image.contentDescription
    AttachmentMimeType.Txt.mimeTypes.contains(mimeType) -> AttachmentMimeType.Txt.contentDescription
    AttachmentMimeType.Audio.mimeTypes.contains(mimeType) -> AttachmentMimeType.Audio.contentDescription
    AttachmentMimeType.Ppt.mimeTypes.contains(mimeType) -> AttachmentMimeType.Ppt.contentDescription
    AttachmentMimeType.Video.mimeTypes.contains(mimeType) -> AttachmentMimeType.Video.contentDescription
    AttachmentMimeType.Xls.mimeTypes.contains(mimeType) -> AttachmentMimeType.Xls.contentDescription
    else -> R.string.attachment_type_unknown
}
