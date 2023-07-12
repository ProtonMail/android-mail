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
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class StoreDraftWithAllFields @Inject constructor(
    private val storeDraftWithSubject: StoreDraftWithSubject,
    private val storeDraftWithBody: StoreDraftWithBody
) {

    suspend operator fun invoke(
        userId: UserId,
        draftMessageId: MessageId,
        fields: DraftFields
    ) {
        withContext(NonCancellable) {
            storeDraftWithBody(draftMessageId, fields.body, fields.sender, userId).logError(draftMessageId)
            storeDraftWithSubject(userId, draftMessageId, fields.sender, fields.subject).logError(draftMessageId)
        }
    }

    private fun <T> Either<T, Unit>.logError(draftMessageId: MessageId) = this.onLeft { error ->
        Timber.e(
            "Storing all draft fields failed due to $error. \n Draft MessageId = $draftMessageId"
        )
    }
}
