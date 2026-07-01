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
 *
 * On app launch and whenever the set of accounts changes, each ready account is reconciled against
 * its Rust content-search state:
 * - If content search is already enabled, nothing is done (and any stale opt-out marker is cleared).
 * - If it is disabled and the user has not deliberately opted out, it is enabled. This covers first
 *   sight (fresh login or accounts present after an app update) and also recovers when Rust loses the
 *   enabled state (e.g. an SDK or data reset) without a sign-out.
 * - A deliberate user *disable* (via DisableContentSearch) records an opt-out, which is respected
 *   here so it is never turned back on. The opt-out is cleared on sign-out (Rust state is wiped) so
 *   a later re-login starts fresh.
 * - The multi-account [StartContentIndexingSweep] is (re)started so newly logged-in accounts are
 *   picked up and any pending indexing resumes. The sweep worker discovers accounts at runtime, so a
 *   restart simply re-evaluates which account to index next (primary-first).
 * - When the app returns to the foreground the sweep is resumed via [ResumeContentIndexingSweep]
 *   (KEEP policy), so a run cancelled from the notification picks up again without waiting for a cold
 *   start; an already-running sweep is left untouched.
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
            // Persisted (not kept in memory) so a sign-out that happens while the process is dead is
            // still detected as a removal here on the next launch, rather than leaking a stale opt-out.
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
            // The account collector only (re)starts the sweep on a cold launch or when an account
            // newly becomes ready. Returning from the background is neither, so a sweep cancelled from
            // the notification would otherwise stay stopped until the process is recreated. Resume it
            // whenever the app comes to the foreground; KEEP leaves an in-progress sweep untouched.
            //
            // No eligibility guard here on purpose: whether an account still needs indexing is the
            // sweep worker's decision (FindFirstEligibleAccountToIndex), and it self-terminates cheaply
            // when nothing is eligible. Debounce instead so a burst of quick foreground/background
            // flips (app-switching) coalesces into a single resume rather than one enqueue each.
            appInBackgroundState.observe()
                .distinctUntilChanged()
                .filter { isInBackground -> !isInBackground }
                .debounce(ForegroundResumeDebounceMillis.milliseconds)
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

        // Coalesce bursts of quick foreground/background flips into a single sweep resume.
        const val ForegroundResumeDebounceMillis = 2_000L
    }
}
