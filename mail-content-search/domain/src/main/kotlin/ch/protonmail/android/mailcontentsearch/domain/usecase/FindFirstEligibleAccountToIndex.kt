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

import arrow.core.getOrElse
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import ch.protonmail.android.mailsession.domain.model.Account
import ch.protonmail.android.mailsession.domain.model.AccountState
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Identifies the next account that should be indexed by the content-search sweep.
 *
 * Reads live state: ready accounts ordered primary-first, returning the first one that has content
 * search enabled and whose indexing has not yet completed. Returns `null` when no account remains,
 * which signals the sweep to stop.
 *
 * [skip] excludes accounts the caller has already attempted and failed in the current sweep, so a
 * non-completed account that errors out (e.g. an interrupted Rust session) is not re-selected in a
 * tight loop. The set is sweep-scoped, so such an account is retried on the next sweep.
 *
 * Idempotent and resumable: completed accounts are excluded, so after a kill/reboot the sweep
 * naturally resumes the still-pending account.
 */
class FindFirstEligibleAccountToIndex @Inject constructor(
    private val userSessionRepository: UserSessionRepository,
    private val isContentSearchEnabled: IsContentSearchEnabled,
    private val getContentSearchIndexingStatus: GetContentSearchIndexingStatus
) {

    suspend operator fun invoke(skip: Set<UserId> = emptySet()): UserId? {
        val accounts = userSessionRepository.observeAccounts().first()
        val primaryUserId = userSessionRepository.observePrimaryUserId().first()

        return accounts
            .filter { it.state == AccountState.Ready && it.userId !in skip }
            .primaryFirst(primaryUserId)
            .firstOrNull { isEligible(it.userId) }
            ?.userId
    }

    private suspend fun isEligible(userId: UserId): Boolean {
        val enabled = isContentSearchEnabled(userId).getOrElse { false }
        if (!enabled) return false
        return getContentSearchIndexingStatus(userId) !is ContentIndexingState.Completed
    }

    private fun List<Account>.primaryFirst(primaryUserId: UserId?): List<Account> {
        if (primaryUserId == null) return this
        return sortedByDescending { it.userId == primaryUserId }
    }
}
