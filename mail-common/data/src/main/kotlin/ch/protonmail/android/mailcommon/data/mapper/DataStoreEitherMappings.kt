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

package ch.protonmail.android.mailcommon.data.mapper

import java.io.IOException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

val DataStore<Preferences>.safeData: Flow<Either<PreferencesError, Preferences>>
    // type inference fails to resolve type for `Preferences.right() as Either<PreferencesError, Preferences>`
    @Suppress("USELESS_CAST")
    get() = data.map { it.right() as Either<PreferencesError, Preferences> }
        .catch { throwable ->
            if (throwable is IOException) {
                Timber.e(throwable, "Error reading preference")
                emit(PreferencesError.left())
            } else throw throwable
        }

suspend fun DataStore<Preferences>.safeEdit(
    transform: suspend (MutablePreferences) -> Unit
): Either<PreferencesError, Preferences> = try {
    edit(transform).right()
} catch (exception: IOException) {
    Timber.e(exception, "Error saving preference")
    PreferencesError.left()
}
