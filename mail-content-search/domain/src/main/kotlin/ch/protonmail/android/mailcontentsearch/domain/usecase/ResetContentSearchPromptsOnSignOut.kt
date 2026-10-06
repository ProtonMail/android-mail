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

import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailcontentsearch.domain.model.ContentSearchPrompt
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchPromptRepository
import ch.protonmail.android.mailsession.domain.model.AccountState
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

/**
 * Forgets the search screen's one-off messages for every account that signs out or is removed.
 *
 * Signing back in then shows the set-up offer and the "indexing complete" note again.
 *
 * It watches the account list instead of the sign-out button, because Rust can also sign an
 * account out, for example when its session expires.
 *
 * The first account list keeps only the answers of fully signed in accounts.
 * That covers whatever happened while the app was not running. After that, only an account that
 * changes to signed out or disappears is reset.
 *
 * Accounts still partway through signing in lose their answers too. Rust only reports a stored,
 * signed in account as [AccountState.Ready]. The other states come up only during sign-in.
 * This covers a user who was removed while the app was not running and then signs in again.
 *
 * Empty lists are skipped until the first real one. The account list starts out empty before Rust
 * answers, and sweeping that would forget every account's answers on each launch.
 */
class ResetContentSearchPromptsOnSignOut @Inject constructor(
    private val userSessionRepository: UserSessionRepository,
    private val promptRepository: ContentSearchPromptRepository,
    @AppScope private val appScope: CoroutineScope
) {

    operator fun invoke() {
        appScope.launch {
            var previous: Map<UserId, AccountState>? = null
            userSessionRepository.observeAccounts().collect { accounts ->
                val current = accounts.associate { it.userId to it.state }
                if (current.isEmpty() && previous == null) return@collect
                val last = previous
                previous = current

                if (last == null) {
                    sweep(current)
                } else {
                    // A removed account is no longer in the list, so it shows up as a missing id.
                    val removed = last.keys - current.keys
                    val signedOut = current.filter { (userId, state) ->
                        state == AccountState.Disabled && last[userId] != AccountState.Disabled
                    }.keys
                    (removed + signedOut).forEach { resetPrompts(it) }
                }
            }
        }
    }

    private suspend fun sweep(accounts: Map<UserId, AccountState>) {
        val signedIn = accounts.filterValues { it == AccountState.Ready }.keys
        promptRepository.retainOnly(signedIn)
            .onLeft { Timber.w("content-search: could not forget the prompts of signed out accounts") }
    }

    private suspend fun resetPrompts(userId: UserId) {
        ContentSearchPrompt.entries.forEach { prompt ->
            promptRepository.reset(userId, prompt)
                .onLeft { Timber.w("content-search: could not reset the $prompt prompt for $userId") }
        }
    }
}
