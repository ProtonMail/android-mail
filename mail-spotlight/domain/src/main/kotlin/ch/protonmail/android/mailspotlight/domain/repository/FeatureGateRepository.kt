/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailspotlight.domain.repository

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailspotlight.domain.model.Announcement
import ch.protonmail.android.mailspotlight.domain.model.FeatureEntryPoint
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId

/**
 * The SDK's feature gate: which announcement shows, and which entry points are visible.
 *
 * The answers of [observeEntryPointEnabled] are kept per user until [refreshEntryPoints], or until an announcement is
 * shown or a BYOE deep link is opened, which both change them.
 */
interface FeatureGateRepository {

    suspend fun getNextAnnouncement(userId: UserId): Either<DataError, Announcement?>

    suspend fun markAnnouncementShown(userId: UserId, announcement: Announcement): Either<DataError, Unit>

    suspend fun markAnnouncementResolved(userId: UserId, announcement: Announcement): Either<DataError, Unit>

    suspend fun isEntryPointEnabled(userId: UserId, entryPoint: FeatureEntryPoint): Either<DataError, Boolean>

    fun observeEntryPointEnabled(userId: UserId, entryPoint: FeatureEntryPoint): Flow<Boolean>

    suspend fun refreshEntryPoints(userId: UserId)

    suspend fun migrateClientState(userId: UserId, seen: List<Announcement>): Either<DataError, Unit>

    suspend fun markByoeDeepLinkOpened(userId: UserId): Either<DataError, Unit>

    /**
     * Makes the SDK forget which announcements were shown or resolved, so each shows again when due.
     *
     * For the debug menu only: a release build must never call it.
     */
    suspend fun debugResetAnnouncements(userId: UserId): Either<DataError, Unit>
}
