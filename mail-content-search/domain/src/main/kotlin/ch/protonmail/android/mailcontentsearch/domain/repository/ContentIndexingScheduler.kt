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

package ch.protonmail.android.mailcontentsearch.domain.repository

import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import ch.protonmail.android.mailcontentsearch.domain.model.EnqueueIndexingResult
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId

interface ContentIndexingScheduler {

    suspend fun enqueue(userId: UserId, allowMobileData: Boolean): EnqueueIndexingResult

    /**
     * Enqueues a sweep that indexes every eligible account in turn under a single worker. The
     * worker discovers accounts at runtime, so no [UserId] is required.
     */
    suspend fun enqueueSweep(allowMobileData: Boolean): EnqueueIndexingResult

    fun cancel(userId: UserId)

    fun observeState(userId: UserId): Flow<ContentIndexingState>

    fun observeOngoingUserId(): Flow<UserId?>
}
