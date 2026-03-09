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

package ch.protonmail.android.composer.data.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.composer.data.mapper.toOpenDraftError
import ch.protonmail.android.composer.data.wrapper.DraftWrapper
import ch.protonmail.android.composer.data.wrapper.DraftWrapperWithSyncStatus
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageId
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import uniffi.mail_uniffi.OpenDraftResult
import uniffi.mail_uniffi.openDraft
import javax.inject.Inject

class OpenRustDraft @Inject constructor() {

    suspend operator fun invoke(mailSession: MailUserSessionWrapper, messageId: LocalMessageId) =
        when (val result = openDraft(mailSession.getRustUserSession(), messageId)) {
            is OpenDraftResult.Error -> result.v1.toOpenDraftError().left()
            is OpenDraftResult.Ok -> DraftWrapperWithSyncStatus(
                DraftWrapper(result.v1.draft),
                result.v1.syncStatus
            ).right()
        }

}
