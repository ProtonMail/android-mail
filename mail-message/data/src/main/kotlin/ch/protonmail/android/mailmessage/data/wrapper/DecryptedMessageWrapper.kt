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

package ch.protonmail.android.mailmessage.data.wrapper

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentData
import ch.protonmail.android.mailcommon.data.mapper.LocalMimeType
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.data.mapper.toAttachmentDataError
import ch.protonmail.android.mailmessage.domain.model.AttachmentDataError
import timber.log.Timber
import uniffi.mail_uniffi.AttachmentDataResult
import uniffi.mail_uniffi.AttachmentMetadata
import uniffi.mail_uniffi.BodyOutput
import uniffi.mail_uniffi.BodyOutputResult
import uniffi.mail_uniffi.DecryptedMessage
import uniffi.mail_uniffi.ImagePolicy
import uniffi.mail_uniffi.TransformOpts
import uniffi.mail_uniffi.VoidActionResult

class DecryptedMessageWrapper(private val decryptedMessage: DecryptedMessage) {

    suspend fun body(transformOpts: TransformOpts): Either<DataError, BodyOutput> =
        when (val result = decryptedMessage.body(transformOpts)) {
            is BodyOutputResult.Error -> result.v1.toDataError().left()
            is BodyOutputResult.Ok -> result.v1.right()
        }

    suspend fun loadImage(url: String, imagePolicy: ImagePolicy): Either<AttachmentDataError, LocalAttachmentData> =
        when (val result = decryptedMessage.loadImage(url, imagePolicy)) {
            is AttachmentDataResult.Error -> {
                Timber.d("DecryptedMessageWrapper: Failed to load image: ${result.v1}")
                result.v1.toAttachmentDataError().left()
            }

            is AttachmentDataResult.Ok -> result.v1.right()
        }

    fun mimeType(): LocalMimeType = decryptedMessage.mimeType()

    fun attachments(): List<AttachmentMetadata> = decryptedMessage.attachments()

    suspend fun privacyLocks() = decryptedMessage.privacyLock()

    suspend fun identifyRsvp() = decryptedMessage.identifyRsvp()

    fun rawHeaders() = decryptedMessage.rawHeaders()

    fun rawBody() = decryptedMessage.rawBody()

    suspend fun unsubscribeFromNewsletter(): Either<DataError, Unit> =
        when (val result = decryptedMessage.unsubscribeFromNewsletter()) {
            is VoidActionResult.Error -> result.v1.toDataError().left()
            is VoidActionResult.Ok -> Unit.right()
        }
}
