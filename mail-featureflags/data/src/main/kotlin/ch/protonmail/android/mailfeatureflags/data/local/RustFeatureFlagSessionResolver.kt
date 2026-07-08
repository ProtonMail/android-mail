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

package ch.protonmail.android.mailfeatureflags.data.local

import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import kotlinx.coroutines.flow.firstOrNull
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailUserSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves which Rust session should answer feature-flag queries: the logged-in
 * [MailUserSession] when available, otherwise the pre-login [MailSession].
 *
 * `MailSession::isFeatureEnabled` won't refresh while a user session is active, so callers must
 * always go through the resolved session rather than picking one themselves.
 */
@Singleton
class RustFeatureFlagSessionResolver @Inject constructor(
    private val userSessionRepository: UserSessionRepository,
    private val mailSessionRepository: MailSessionRepository
) {

    internal suspend fun activeSession(): ActiveSession {
        val userSession = loggedInUserSession()
        return if (userSession != null) {
            ActiveSession.User(userSession)
        } else {
            ActiveSession.App(mailSessionRepository.getMailSession().getRustMailSession())
        }
    }

    private suspend fun loggedInUserSession(): MailUserSession? {
        if (mailSessionRepository.isMailSessionInitialised().not()) return null
        val userId = userSessionRepository.observePrimaryUserId().firstOrNull() ?: return null
        return userSessionRepository.getUserSession(userId)?.getRustUserSession()
    }
}

internal sealed interface ActiveSession {
    data class User(val session: MailUserSession) : ActiveSession
    data class App(val session: MailSession) : ActiveSession
}
