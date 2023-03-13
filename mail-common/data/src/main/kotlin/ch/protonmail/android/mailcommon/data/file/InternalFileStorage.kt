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

package ch.protonmail.android.mailcommon.data.file

import java.security.MessageDigest
import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class InternalFileStorage @Inject constructor(
    @ApplicationContext
    private val applicationContext: Context,
    private val fileHelper: FileHelper
) {

    suspend fun readFromFile(
        userId: UserId,
        folder: Folder,
        fileIdentifier: FileIdentifier
    ): String? = fileHelper.readFromFile(
        folder = FileHelper.Folder("${userId.asRootDirectory()}${folder.path}"),
        filename = FileHelper.Filename(fileIdentifier.value.asSanitisedPath())
    )

    suspend fun writeToFile(
        userId: UserId,
        folder: Folder,
        fileIdentifier: FileIdentifier,
        content: String
    ): Boolean = fileHelper.writeToFile(
        folder = FileHelper.Folder("${userId.asRootDirectory()}${folder.path}"),
        filename = FileHelper.Filename(fileIdentifier.value.asSanitisedPath()),
        content = content
    )

    suspend fun deleteFile(
        userId: UserId,
        folder: Folder,
        fileIdentifier: FileIdentifier
    ): Boolean = fileHelper.deleteFile(
        folder = FileHelper.Folder("${userId.asRootDirectory()}${folder.path}"),
        filename = FileHelper.Filename(fileIdentifier.value.asSanitisedPath())
    )

    suspend fun deleteFolder(
        userId: UserId,
        folder: Folder
    ): Boolean = fileHelper.deleteFolder(
        folder = FileHelper.Folder("${userId.asRootDirectory()}${folder.path}")
    )

    private fun UserId.asRootDirectory() = "${applicationContext.filesDir}/${id.asSanitisedPath()}/"

    private fun String.asSanitisedPath(): String {
        val digest = MessageDigest.getInstance("SHA256").digest(this.toByteArray())
        return Base64.encodeToString(digest, Base64.URL_SAFE)
    }

    @JvmInline
    value class FileIdentifier(val value: String)

    enum class Folder(val path: String) {
        MESSAGE_BODIES("message_bodies/")
    }
}

/**
 * See https://www.sqlite.org/intern-v-extern-blob.html
 *
 * The article compares the performance of storing large blobs in db directly vs storing them as files on disk.
 * It does not directly relate to Android, and no actual testing was done on Android from our side.
 * The assumption is that the performance findings will roughly translate to Android nevertheless and the cut-off
 * threshold of 500kB is chosen based on the measurements from the article.
 */
@Suppress("MagicNumber")
fun String.shouldBeStoredAsFile() = this.toByteArray().size > 500 * 1024
