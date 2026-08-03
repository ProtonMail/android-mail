/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailattachments.presentation.model

import ch.protonmail.android.mailattachments.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel

/**
 * The state of the single attachment download a list screen allows at a time: which pill is spinning,
 * the file once it is ready to hand to the opener, and the message shown when it cannot be downloaded.
 *
 * Screen-agnostic so any list that offers attachment pills (mailbox, content search) drives it through
 * the shared [ch.protonmail.android.mailattachments.presentation.reducer.AttachmentDownloadReducer]
 * rather than re-implementing the transitions. Screens embed or map it into their own state container.
 */
data class AttachmentDownloadState(
    val downloadingAttachmentId: AttachmentIdUiModel?,
    val openAttachment: Effect<OpenAttachmentIntentValues>,
    val error: Effect<TextUiModel>
) {

    companion object {

        val Initial = AttachmentDownloadState(
            downloadingAttachmentId = null,
            openAttachment = Effect.empty(),
            error = Effect.empty()
        )
    }
}
