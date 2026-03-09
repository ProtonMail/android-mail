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

package ch.protonmail.android.composer.data.wrapper

import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentId
import uniffi.mail_uniffi.AsyncLiveQueryCallback
import uniffi.mail_uniffi.AttachmentList

class AttachmentsWrapper(private val rustAttachmentList: AttachmentList) {

    fun attachmentUploadDirectory() = rustAttachmentList.attachmentUploadDirectory()
    suspend fun addAttachment(filePath: String, displayName: String) = rustAttachmentList.add(filePath, displayName)
    suspend fun addInlineAttachment(filePath: String, displayName: String) =
        rustAttachmentList.addInline(filePath, displayName)

    suspend fun removeAttachment(attachmentId: LocalAttachmentId) = rustAttachmentList.remove(attachmentId)
    suspend fun removeInlineAttachment(cid: String) = rustAttachmentList.removeWithCid(cid)
    suspend fun attachments() = rustAttachmentList.attachments()
    suspend fun createWatcher(callback: AsyncLiveQueryCallback) = rustAttachmentList.watcher(callback)
    suspend fun watcherStream() = rustAttachmentList.watcherStream()

    suspend fun transformToAttachment(cid: String) = rustAttachmentList.swapAttachmentDisposition(cid)
}
