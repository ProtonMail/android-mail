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

import ch.protonmail.android.mailcomposer.domain.Transactor
import ch.protonmail.android.mailcomposer.domain.repository.MessagePasswordRepository
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class DeleteMessagePassword @Inject constructor(
    private val draftStateRepository: DraftStateRepository,
    private val messagePasswordRepository: MessagePasswordRepository,
    private val transactor: Transactor
) {

    suspend operator fun invoke(userId: UserId, messageId: MessageId) = transactor.performTransaction {
        val apiMessageId = draftStateRepository.observe(userId, messageId).first().onLeft {
            Timber.e("No draft state found for $messageId")
        }.getOrNull()?.apiMessageId

        return@performTransaction messagePasswordRepository.deleteMessagePassword(userId, apiMessageId ?: messageId)
    }
}
