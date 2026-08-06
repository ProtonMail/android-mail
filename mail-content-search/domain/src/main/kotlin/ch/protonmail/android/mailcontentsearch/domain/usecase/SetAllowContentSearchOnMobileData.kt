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

import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchPreferencesRepository
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchRepository
import ch.protonmail.android.mailsession.domain.model.AccountState
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

/**
 * The toggle is app-wide, but the flag the orchestrator actually obeys is per-account and lives in
 * Rust. Record the app-wide intent and fan it out to every account that is currently ready; accounts
 * that are signed out or still unlocking pick it up through [ApplyContentSearchMobileDataPreference].
 */
class SetAllowContentSearchOnMobileData @Inject constructor(
    private val repository: ContentSearchPreferencesRepository,
    private val contentSearchRepository: ContentSearchRepository,
    private val userSessionRepository: UserSessionRepository
) {

    suspend operator fun invoke(value: Boolean) {
        repository.setAllowMobileData(value)

        userSessionRepository.observeAccounts().first()
            .filter { it.state == AccountState.Ready }
            .forEach { account ->
                contentSearchRepository.setMeteredConnectionAllowed(account.userId, value).onLeft {
                    Timber.w("content-search: could not apply the mobile data preference to an account: $it")
                }
            }
    }
}
