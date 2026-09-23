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

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingActivity
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingStartOutcome
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingStartSummary
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId

interface ContentSearchRepository {

    suspend fun clearLocalData(userId: UserId): Either<DataError, Unit>

    /**
     * Starts the session-wide orchestrator, which picks its own accounts and order.
     *
     * Idempotent - an orchestrator that is already running reports its current stats instead of
     * restarting - so it is safe to call on every foreground transition.
     */
    suspend fun startIndexing(): Either<DataError, ContentIndexingStartSummary>

    /**
     * Starts indexing [userId] specifically, which is the only way out of a recorded failure: Rust
     * clears it on the way to taking the account, and never retries one on its own.
     */
    suspend fun startIndexingForUser(userId: UserId): Either<DataError, ContentIndexingStartOutcome>

    /** Stops the orchestrator for every account: there is no per-account stop. */
    suspend fun stopIndexing(): Either<DataError, Unit>

    /**
     * Pauses [userId]'s backfill, keeping everything already indexed.
     *
     * Rust records the pause, so it outlives the process. Only [startIndexingForUser] clears it.
     */
    suspend fun pauseIndexing(userId: UserId): Either<DataError, Unit>

    /**
     * The size of [userId]'s local index in bytes, or null when it cannot be read.
     *
     * Null also covers an account that content search is unavailable for.
     */
    suspend fun getIndexSizeBytes(userId: UserId): Long?

    fun observeIndexingActivity(): Flow<ContentIndexingActivity>

    fun observeIndexingStatus(userId: UserId): Flow<ContentIndexingState>

    suspend fun getIndexingStatus(userId: UserId): ContentIndexingState

}
