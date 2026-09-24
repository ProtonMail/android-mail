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

package ch.protonmail.android.mailcontentsearch.data.repository

import androidx.datastore.preferences.core.booleanPreferencesKey
import arrow.core.Either
import ch.protonmail.android.mailcommon.data.mapper.safeData
import ch.protonmail.android.mailcommon.data.mapper.safeEdit
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailcontentsearch.data.local.ContentSearchDataStoreProvider
import ch.protonmail.android.mailcontentsearch.domain.model.ContentSearchPrompt
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchPromptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class ContentSearchPromptRepositoryImpl @Inject constructor(
    private val dataStoreProvider: ContentSearchDataStoreProvider
) : ContentSearchPromptRepository {

    override fun observeDismissed(
        userId: UserId,
        prompt: ContentSearchPrompt
    ): Flow<Either<PreferencesError, Boolean>> = dataStoreProvider.contentSearchDataStore.safeData.map { prefsEither ->
        prefsEither.map { prefs -> prefs[dismissedKey(userId, prompt)] ?: false }
    }

    override suspend fun dismiss(userId: UserId, prompt: ContentSearchPrompt): Either<PreferencesError, Unit> =
        dataStoreProvider.contentSearchDataStore.safeEdit { prefs ->
            prefs[dismissedKey(userId, prompt)] = true
        }.map { }

    override suspend fun reset(userId: UserId, prompt: ContentSearchPrompt): Either<PreferencesError, Unit> =
        dataStoreProvider.contentSearchDataStore.safeEdit { prefs ->
            prefs.remove(dismissedKey(userId, prompt))
        }.map { }

    companion object {

        private const val DISMISSED_KEY_PREFIX = "contentSearchPromptDismissed_"

        /**
         * One key per account and message: an account that has never been shown a message still
         * gets it, whatever the other accounts on the device answered.
         *
         * Public because the debug menu clears these keys one account at a time.
         */
        fun dismissedKey(userId: UserId, prompt: ContentSearchPrompt) =
            booleanPreferencesKey("$DISMISSED_KEY_PREFIX${prompt.name}_${userId.id}")
    }
}
