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
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchSettingsRepository
import ch.protonmail.android.mailcontentsearch.domain.usecase.ApplyContentSearchMobileDataPreference
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchEnabled
import ch.protonmail.android.mailsession.domain.model.AccountState
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

/**
 * Keeps every account's content-search state in step with the app-wide intent, so the user does not
 * have to opt each one in by hand.
 *
 * It only reconciles per-account state. Actually running the indexing queue belongs to the Rust
 * orchestrator, which is started from the app's foreground transitions.
 */
class ContentSearchAutoIndexingHandler @Inject constructor(
    private val userSessionRepository: UserSessionRepository,
    private val isContentSearchEnabled: IsContentSearchEnabled,
    private val applyMobileDataPreference: ApplyContentSearchMobileDataPreference,
    private val settingsRepository: ContentSearchSettingsRepository,
    private val preferencesRepository: ContentSearchPreferencesRepository,
    @AppScope private val appScope: CoroutineScope
) {

    fun start() {
        observeAccountChanges()
    }

    private fun observeAccountChanges() {
        appScope.launch {
            var knownUserIds = preferencesRepository.getKnownUserIds().getOrElse { emptySet() }
            userSessionRepository.observeAccounts()
                .distinctUntilChanged()
                .collect { accounts ->
                    val currentUserIds = accounts.map { it.userId }.toSet()

                    // Persist the known-user set only when accounts list actually changes. Account removal (sign-out)
                    // is not collected as a state change; the entity simply stops being emitted, so
                    // clear the opt-out for removed accounts here so a future re-login starts fresh.
                    if (currentUserIds != knownUserIds) {
                        // Keep any user whose opt-out clear failed in the known set, so the removal is
                        // re-diffed and retried on the next emission instead of being silently dropped.
                        val failedToClear = (knownUserIds - currentUserIds)
                            .filter { preferencesRepository.clearUserOptedOut(it).isLeft() }
                            .toSet()
                        knownUserIds = currentUserIds + failedToClear
                        preferencesRepository.saveKnownUserIds(knownUserIds)
                    }

                    accounts.filter { it.state == AccountState.Ready }.forEach {
                        reconcileAutoEnable(it.userId)
                        // Rust's per-account metered flag defaults to permissive, so a fresh login
                        // would opt itself back in to indexing over cellular.
                        applyMobileDataPreference(it.userId)
                    }
                }
        }
    }

    /**
     * Reconciles auto-enable for [userId] against the account's live Rust state, so it recovers when
     * Rust lost the enabled state without a sign-out while still respecting a deliberate opt-out.
     */
    private suspend fun reconcileAutoEnable(userId: UserId) {
        val enabled = isContentSearchEnabled(userId).getOrElse {
            Timber.w("Content search auto-enable: could not read state for user, skipping")
            return
        }
        if (enabled) {
            // Already on (auto-enabled earlier or turned on by the user): a stale opt-out no longer
            // applies. Only write when one is actually present, to avoid a redundant DataStore edit.
            if (preferencesRepository.hasUserOptedOut(userId).getOrElse { false }) {
                preferencesRepository.clearUserOptedOut(userId)
            }
            return
        }
        // Disabled in Rust: respect a deliberate opt-out, otherwise (re-)enable.
        if (preferencesRepository.hasUserOptedOut(userId).getOrElse { false }) return
        settingsRepository.setEnabled(userId, enabled = true).onLeft {
            Timber.w("Content search auto-enable failed for user: $it")
        }
    }
}
