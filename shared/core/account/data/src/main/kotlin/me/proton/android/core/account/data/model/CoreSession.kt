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

package me.proton.android.core.account.data.model

import me.proton.android.core.account.domain.model.CoreSession
import me.proton.android.core.account.domain.model.CoreSessionId
import me.proton.android.core.account.domain.model.CoreSessionState
import me.proton.android.core.account.domain.model.CoreUserId
import uniffi.mail_uniffi.StoredSession
import uniffi.mail_uniffi.StoredSessionState

internal fun StoredSession.toCoreSession() = CoreSession(
    sessionId = CoreSessionId(sessionId()),
    state = state().toCoreSessionState(),
    userId = CoreUserId(userId())
)

internal fun StoredSessionState.toCoreSessionState() = when (this) {
    StoredSessionState.AUTHENTICATED -> CoreSessionState.Authenticated
    StoredSessionState.NEED_KEY -> CoreSessionState.NeedKey
    StoredSessionState.NEED_TFA -> CoreSessionState.NeedSecondFactor
}
