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

import java.io.File
import java.io.InputStream
import android.content.Context
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.HashUtils
import javax.inject.Inject

class InternalFileStorage @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val fileHelper: FileHelper,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun readFromFile(
        userId: UserId,
        folder: Folder,
        fileIdentifier: FileIdentifier
    ): String? = fileHelper.readFromFile(
        folder = FileHelper.Folder("${userId.asRootDirectory()}${folder.path}"),
        filename = FileHelper.Filename(fileIdentifier.value.asSanitisedPath())
    )

    suspend fun readFromCachedFile(
        userId: UserId,
        folder: Folder,
        fileIdentifier: FileIdentifier
    ): String? = fileHelper.readFromFile(
        folder = FileHelper.Folder("${userId.asRootCacheDirectory()}${folder.path}"),
        filename = FileHelper.Filename(fileIdentifier.value.asSanitisedPath())
    )

    suspend fun renameFolder(
        userId: UserId,
        oldFolder: Folder,
        newFolder: Folder
    ) = fileHelper.renameFolder(
        oldFolder = FileHelper.Folder("${userId.asRootDirectory()}${oldFolder.path}"),
        newFolder = FileHelper.Folder("${userId.asRootDirectory()}${newFolder.path}")
    )

    suspend fun renameFile(
        userId: UserId,
        folder: Folder,
        oldFileIdentifier: FileIdentifier,
        newFileIdentifier: FileIdentifier
    ) = fileHelper.renameFile(
        folder = FileHelper.Folder("${userId.asRootDirectory()}${folder.path}"),
        oldFilename = FileHelper.Filename(oldFileIdentifier.value.asSanitisedPath()),
        newFilename = FileHelper.Filename(newFileIdentifier.value.asSanitisedPath())
    )

    suspend fun getFile(
        userId: UserId,
        folder: Folder,
        fileIdentifier: FileIdentifier
    ): File? = fileHelper.getFile(
        folder = FileHelper.Folder("${userId.asRootDirectory()}${folder.path}"),
        filename = FileHelper.Filename(fileIdentifier.value.asSanitisedPath())
    )

    suspend fun getCachedFile(
        userId: UserId,
        folder: Folder,
        fileIdentifier: FileIdentifier
    ): File? = fileHelper.getFile(
        folder = FileHelper.Folder("${userId.asRootCacheDirectory()}${folder.path}"),
        filename = FileHelper.Filename(fileIdentifier.value.asSanitisedPath())
    )

    suspend fun getFolder(userId: UserId, folder: Folder): File? =
        fileHelper.getFolder(FileHelper.Folder("${userId.asRootCacheDirectory()}${folder.path}"))

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

    suspend fun writeToCachedFile(
        userId: UserId,
        folder: Folder,
        fileIdentifier: FileIdentifier,
        content: String
    ): Boolean = fileHelper.writeToFile(
        folder = FileHelper.Folder("${userId.asRootCacheDirectory()}${folder.path}"),
        filename = FileHelper.Filename(fileIdentifier.value.asSanitisedPath()),
        content = content
    )

    suspend fun writeFile(
        userId: UserId,
        folder: Folder,
        fileIdentifier: FileIdentifier,
        content: ByteArray
    ): File? = fileHelper.writeToFile(
        folder = FileHelper.Folder("${userId.asRootDirectory()}${folder.path}"),
        filename = FileHelper.Filename(fileIdentifier.value.asSanitisedPath()),
        content = content
    )

    suspend fun writeCachedFile(
        userId: UserId,
        folder: Folder,
        fileIdentifier: FileIdentifier,
        content: ByteArray
    ): File? = fileHelper.writeToFile(
        folder = FileHelper.Folder("${userId.asRootCacheDirectory()}${folder.path}"),
        filename = FileHelper.Filename(fileIdentifier.value.asSanitisedPath()),
        content = content
    )

    suspend fun writeFileAsStream(
        userId: UserId,
        folder: Folder,
        fileIdentifier: FileIdentifier,
        inputStream: InputStream
    ): File? = fileHelper.writeToFileAsStream(
        folder = FileHelper.Folder("${userId.asRootDirectory()}${folder.path}"),
        filename = FileHelper.Filename(fileIdentifier.value.asSanitisedPath()),
        inputStream = inputStream
    )

    suspend fun deleteFile(
        userId: UserId,
        folder: Folder,
        fileIdentifier: FileIdentifier
    ): Boolean = fileHelper.deleteFile(
        folder = FileHelper.Folder("${userId.asRootDirectory()}${folder.path}"),
        filename = FileHelper.Filename(fileIdentifier.value.asSanitisedPath())
    )

    suspend fun deleteCachedFile(
        userId: UserId,
        folder: Folder,
        fileIdentifier: FileIdentifier
    ): Boolean = fileHelper.deleteFile(
        folder = FileHelper.Folder("${userId.asRootCacheDirectory()}${folder.path}"),
        filename = FileHelper.Filename(fileIdentifier.value.asSanitisedPath())
    )

    suspend fun deleteFolder(userId: UserId, folder: Folder): Boolean = fileHelper.deleteFolder(
        folder = FileHelper.Folder("${userId.asRootDirectory()}${folder.path}")
    )

    suspend fun deleteCachedFolder(userId: UserId, folder: Folder): Boolean = fileHelper.deleteFolder(
        folder = FileHelper.Folder("${userId.asRootCacheDirectory()}${folder.path}")
    )

    suspend fun copyCachedFileToNonCachedFolder(
        userId: UserId,
        sourceFolder: Folder,
        sourceFileIdentifier: FileIdentifier,
        targetFolder: Folder,
        targetFileIdentifier: FileIdentifier
    ): File? = fileHelper.copyFile(
        sourceFolder = FileHelper.Folder("${userId.asRootCacheDirectory()}${sourceFolder.path}"),
        sourceFilename = FileHelper.Filename(sourceFileIdentifier.value.asSanitisedPath()),
        targetFolder = FileHelper.Folder("${userId.asRootDirectory()}${targetFolder.path}"),
        targetFilename = FileHelper.Filename(targetFileIdentifier.value.asSanitisedPath())
    )

    private suspend fun UserId.asRootDirectory() = withContext(ioDispatcher) {
        "${applicationContext.filesDir}/${id.asSanitisedPath()}/"
    }

    private suspend fun UserId.asRootCacheDirectory() = withContext(ioDispatcher) {
        "${applicationContext.cacheDir}/${id.asSanitisedPath()}/"
    }

    private fun String.asSanitisedPath() = HashUtils.sha256(this)

    @JvmInline
    value class FileIdentifier(val value: String)

    sealed class Folder(open val path: String) {
        object MessageBodies : Folder("message_bodies/")
        object MessageAttachmentsRoot : Folder("attachments/")
        data class MessageAttachments(val messageId: String) : Folder("${MessageAttachmentsRoot.path}$messageId/")
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
