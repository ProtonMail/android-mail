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

package ch.protonmail.android.mailcomposer.domain.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.right
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.ProvideNewAttachmentId
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailSettings
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class InjectAddressPublicKeyIntoMessage @Inject constructor(
    private val attachmentRepository: AttachmentRepository,
    private val provideNewAttachmentId: ProvideNewAttachmentId,
    private val findLocalDraft: FindLocalDraft,
    private val getAddressPublicKey: GetAddressPublicKey,
    private val observeMailSettings: ObserveMailSettings
) {

    suspend operator fun invoke(userId: UserId, messageId: MessageId): Either<Error, Unit> = either {

        if (observeMailSettings(userId).firstOrNull()?.attachPublicKey != true) {
            return Unit.right()
        }

        val localDraft = ensureNotNull(findLocalDraft(userId, messageId)) {
            Error.DraftNotFound
        }

        val publicKey = getAddressPublicKey(
            userId,
            SenderEmail(localDraft.message.sender.address)
        ).mapLeft {
            Error.GettingAddressPublicKey
        }.bind()

        attachmentRepository.createAttachment(
            userId,
            localDraft.message.messageId,
            provideNewAttachmentId(),
            publicKey.fileName,
            publicKey.mimeType,
            publicKey.bytes
        ).mapLeft {
            Error.CreatingAttachment
        }.bind()
    }

    sealed interface Error {
        object DraftNotFound : Error
        object GettingAddressPublicKey : Error
        object CreatingAttachment : Error
    }

}
