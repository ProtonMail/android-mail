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

package ch.protonmail.android.mailcontentsearch.domain.usecase

import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchRepository
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

/**
 * Mirrors the app-wide mobile data preference onto [userId]'s Rust flag.
 *
 * Rust defaults to allowing metered connections, so an account that was signed out (or not yet
 * unlocked) when the user turned the toggle off would silently index over cellular on its next
 * launch. Called whenever an account becomes ready.
 */
class ApplyContentSearchMobileDataPreference @Inject constructor(
    private val isContentSearchAllowedOnMobileData: IsContentSearchAllowedOnMobileData,
    private val contentSearchRepository: ContentSearchRepository
) {

    suspend operator fun invoke(userId: UserId) {
        val allowed = isContentSearchAllowedOnMobileData()
        // Only write on a mismatch: the setter is an actor round-trip, and it re-evaluates the
        // network guard, so a redundant call on every account emission is not free.
        val current = contentSearchRepository.isMeteredConnectionAllowed(userId).getOrNull()
        if (current == allowed) return

        contentSearchRepository.setMeteredConnectionAllowed(userId, allowed).onLeft {
            Timber.w("content-search: could not apply the mobile data preference on account ready: $it")
        }
    }
}
