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

import java.util.UUID
import ch.protonmail.android.mailmessage.domain.model.DraftState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import timber.log.Timber
import javax.inject.Inject

class IsDraftKnownToApi @Inject constructor() {

    operator fun invoke(draftState: DraftState): Boolean =
        draftState.apiMessageId != null || hasUuidFormat(draftState.messageId).not()


    private fun hasUuidFormat(messageId: MessageId) = try {
        UUID.fromString(messageId.id)
        true
    } catch (e: IllegalArgumentException) {
        Timber.d("Given messageId ($this) is not a local id (not in UUID format). $e")
        false
    }
}
