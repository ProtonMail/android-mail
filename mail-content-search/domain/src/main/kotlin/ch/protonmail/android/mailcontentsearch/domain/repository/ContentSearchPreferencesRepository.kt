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
     * Whether content search has already been auto-enabled for [userId]. Used to enable indexing
     * exactly once per account (on first sight), so that a subsequent explicit user *disable* is not
     * clobbered on the next app launch.
     */
    suspend fun hasAutoEnableBeenApplied(userId: UserId): Either<PreferencesError, Boolean>

    suspend fun markAutoEnableApplied(userId: UserId): Either<PreferencesError, Unit>

    /**
     * Clears the auto-enable marker for [userId], to be called when the account is signed out. The
     * account's Rust content-search state is wiped on sign-out, so a later re-login must count as a
     * first sight again and re-enable indexing.
     */
    suspend fun clearAutoEnableApplied(userId: UserId): Either<PreferencesError, Unit>
}
