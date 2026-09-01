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

package ch.protonmail.android.mailattachments.domain.usecase

import android.net.Uri
import arrow.core.Either
import arrow.core.raise.either
import ch.protonmail.android.mailattachments.domain.model.AttachmentId
import ch.protonmail.android.mailattachments.domain.repository.AttachmentRepository
import ch.protonmail.android.mailcommon.domain.model.DataError
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Resolves the [Uri] of a decrypted attachment right before it is read.
 *
 * Decrypted attachments are stored in the app cache, which is evicted by the SDK once it grows past its size limit
 * and trimmed by the OS when the device runs low on storage. A [Uri] obtained earlier can therefore point at a file
 * that no longer exists, so any deferred read has to resolve it again rather than reuse the earlier one.
 */
class GetDecryptedAttachmentUri @Inject constructor(private val attachmentRepository: AttachmentRepository) {

    suspend operator fun invoke(userId: UserId, attachmentId: AttachmentId): Either<DataError, Uri> = either {
        attachmentRepository.getAttachment(userId, attachmentId).bind().fileUri
    }
}
