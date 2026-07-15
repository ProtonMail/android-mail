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
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchPreferencesRepository
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchSettingsRepository
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ResumeContentIndexingSweep
import ch.protonmail.android.mailcontentsearch.domain.usecase.StartContentIndexingSweep
import ch.protonmail.android.mailsession.domain.model.AccountState
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Drives content-search indexing automatically, without the user having to open settings.
 */
class ContentSearchAutoIndexingHandler @Inject constructor(
    private val userSessionRepository: UserSessionRepository,
    private val isContentSearchEnabled: IsContentSearchEnabled,
    private val settingsRepository: ContentSearchSettingsRepository,
    private val startContentIndexingSweep: StartContentIndexingSweep,
    private val resumeContentIndexingSweep: ResumeContentIndexingSweep,
    private val preferencesRepository: ContentSearchPreferencesRepository,
    private val appInBackgroundState: AppInBackgroundState,
    @AppScope private val appScope: CoroutineScope
) {

    fun start() {
        observeAccountChanges()
        observeForegroundResumes()
    }

    private fun observeAccountChanges() {
        appScope.launch {
            var knownUserIds = preferencesRepository.getKnownUserIds().getOrElse { emptySet() }
            var previousReadyUserIds = emptySet<UserId>()
            var sweepStarted = false
            userSessionRepository.observeAccounts()
                .distinctUntilChanged()
                .collect { accounts ->
                    val currentUserIds = accounts.map { it.userId }.toSet()

                    // Persist the known-user set only when accounts list actually changes, to avoid a
                    // DataStore write on every unrelated account emission. Account removal (sign-out)
                    // is not collected as a state change; the entity simply stops being emitted, so
                    // clear the opt-out for removed accounts here so a future re-login starts fresh.
                    if (currentUserIds != knownUserIds) {
                        (knownUserIds - currentUserIds).forEach { preferencesRepository.clearUserOptedOut(it) }
                        knownUserIds = currentUserIds
                        preferencesRepository.saveKnownUserIds(knownUserIds)
                    }

                    val readyUserIds = accounts.filter { it.state == AccountState.Ready }.map { it.userId }
                    readyUserIds.forEach { reconcileAutoEnable(it) }

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

    @OptIn(FlowPreview::class)
    private fun observeForegroundResumes() {
        appScope.launch {
            appInBackgroundState.observe()
                .distinctUntilChanged()
                .debounce(ForegroundResumeDebounceMillis.milliseconds)
                .filter { isInBackground -> !isInBackground }
                .collect { resumeContentIndexingSweep() }
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

    private companion object {

        const val ForegroundResumeDebounceMillis = 2_000L
    }
}
