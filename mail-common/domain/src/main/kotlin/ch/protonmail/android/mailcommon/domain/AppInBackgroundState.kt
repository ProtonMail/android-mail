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

package ch.protonmail.android.mailcommon.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInBackgroundState @Inject constructor() {

    private val _state: MutableStateFlow<Boolean?> = MutableStateFlow(null)

    /**
     * Returns the current background state of the the app.
     *
     * If the state is unset, it defaults to `true`.
     */
    fun isAppInBackground(): Boolean = _state.value ?: true

    /**
     * Will emit the current background state of the app according to lifecycle events.
     */
    fun observe(): Flow<Boolean> = _state.asStateFlow().filterNotNull()

    @Synchronized
    fun setAppInBackground(isAppInBackground: Boolean) = _state.update { isAppInBackground }
}
