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

package me.proton.android.core.auth.presentation.flow

import me.proton.android.core.auth.presentation.flow.FlowManager.CurrentFlow
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.StoredSession
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlowCache @Inject constructor(
    private val sessionInterface: MailSession
) {

    private var cachedFlow: CurrentFlow? = null
    private var cachedSession: StoredSession? = null
    private var cachedUserId: String? = null

    fun getActiveFlow(): CurrentFlow? = cachedFlow

    fun setActiveFlow(flow: CurrentFlow) {
        this.cachedFlow = flow
    }

    fun setCachedSession(session: StoredSession, userId: String) {
        cachedSession = session
        cachedUserId = userId
    }

    fun getCachedSession(): StoredSession? = cachedSession

    fun clearIfUserChanged(userId: String) {
        if (cachedUserId != null && cachedUserId != userId) {
            clear()
        }
    }

    suspend fun deleteAccount() {
        if (cachedFlow is CurrentFlow.LoggingIn) {
            cachedUserId?.let { sessionInterface.deleteAccount(it) }
        }
    }

    fun clear(): Boolean {
        cachedFlow = null
        cachedSession = null
        cachedUserId = null

        return true
    }
}
