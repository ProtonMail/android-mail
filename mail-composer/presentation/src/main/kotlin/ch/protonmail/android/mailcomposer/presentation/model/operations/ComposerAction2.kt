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

package ch.protonmail.android.mailcomposer.presentation.model.operations

import android.net.Uri
import ch.protonmail.android.mailcomposer.presentation.model.SenderUiModel
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import kotlin.time.Duration

internal sealed interface ComposerAction2 : ComposerStateOperation {
    data object ChangeSender : ComposerAction2
    data class SetSenderAddress(val sender: SenderUiModel) : ComposerAction2

    data object OpenExpirationSettings : ComposerAction2
    data class SetMessageExpiration(val duration: Duration) : ComposerAction2

    data object RespondInline : ComposerAction2

    data object CloseComposer : ComposerAction2
    data object SendMessage : ComposerAction2

    data object ConfirmSendWithNoSubject : ComposerAction2
    data object CancelSendWithNoSubject : ComposerAction2

    data object ConfirmSendExpirationSetToExternal : ComposerAction2
    data object CancelSendExpirationSetToExternal : ComposerAction2

    data object ClearSendingError : ComposerAction2

    data object OpenFilePicker : ComposerAction2
    data class StoreAttachments(val uriList: List<Uri>) : ComposerAction2
    data class RemoveAttachment(val attachmentId: AttachmentId) : ComposerAction2
}
