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

package ch.protonmail.android.maildetail.domain

import ch.protonmail.android.maildetail.domain.model.getDrawableForMimeType
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

        private val fallbackDrawable = R.drawable.ic_proton_file_attachment_24
        private val expectedDocDrawable = R.drawable.ic_proton_file_word_24
        private val expectedImageDrawable = R.drawable.ic_proton_file_image_24
        private val expectedPdfDrawable = R.drawable.ic_proton_file_pdf_24
        private val expectedZipDrawable = R.drawable.ic_proton_file_rar_zip_24

        private val audio = listOf(
            TestInput(
                group = "Audio",
                mimeType = "audio/x-m4a",
                expectedDrawable = fallbackDrawable
            ),
            TestInput(
                group = "Audio",
                mimeType = "audio/mpeg3",
                expectedDrawable = fallbackDrawable
            ),
            TestInput(
                group = "Audio",
                mimeType = "audio/x-mpeg-3",
                expectedDrawable = fallbackDrawable
            ),
            TestInput(
                group = "Audio",
                mimeType = "audio/mpeg",
                expectedDrawable = fallbackDrawable
            ),
            TestInput(
                group = "Audio",
                mimeType = "audio/x-mpeg",
                expectedDrawable = fallbackDrawable
            ),
            TestInput(
                group = "Audio",
                mimeType = "audio/aac",
                expectedDrawable = fallbackDrawable
            ),
            TestInput(
                group = "Audio",
                mimeType = "audio/x-hx-aac-adts",
                expectedDrawable = fallbackDrawable
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
                mimeType = "image/png",
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
                expectedDrawable = fallbackDrawable
            ),
            TestInput(
                group = "Ppt",
                mimeType = "application/powerpoint",
                expectedDrawable = fallbackDrawable
            ),
            TestInput(
                group = "Ppt",
                mimeType = "application/vnd.ms-powerpoint",
                expectedDrawable = fallbackDrawable
            ),
            TestInput(
                group = "Ppt",
                mimeType = "application/x-mspowerpoint",
                expectedDrawable = fallbackDrawable
            ),
            TestInput(
                group = "Ppt",
                mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                expectedDrawable = fallbackDrawable
            )
        )

        private val txt = listOf(
            TestInput(
                group = "Txt",
                mimeType = "text/plain",
                expectedDrawable = fallbackDrawable
            )
        )

        private val video = listOf(
            TestInput(
                group = "Video",
                mimeType = "video/quicktime",
                expectedDrawable = fallbackDrawable
            ),
            TestInput(
                group = "Video",
                mimeType = "video/x-quicktime",
                expectedDrawable = fallbackDrawable
            ),
            TestInput(
                group = "Video",
                mimeType = "image/mov",
                expectedDrawable = fallbackDrawable
            ),
            TestInput(
                group = "Video",
                mimeType = "audio/aiff",
                expectedDrawable = fallbackDrawable
            ),
            TestInput(
                group = "Video",
                mimeType = "audio/x-midi",
                expectedDrawable = fallbackDrawable
            ),
            TestInput(
                group = "Video",
                mimeType = "audio/x-wav",
                expectedDrawable = fallbackDrawable
            ),
            TestInput(
                group = "Video",
                mimeType = "video/avi",
                expectedDrawable = fallbackDrawable
            ),
            TestInput(
                group = "Video",
                mimeType = "video/mp4",
                expectedDrawable = fallbackDrawable
            ),
            TestInput(
                group = "Video",
                mimeType = "video/x-matroska",
                expectedDrawable = fallbackDrawable
            )
        )

        private val xls = listOf(
            TestInput(
                group = "Xls",
                mimeType = "application/excel",
                expectedDrawable = fallbackDrawable
            ),
            TestInput(
                group = "Xls",
                mimeType = "application/vnd.ms-excel",
                expectedDrawable = fallbackDrawable
            ),
            TestInput(
                group = "Xls",
                mimeType = "application/x-excel",
                expectedDrawable = fallbackDrawable
            ),
            TestInput(
                group = "Xls",
                mimeType = "application/x-msexcel",
                expectedDrawable = fallbackDrawable
            ),
            TestInput(
                group = "Xls",
                mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                expectedDrawable = fallbackDrawable
            )
        )

        private val zip = listOf(
            TestInput(
                group = "Zip",
                mimeType = "application/zip",
                expectedDrawable = expectedZipDrawable
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = (
            audio +
                doc +
                image +
                pdf +
                ppt +
                txt +
                video +
                xls +
                zip
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
