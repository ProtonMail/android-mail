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

package ch.protonmail.android.composer.data.repository

import java.io.File
import java.util.UUID
import android.content.Context
import android.util.Base64
import arrow.core.Either
import ch.protonmail.android.mailcommon.data.file.FileHelper
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.repository.PastedImageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PastedImageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    private val fileHelper: FileHelper
) : PastedImageRepository {

    override suspend fun saveBase64ImageToCache(base64: String): Either<DataError, File> = withContext(ioDispatcher) {
        Either.catch {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val folder = File(context.cacheDir, PastedImagesDirName)
            fileHelper.writeToFile(
                FileHelper.Folder(folder.absolutePath),
                FileHelper.Filename("${UUID.randomUUID()}.${bytes.imageExtension()}"),
                bytes
            ) ?: error("Failed to write pasted image to cache")
        }.mapLeft {
            DataError.Local.FailedToStoreFile
        }
    }

    // Pasted images arrive as raw base64 without a filename or mime type, so infer a sane extension from the
    // magic bytes. The FileProvider derives the content mime type from this extension, which is what routes the
    // image to the inline-attachment path. Anything unrecognised defaults to png, a valid inline image type.
    private fun ByteArray.imageExtension(): String = when {
        startsWith(SignatureJpeg) -> "jpg"
        startsWith(SignatureGif) -> "gif"
        startsWith(SignatureBmp) -> "bmp"
        isWebp() -> "webp"
        isHeic() -> "heic"
        else -> "png"
    }

    private fun ByteArray.startsWith(signature: List<Int>): Boolean {
        if (size < signature.size) return false
        return signature.withIndex().all { (index, byte) -> this[index] == byte.toByte() }
    }

    // WebP is a RIFF container: bytes 0..3 are "RIFF" and bytes 8..11 are "WEBP".
    private fun ByteArray.isWebp(): Boolean {
        if (size < WebpHeaderSize) return false
        return startsWith(SignatureRiff) &&
            SignatureWebp.withIndex().all { (index, byte) -> this[index + WebpFormatOffset] == byte.toByte() }
    }

    // HEIC/HEIF is an ISO base media container: bytes 4..7 spell "ftyp" and bytes 8..11 name the brand.
    private fun ByteArray.isHeic(): Boolean {
        if (size < HeicHeaderSize) return false
        val hasFileTypeBox = SignatureFtyp.withIndex()
            .all { (index, byte) -> this[index + HeicFtypOffset] == byte.toByte() }
        if (!hasFileTypeBox) return false
        return HeicBrands.any { brand ->
            brand.withIndex().all { (index, byte) -> this[index + HeicBrandOffset] == byte.toByte() }
        }
    }

    private companion object {

        const val PastedImagesDirName = "pasted_image_staging"
        const val WebpHeaderSize = 12
        const val WebpFormatOffset = 8
        const val HeicHeaderSize = 12
        const val HeicFtypOffset = 4
        const val HeicBrandOffset = 8
        val SignatureJpeg = listOf(0xFF, 0xD8, 0xFF)
        val SignatureGif = listOf(0x47, 0x49, 0x46)
        val SignatureBmp = listOf(0x42, 0x4D)
        val SignatureRiff = listOf(0x52, 0x49, 0x46, 0x46)
        val SignatureWebp = listOf(0x57, 0x45, 0x42, 0x50)
        val SignatureFtyp = listOf(0x66, 0x74, 0x79, 0x70)
        // HEVC still-image and generic HEIF brands, all stored with a .heic extension.
        val HeicBrands = listOf(
            listOf(0x68, 0x65, 0x69, 0x63), // "heic"
            listOf(0x68, 0x65, 0x69, 0x78), // "heix"
            listOf(0x6D, 0x69, 0x66, 0x31), // "mif1"
            listOf(0x68, 0x65, 0x69, 0x6D), // "heim"
            listOf(0x68, 0x65, 0x69, 0x73) // "heis"
        )
    }
}
