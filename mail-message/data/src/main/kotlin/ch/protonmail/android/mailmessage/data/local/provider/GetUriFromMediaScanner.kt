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

package ch.protonmail.android.mailmessage.data.local.provider

import java.io.File
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject

class GetUriFromMediaScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {


    suspend operator fun invoke(file: File, mimeType: String): Uri? {
        return suspendCancellableCoroutine { continuation ->
            val callback = MediaScannerConnection.OnScanCompletedListener { _, uri ->
                continuation.resume(uri, null)
            }
            // Register callback
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf(mimeType),
                callback
            )
            continuation.invokeOnCancellation { Timber.d("Attachment Uri resolution cancelled") }
        }
    }
}
