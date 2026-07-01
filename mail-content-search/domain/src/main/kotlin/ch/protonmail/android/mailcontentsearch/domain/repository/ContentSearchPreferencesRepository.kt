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
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import me.proton.core.domain.entity.UserId

interface ContentSearchPreferencesRepository {

    suspend fun getAllowMobileData(): Either<PreferencesError, Boolean>

    suspend fun setAllowMobileData(value: Boolean): Either<PreferencesError, Unit>

    /**
     * Whether the user has deliberately turned content search off for [userId]. Auto-enable respects
     * this so a manual *disable* is never clobbered, while still (re-)enabling accounts that were
     * never opted out — including recovering when Rust loses the enabled state (e.g. after an SDK or
     * data reset) without a sign-out.
     */
    suspend fun hasUserOptedOut(userId: UserId): Either<PreferencesError, Boolean>

    suspend fun markUserOptedOut(userId: UserId): Either<PreferencesError, Unit>

    /**
     * Clears the opt-out for [userId]. Called on sign-out (Rust state is wiped, so a re-login starts
     * fresh) and once content search is observed enabled again (a stale opt-out no longer applies).
     */
    suspend fun clearUserOptedOut(userId: UserId): Either<PreferencesError, Unit>

    /**
     * The set of accounts seen on a previous run. Persisted (rather than kept in memory) so a sign-out
     * that happens while the process is dead is still detected as a removal on the next launch.
     */
    suspend fun getKnownUserIds(): Either<PreferencesError, Set<UserId>>

    suspend fun saveKnownUserIds(userIds: Set<UserId>): Either<PreferencesError, Unit>
}
