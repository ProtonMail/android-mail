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

package ch.protonmail.android.mailcontentsearch.data.repository

import androidx.datastore.preferences.core.stringSetPreferencesKey
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.safeData
import ch.protonmail.android.mailcommon.data.mapper.safeEdit
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailcontentsearch.data.local.ContentSearchDataStoreProvider
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchPreferencesRepository
import ch.protonmail.android.mailsettings.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class ContentSearchPreferencesRepositoryImpl @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val dataStoreProvider: ContentSearchDataStoreProvider
) : ContentSearchPreferencesRepository {

    private val optedOutUserIdsKey = stringSetPreferencesKey("contentSearchOptedOutUserIdsPrefKey")
    private val knownUserIdsKey = stringSetPreferencesKey("contentSearchKnownUserIdsPrefKey")

    override suspend fun getAllowMobileData(): Either<PreferencesError, Boolean> =
        appSettingsRepository.observeAppSettings().first().useMobileDataForContentSearchIndexing.enabled.right()

    override suspend fun setAllowMobileData(value: Boolean): Either<PreferencesError, Unit> =
        appSettingsRepository.updateUseMobileDataForContentSearch(value).fold(
            ifLeft = { PreferencesError.left() },
            ifRight = { Unit.right() }
        )

    override suspend fun hasUserOptedOut(userId: UserId): Either<PreferencesError, Boolean> =
        dataStoreProvider.contentSearchDataStore.safeData.map { preferences ->
            preferences.map { it[optedOutUserIdsKey].orEmpty().contains(userId.id) }
        }.first()

    override suspend fun markUserOptedOut(userId: UserId): Either<PreferencesError, Unit> =
        dataStoreProvider.contentSearchDataStore.safeEdit { preferences ->
            preferences[optedOutUserIdsKey] = preferences[optedOutUserIdsKey].orEmpty() + userId.id
        }.map { }

    override suspend fun clearUserOptedOut(userId: UserId): Either<PreferencesError, Unit> =
        dataStoreProvider.contentSearchDataStore.safeEdit { preferences ->
            preferences[optedOutUserIdsKey] = preferences[optedOutUserIdsKey].orEmpty() - userId.id
        }.map { }

    override suspend fun getKnownUserIds(): Either<PreferencesError, Set<UserId>> =
        dataStoreProvider.contentSearchDataStore.safeData.map { preferences ->
            preferences.map { it[knownUserIdsKey].orEmpty().map(::UserId).toSet() }
        }.first()

    override suspend fun saveKnownUserIds(userIds: Set<UserId>): Either<PreferencesError, Unit> =
        dataStoreProvider.contentSearchDataStore.safeEdit { preferences ->
            preferences[knownUserIdsKey] = userIds.map { it.id }.toSet()
        }.map { }
}
