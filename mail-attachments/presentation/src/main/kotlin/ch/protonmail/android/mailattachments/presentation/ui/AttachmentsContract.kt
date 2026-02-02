/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailattachments.presentation.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import ch.protonmail.android.mailattachments.presentation.R

data class SaveAttachmentInput(
    val fileName: String,
    val mimeType: String
)

/**
 * [ActivityResultContract] to create an intent to save a document/file via the external file manager.
 */
class CreateDocumentWithMimeType : ActivityResultContract<SaveAttachmentInput, Uri?>() {

    override fun createIntent(context: Context, input: SaveAttachmentInput): Intent {
        val (fileName, mimeType) = input
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?) =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

data class OpenAttachmentInput(
    val uri: Uri,
    val mimeType: String
)

/**
 * [ActivityResultContract] to trigger an intent to open a document/file through an external application.
 *
 * When using this, remember to check at the call site if the intent can be resolved.
 * See [ch.protonmail.android.mailattachments.presentation.IntentHelper.canOpenFile].
 */
class OpenAttachmentContract : ActivityResultContract<OpenAttachmentInput, Unit>() {

    override fun createIntent(context: Context, input: OpenAttachmentInput): Intent {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(input.uri, input.mimeType)
            putExtra(Intent.EXTRA_STREAM, input.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return Intent.createChooser(intent, context.getString(R.string.open_with))
    }

    override fun parseResult(resultCode: Int, intent: Intent?) {}
}
