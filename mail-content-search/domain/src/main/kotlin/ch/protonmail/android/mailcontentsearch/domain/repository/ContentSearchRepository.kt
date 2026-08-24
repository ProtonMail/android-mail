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

    /**
     * Whether the feature is available for [userId], as answered by the Rust SDK.
     *
     * Not the plain feature flag: the SDK combines it with any additional eligibility rule, and applies
     * device-local debug overrides itself, so this must not be read through the feature flag resolver.
     * Answered by the user session, so it is false for an account that has no session yet - there is
     * nobody to ask before the account is ready.
     */
    suspend fun isFeatureEnabled(userId: UserId): Boolean

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

    fun observeIndexingActivity(): Flow<ContentIndexingActivity>

    fun observeIndexingStatus(userId: UserId): Flow<ContentIndexingState>

    suspend fun getIndexingStatus(userId: UserId): ContentIndexingState

    suspend fun shouldShowMobileBottomSheet(userId: UserId): Boolean
}
