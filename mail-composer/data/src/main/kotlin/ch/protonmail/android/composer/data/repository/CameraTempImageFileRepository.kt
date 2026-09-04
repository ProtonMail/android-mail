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
import android.content.Context
import arrow.core.Either
import ch.protonmail.android.mailcommon.data.file.FileHelper
import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.repository.CameraTempImageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CameraTempImageFileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    @AppScope private val coroutineScope: CoroutineScope,
    private val fileFactory: FileHelper.FileFactory,
    private val fileHelper: FileHelper
) : CameraTempImageRepository {

    override suspend fun getFile(name: String): Either<DataError, File> = withContext(ioDispatcher) {
        Either.catch {
            // Kept inside a dedicated sub directory so the FileProvider does not need a root over the whole cache.
            // The folder itself is created by `fileFrom`.
            val folder = File(context.cacheDir, CameraCaptureDirName)
            fileFactory.fileFrom(FileHelper.Folder(folder.absolutePath), FileHelper.Filename(name))
        }.mapLeft {
            DataError.Local.FailedToStoreFile
        }
    }

    override fun clearStagingArea() {
        coroutineScope.launch(ioDispatcher) {
            val folder = File(context.cacheDir, CameraCaptureDirName)
            fileHelper.deleteFolder(FileHelper.Folder(folder.absolutePath))
        }
    }

    private companion object {

        const val CameraCaptureDirName = "camera"
    }
}
