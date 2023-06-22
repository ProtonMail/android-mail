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

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class AttachmentFileProvider : ContentProvider() {

    private val injections by lazy(LazyThreadSafetyMode.NONE) {
        EntryPointAccessors.fromApplication<AttachmentFileProviderEntryPoint>(requireNotNull(context))
    }

    override fun onCreate(): Boolean {
        Timber.d("AttachmentFileProvider onCreate")
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException()
    }

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        throw UnsupportedOperationException()
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        throw UnsupportedOperationException()
    }

    override fun openFile(uri: Uri, mode: String) = runBlocking {
        val filePath = uri.path ?: return@runBlocking null
        val attachmentHash = filePath.split("/").last().split(".").first()
        injections.decryptAttachmentFile().invoke(attachmentHash)
    }

    companion object {

        private val Context.AUTHORITY get() = "$packageName.attachments"

        fun getUri(
            context: Context,
            attachmentHash: String,
            extension: String
        ): Uri {
            val uri = Uri.parse("content://${context.AUTHORITY}/$attachmentHash.$extension")
            Timber.d("Attachment Uri: $uri")
            return uri
        }

    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AttachmentFileProviderEntryPoint {

        fun decryptAttachmentFile(): DecryptAttachmentFile
    }

}
