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

import ch.protonmail.android.mailcommon.domain.coroutines.DefaultDispatcher
import ch.protonmail.android.mailcomposer.domain.model.DraftAction
import ch.protonmail.android.mailcomposer.domain.repository.DraftRepository
import ch.protonmail.android.mailcomposer.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class DraftSyncer @Inject constructor(
    private val draftStateRepository: DraftStateRepository,
    private val draftRepository: DraftRepository,
    @DefaultDispatcher
    private val dispatcher: CoroutineDispatcher
) {

    private var syncJob: Job? = null

    suspend fun start(
        userId: UserId,
        messageId: MessageId,
        action: DraftAction,
        scope: CoroutineScope
    ) {
        syncJob?.cancel()
        syncJob = scope.launch(dispatcher) {
            draftStateRepository.saveLocalState(userId, messageId, action)
            while (true) {
                delay(SyncInterval)
                Timber.d("Draft syncer: syncing draft $messageId")
                println("foooooo")
                draftRepository.sync(userId, messageId)
            }
        }
    }

    companion object {
        val SyncInterval = 1.seconds
    }
}
