/*
 * Copyright (c) 2022 Proton Technologies AG
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

package me.proton.android.core.account.data.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.android.core.account.data.qualifier.QueryWatcherCoroutineScope
import me.proton.android.core.account.domain.usecase.ObserveCoreSessions
import me.proton.android.core.account.domain.usecase.ObserveStoredAccounts
import uniffi.mail_uniffi.LiveQueryCallback
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailSessionGetAccountsResult
import uniffi.mail_uniffi.MailSessionWatchAccountsResult
import uniffi.mail_uniffi.StoredAccount
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObserveStoredAccountsImpl @Inject constructor(
    @QueryWatcherCoroutineScope private val coroutineScope: CoroutineScope,
    private val mailSession: MailSession,
    private val observeCoreSessions: ObserveCoreSessions
) : ObserveStoredAccounts {

    private val accountsMutableStateFlow = MutableStateFlow<List<StoredAccount>>(emptyList())
    private val watcherMutex = Mutex()

    private var watchedStoredAccounts: MailSessionWatchAccountsResult.Ok? = null

    private val storedAccountsWithSessionStateFlow: StateFlow<List<StoredAccount>> =
        accountsMutableStateFlow.stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    init {
        /**
         * Note:
         * Instead of just invoking [MailSession.watchAccounts],
         * we also call [observeCoreSessions],
         * so that the [StoredAccount.state] is always up to date.
         * This is needed because currently, a session state is not part of the "Accounts" SQL table.
         */
        coroutineScope.launch {
            observeCoreSessions().collect {
                refreshAccounts()
                ensureWatchingAccounts()
            }
        }
    }

    private fun ensureWatchingAccounts() {
        coroutineScope.launch {
            watcherMutex.withLock {
                if (watchedStoredAccounts != null) return@withLock

                when (
                    val result = mailSession.watchAccounts(
                        object : LiveQueryCallback {
                            override fun onUpdate() {
                                coroutineScope.launch {
                                    refreshAccounts()
                                }
                            }
                        }
                    )
                ) {
                    is MailSessionWatchAccountsResult.Ok -> {
                        watchedStoredAccounts = result
                        accountsMutableStateFlow.value = result.v1.accounts
                    }

                    is MailSessionWatchAccountsResult.Error -> {
                        accountsMutableStateFlow.value = emptyList()
                    }
                }
            }
        }
    }

    private suspend fun refreshAccounts() {
        when (val accountsResult = mailSession.getAccounts()) {
            is MailSessionGetAccountsResult.Ok -> {
                accountsMutableStateFlow.value = accountsResult.v1
            }

            is MailSessionGetAccountsResult.Error -> {
                accountsMutableStateFlow.value = emptyList()
            }
        }
    }

    override fun invoke(): Flow<List<StoredAccount>> = storedAccountsWithSessionStateFlow
}
