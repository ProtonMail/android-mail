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

package ch.protonmail.android.mailspotlight.data.repository

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailspotlight.data.mapper.toAnnouncement
import ch.protonmail.android.mailspotlight.data.mapper.toSdk
import ch.protonmail.android.mailspotlight.domain.model.Announcement
import ch.protonmail.android.mailspotlight.domain.model.FeatureEntryPoint
import ch.protonmail.android.mailspotlight.domain.repository.FeatureGateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.mail_uniffi.FeatureGate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureGateRepositoryImpl @Inject constructor(
    private val executeWithUserSession: ExecuteWithUserSession
) : FeatureGateRepository {

    private val entryPoints = MutableStateFlow<Map<EntryPointKey, Boolean>>(emptyMap())

    override suspend fun getNextAnnouncement(userId: UserId): Either<DataError, Announcement?> =
        withFeatureGate(userId) { gate ->
            gate.nextAnnouncement().also { Timber.d("feature-gate: next announcement = $it") }?.toAnnouncement()
        }

    override suspend fun markAnnouncementShown(userId: UserId, announcement: Announcement): Either<DataError, Unit> =
        withFeatureGate(userId) { gate -> gate.markAnnouncementShown(announcement.toSdk()) }
            .onRight { refreshEntryPoints(userId) }

    override suspend fun markAnnouncementResolved(userId: UserId, announcement: Announcement): Either<DataError, Unit> =
        withFeatureGate(userId) { gate -> gate.markAnnouncementResolved(announcement.toSdk()) }

    override suspend fun isEntryPointEnabled(
        userId: UserId,
        entryPoint: FeatureEntryPoint
    ): Either<DataError, Boolean> = withFeatureGate(userId) { gate -> gate.isEnabled(entryPoint.toSdk()) }
        .onRight { isEnabled -> entryPoints.update { it + (EntryPointKey(userId, entryPoint) to isEnabled) } }

    override fun observeEntryPointEnabled(userId: UserId, entryPoint: FeatureEntryPoint): Flow<Boolean> = flow {
        val key = EntryPointKey(userId, entryPoint)
        if (key !in entryPoints.value) fetch(key)
        emitAll(entryPoints.map { it[key] }.filterNotNull().distinctUntilChanged())
    }

    override suspend fun refreshEntryPoints(userId: UserId) {
        entryPoints.value.keys.filter { it.userId == userId }.forEach { fetch(it) }
    }

    override suspend fun migrateClientState(userId: UserId, seen: List<Announcement>): Either<DataError, Unit> =
        withFeatureGate(userId) { gate -> gate.migrateClientState(seen.map { it.toSdk() }) }

    override suspend fun markByoeDeepLinkOpened(userId: UserId): Either<DataError, Unit> =
        withFeatureGate(userId) { gate -> gate.markByoeDeepLinkOpened() }
            .onRight { refreshEntryPoints(userId) }

    // A failed read hides the entry point, as the SDK does when it cannot decide.
    private suspend fun fetch(key: EntryPointKey) {
        val isEnabled = isEntryPointEnabled(key.userId, key.entryPoint).getOrNull() ?: false
        entryPoints.update { it + (key to isEnabled) }
    }

    private suspend fun <T> withFeatureGate(userId: UserId, action: suspend (FeatureGate) -> T): Either<DataError, T> =
        executeWithUserSession(userId) { session -> session.getRustUserSession().featureGate().use { action(it) } }

    private data class EntryPointKey(val userId: UserId, val entryPoint: FeatureEntryPoint)
}
