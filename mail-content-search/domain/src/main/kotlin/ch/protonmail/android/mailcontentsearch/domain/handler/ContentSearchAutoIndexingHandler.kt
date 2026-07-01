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

package ch.protonmail.android.mailcontentsearch.domain.handler

import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchPreferencesRepository
import ch.protonmail.android.mailcontentsearch.domain.usecase.SetContentSearchEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.StartContentIndexingSweep
import ch.protonmail.android.mailsession.domain.model.AccountState
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

/**
 * Drives content-search indexing automatically, without the user having to open settings.
 *
 * On app launch and whenever the set of accounts changes:
 * - Each account is auto-enabled for content search exactly once (on first sight), covering both a
 *   fresh login and accounts already present after an app update. The "applied" marker is persisted,
 *   so a later explicit user *disable* is respected and never re-enabled on the next launch.
 * - When an account is signed out it disappears from [UserSessionRepository.observeAccounts], so its
 *   marker is cleared: sign-out wipes the account's Rust content-search state, so a later re-login
 *   must be treated as a first sight again and re-enabled.
 * - The multi-account [StartContentIndexingSweep] is (re)started so newly logged-in accounts are
 *   picked up and any pending indexing resumes. The sweep worker discovers accounts at runtime, so a
 *   restart simply re-evaluates which account to index next (primary-first).
 */
class ContentSearchAutoIndexingHandler @Inject constructor(
    private val userSessionRepository: UserSessionRepository,
    private val setContentSearchEnabled: SetContentSearchEnabled,
    private val startContentIndexingSweep: StartContentIndexingSweep,
    private val preferencesRepository: ContentSearchPreferencesRepository,
    @AppScope private val appScope: CoroutineScope
) {

    fun start() {
        appScope.launch {
            val knownUserIds = mutableSetOf<UserId>()
            var previousReadyUserIds = emptySet<UserId>()
            var sweepStarted = false
            userSessionRepository.observeAccounts()
                .distinctUntilChanged()
                .collect { accounts ->
                    val currentUserIds = accounts.map { it.userId }.toSet()

                    // Account removal (sign-out) is not collected as a state change; the entity simply
                    // stops being emitted. Clear the marker so a future re-login re-enables.
                    (knownUserIds - currentUserIds).forEach { preferencesRepository.clearAutoEnableApplied(it) }
                    knownUserIds.clear()
                    knownUserIds.addAll(currentUserIds)

                    val readyUserIds = accounts.filter { it.state == AccountState.Ready }.map { it.userId }
                    readyUserIds.forEach { autoEnableOnFirstSight(it) }

                    // (Re)start the sweep on first run and whenever an account newly becomes ready (fresh
                    // login or an account unlocking later), so it is picked up even if the worker already
                    // finished. The worker discovers accounts at runtime, so a restart just re-evaluates.
                    val newlyReady = readyUserIds.toSet() - previousReadyUserIds
                    previousReadyUserIds = readyUserIds.toSet()
                    if (readyUserIds.isNotEmpty() && (!sweepStarted || newlyReady.isNotEmpty())) {
                        startContentIndexingSweep()
                        sweepStarted = true
                    }
                }
        }
    }

    /** Returns true when content search was enabled for [userId] as a result of this call. */
    private suspend fun autoEnableOnFirstSight(userId: UserId): Boolean {
        val alreadyApplied = preferencesRepository.hasAutoEnableBeenApplied(userId).getOrElse { false }
        if (alreadyApplied) return false

        return setContentSearchEnabled(userId, enabled = true).fold(
            ifLeft = { error ->
                Timber.w("Content search auto-enable failed for user: $error")
                false
            },
            ifRight = {
                preferencesRepository.markAutoEnableApplied(userId)
                true
            }
        )
    }
}
