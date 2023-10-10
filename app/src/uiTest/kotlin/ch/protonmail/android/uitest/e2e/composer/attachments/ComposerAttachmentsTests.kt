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

package ch.protonmail.android.uitest.e2e.composer.attachments

import android.app.Activity
import android.app.Instrumentation
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import ch.protonmail.android.networkmocks.assets.RawAssets
import ch.protonmail.android.uitest.e2e.composer.ComposerTests
import ch.protonmail.android.uitest.util.InstrumentationHolder

internal interface ComposerAttachmentsTests : ComposerTests {

    /**
     * Pushes a file from internal assets into the Download directory of the targeted device
     * and returns the newly created file's URI.
     *
     * @param testAssetFileName the file name provided by internal assets.
     * @param downloadDirFileName the name of the locally copied file.
     * @param mimeType the MIME type of the file.
     *
     * @return the created file URI.
     */
    fun initFakeFileUri(testAssetFileName: String, downloadDirFileName: String, mimeType: String): Uri {
        val contentResolver = InstrumentationHolder.instrumentation.targetContext.contentResolver

        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, downloadDirFileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
        })

        requireNotNull(uri) { "Generated file Uri is null" } // `null` should never happen

        val assetPath = "$RawAssetsRootDir/$testAssetFileName"
        val outputStream = requireNotNull(contentResolver.openOutputStream(uri, "w")) { "Unable to open output stream" }
        val fileInputStream =
            requireNotNull(RawAssets.getInputStreamForPath(assetPath)) { "Unable to open input stream for path $assetPath" }

        fileInputStream.use { outputStream.write(it.readBytes()) }

        return uri
    }

    /**
     * Stubs the File Picker (or similar intent actions) Activity Result by passing the given [Uri] as the [Intent] data.
     *
     * @param uri the file URI that needs to be returned
     */
    fun stubPickerActivityResultWithUri(uri: Uri) {
        val intent = Intent().apply { data = uri }
        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, intent)
        Intents.intending(IntentMatchers.hasAction(Intent.ACTION_GET_CONTENT)).respondWith(result)
    }

    private companion object {

        val RawAssetsRootDir = "assets/raw"
    }
}
