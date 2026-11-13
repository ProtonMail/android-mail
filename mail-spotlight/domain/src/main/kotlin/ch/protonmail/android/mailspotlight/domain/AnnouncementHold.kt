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

package ch.protonmail.android.mailspotlight.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the announcements back while the app works out something that decides them, e.g. a BYOE deep link: opening
 * it counts as the BYOE spotlight seen, so no announcement may show until the link is handled.
 */
@Singleton
class AnnouncementHold @Inject constructor() {

    private val holders = MutableStateFlow(0)

    fun observeIsHeld(): Flow<Boolean> = holders.map { it > 0 }.distinctUntilChanged()

    /** The hold starts before [block] first suspends, and ends when it returns, fails or is cancelled. */
    suspend fun <T> hold(block: suspend () -> T): T {
        holders.update { it + 1 }
        return try {
            block()
        } finally {
            holders.update { it - 1 }
        }
    }
}
