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

package ch.protonmail.android.maildetail.presentation

import ch.protonmail.android.maildetail.domain.R
import ch.protonmail.android.mailmessage.presentation.model.getDrawableForMimeType
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class AttachmentMimeTypeTest(
    private val testName: String,
    private val testInput: TestInput
) {

    @Test
    fun `should map mime type to expected drawable`() = with(testInput) {
        val actualDrawable = getDrawableForMimeType(mimeType)
        assertEquals(expectedDrawable, actualDrawable, testName)
    }

    companion object {

        private val fallbackDrawable = R.drawable.ic_proton_file_type_default_24
        private val expectedDocDrawable = R.drawable.ic_proton_file_type_word_24
        private val expectedImageDrawable = R.drawable.ic_proton_file_type_image_24
        private val expectedPdfDrawable = R.drawable.ic_proton_file_type_pdf_24
        private val expectedArchiveDrawable = R.drawable.ic_proton_file_type_zip_24
        private val expectedVideoDrawable = R.drawable.ic_proton_file_type_video_24
        private val expectedTextDrawable = R.drawable.ic_proton_file_type_text_24
        private val expectedAudioDrawable = R.drawable.ic_proton_file_type_audio_24
        private val expectedSpreadsheetDrawable = R.drawable.ic_proton_file_type_excel_24
        private val expectedPowerPointDrawable = R.drawable.ic_proton_file_type_powerpoint_24

        private val archive = listOf(
            TestInput(
                group = "Archive",
                mimeType = "application/gzip",
                expectedDrawable = expectedArchiveDrawable
            ),
            TestInput(
                group = "Archive",
                mimeType = "application/x-7z-compressed",
                expectedDrawable = expectedArchiveDrawable
            ),
            TestInput(
                group = "Archive",
                mimeType = "application/x-bzip",
                expectedDrawable = expectedArchiveDrawable
            ),
            TestInput(
                group = "Archive",
                mimeType = "application/x-bzip2",
                expectedDrawable = expectedArchiveDrawable
            ),
            TestInput(
                group = "Archive",
                mimeType = "application/vnd.rar",
                expectedDrawable = expectedArchiveDrawable
            ),
            TestInput(
                group = "Archive",
                mimeType = "application/zip",
                expectedDrawable = expectedArchiveDrawable
            )
        )

        private val audio = listOf(
            TestInput(
                group = "Audio",
                mimeType = "audio/x-m4a",
                expectedDrawable = expectedAudioDrawable
            ),
            TestInput(
                group = "Audio",
                mimeType = "audio/mpeg3",
                expectedDrawable = expectedAudioDrawable
            ),
            TestInput(
                group = "Audio",
                mimeType = "audio/x-mpeg-3",
                expectedDrawable = expectedAudioDrawable
            ),
            TestInput(
                group = "Audio",
                mimeType = "video/mpeg",
                expectedDrawable = expectedAudioDrawable
            ),
            TestInput(
                group = "Audio",
                mimeType = "video/x-mpeg",
                expectedDrawable = expectedAudioDrawable
            ),
            TestInput(
                group = "Audio",
                mimeType = "audio/aac",
                expectedDrawable = expectedAudioDrawable
            ),
            TestInput(
                group = "Audio",
                mimeType = "audio/x-hx-aac-adts",
                expectedDrawable = expectedAudioDrawable
            )
        )

        private val doc = listOf(
            TestInput(
                group = "Doc",
                mimeType = "application/doc",
                expectedDrawable = expectedDocDrawable
            ),
            TestInput(
                group = "Doc",
                mimeType = "application/ms-doc",
                expectedDrawable = expectedDocDrawable
            ),
            TestInput(
                group = "Doc",
                mimeType = "application/msword",
                expectedDrawable = expectedDocDrawable
            ),
            TestInput(
                group = "Doc",
                mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                expectedDrawable = expectedDocDrawable
            )
        )

        private val image = listOf(
            TestInput(
                group = "Image",
                mimeType = "image/bmp",
                expectedDrawable = expectedImageDrawable
            ),
            TestInput(
                group = "Image",
                mimeType = "image/gif",
                expectedDrawable = expectedImageDrawable
            ),
            TestInput(
                group = "Image",
                mimeType = "image/jpg",
                expectedDrawable = expectedImageDrawable
            ),
            TestInput(
                group = "Image",
                mimeType = "image/jpeg",
                expectedDrawable = expectedImageDrawable
            ),
            TestInput(
                group = "Image",
                mimeType = "image/heic",
                expectedDrawable = expectedImageDrawable
            ),
            TestInput(
                group = "Image",
                mimeType = "image/png",
                expectedDrawable = expectedImageDrawable
            ),
            TestInput(
                group = "Image",
                mimeType = "image/svg+xml",
                expectedDrawable = expectedImageDrawable
            ),
            TestInput(
                group = "Image",
                mimeType = "image/tiff",
                expectedDrawable = expectedImageDrawable
            ),
            TestInput(
                group = "Image",
                mimeType = "image/x-icon",
                expectedDrawable = expectedImageDrawable
            ),
            TestInput(
                group = "Image",
                mimeType = "image/webp",
                expectedDrawable = expectedImageDrawable
            )
        )

        private val pdf = listOf(
            TestInput(
                group = "Pdf",
                mimeType = "application/pdf",
                expectedDrawable = expectedPdfDrawable
            )
        )

        private val ppt = listOf(
            TestInput(
                group = "Ppt",
                mimeType = "application/mspowerpoint",
                expectedDrawable = expectedPowerPointDrawable
            ),
            TestInput(
                group = "Ppt",
                mimeType = "application/powerpoint",
                expectedDrawable = expectedPowerPointDrawable
            ),
            TestInput(
                group = "Ppt",
                mimeType = "application/vnd.ms-powerpoint",
                expectedDrawable = expectedPowerPointDrawable
            ),
            TestInput(
                group = "Ppt",
                mimeType = "application/x-mspowerpoint",
                expectedDrawable = expectedPowerPointDrawable
            ),
            TestInput(
                group = "Ppt",
                mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                expectedDrawable = expectedPowerPointDrawable
            )
        )

        private val txt = listOf(
            TestInput(
                group = "Txt",
                mimeType = "application/rtf",
                expectedDrawable = expectedTextDrawable
            ),
            TestInput(
                group = "Txt",
                mimeType = "text/plain",
                expectedDrawable = expectedTextDrawable
            )
        )

        private val video = listOf(
            TestInput(
                group = "Video",
                mimeType = "video/quicktime",
                expectedDrawable = expectedVideoDrawable
            ),
            TestInput(
                group = "Video",
                mimeType = "video/x-quicktime",
                expectedDrawable = expectedVideoDrawable
            ),
            TestInput(
                group = "Video",
                mimeType = "image/mov",
                expectedDrawable = expectedVideoDrawable
            ),
            TestInput(
                group = "Video",
                mimeType = "audio/aiff",
                expectedDrawable = expectedVideoDrawable
            ),
            TestInput(
                group = "Video",
                mimeType = "audio/x-midi",
                expectedDrawable = expectedVideoDrawable
            ),
            TestInput(
                group = "Video",
                mimeType = "audio/x-wav",
                expectedDrawable = expectedVideoDrawable
            ),
            TestInput(
                group = "Video",
                mimeType = "video/avi",
                expectedDrawable = expectedVideoDrawable
            ),
            TestInput(
                group = "Video",
                mimeType = "video/mp4",
                expectedDrawable = expectedVideoDrawable
            ),
            TestInput(
                group = "Video",
                mimeType = "video/webm",
                expectedDrawable = expectedVideoDrawable
            ),
            TestInput(
                group = "Video",
                mimeType = "video/x-matroska",
                expectedDrawable = expectedVideoDrawable
            )
        )

        private val xls = listOf(
            TestInput(
                group = "Xls",
                mimeType = "application/excel",
                expectedDrawable = expectedSpreadsheetDrawable
            ),
            TestInput(
                group = "Xls",
                mimeType = "application/vnd.ms-excel",
                expectedDrawable = expectedSpreadsheetDrawable
            ),
            TestInput(
                group = "Xls",
                mimeType = "application/x-excel",
                expectedDrawable = expectedSpreadsheetDrawable
            ),
            TestInput(
                group = "Xls",
                mimeType = "application/x-msexcel",
                expectedDrawable = expectedSpreadsheetDrawable
            ),
            TestInput(
                group = "Xls",
                mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                expectedDrawable = expectedSpreadsheetDrawable
            )
        )

        private val unknown = listOf(
            TestInput(
                group = "Unknown",
                mimeType = "application/x-envoy",
                expectedDrawable = fallbackDrawable
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = (
            archive +
                audio +
                doc +
                image +
                pdf +
                ppt +
                txt +
                video +
                xls +
                unknown
            ).map {
            val testName = """
                Group: ${it.group}
                MimeType: ${it.mimeType}
            """.trimIndent()
            arrayOf(testName, it)
        }
    }

    data class TestInput(
        val group: String,
        val mimeType: String,
        val expectedDrawable: Int
    )
}
