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

import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailcontentsearch.domain.usecase.ApplyContentSearchMobileDataPreference
import ch.protonmail.android.mailsession.domain.model.AccountState
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pushes the content-search settings Rust cannot infer onto each account as it becomes ready.
 *
 * Whether content search is on for an account is Rust's own state - enabled by default, with an
 * explicit disable persisted in its database - so there is nothing to reconcile there. The mobile
 * data preference is the exception: one app-wide toggle in our UI, mirrored onto a per-account Rust
 * flag that defaults to permissive.
 *
 * Trusting Rust's database does give up one recovery the local opt-out ledger used to provide: if
 * that database is reset without a sign-out - an SDK migration, a data reset - the disable is lost
 * with it, and the account starts indexing again with nothing on this side to stop it. Accepted
 * rather than overlooked. Keeping a second copy of the flag means keeping the two in step, and the
 * ledger only ever diverged in the other direction: it went stale whenever the disable was changed
 * anywhere but this app.
 */
class ContentSearchAccountReadyHandler @Inject constructor(
    private val userSessionRepository: UserSessionRepository,
    private val applyMobileDataPreference: ApplyContentSearchMobileDataPreference,
    @AppScope private val appScope: CoroutineScope
) {

    fun start() {
        appScope.launch {
            userSessionRepository.observeAccounts()
                .distinctUntilChanged()
                .collect { accounts ->
                    accounts.filter { it.state == AccountState.Ready }.forEach {
                        // An account that was signed out (or still locked) when the user turned the
                        // toggle off would otherwise index over cellular on its next launch.
                        applyMobileDataPreference(it.userId)
                    }
                }
        }
    }
}
