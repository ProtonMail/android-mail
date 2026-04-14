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

package ch.protonmail.android.logging

import java.util.UUID
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import io.sentry.Sentry
import io.sentry.protocol.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.CoroutineScopeProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SentryUserObserver @Inject constructor(
    private val scopeProvider: CoroutineScopeProvider,
    private val sessionRepository: UserSessionRepository
) {

    // The returning Job can be ignored in prod as it's a fire and forget call, it's only needed in tests.
    fun start(onCrashReportSettingChange: (enabled: Boolean) -> Unit): Job {
        assignSentryUser(userId = null)

        return sessionRepository.observePrimaryUserId()
            .onEach { userId ->
                assignSentryUser(userId)
                onCrashReportSettingChange(isCrashReportsEnabled(userId))
            }
            .launchIn(scopeProvider.GlobalDefaultSupervisedScope)
    }

    private fun assignSentryUser(userId: UserId?) {
        val user = User().apply { id = userId?.id ?: UUID.randomUUID().toString() }
        Sentry.setUser(user)
    }

    private suspend fun isCrashReportsEnabled(userId: UserId?): Boolean {
        userId ?: return true
        return sessionRepository.getUserSettings(userId)?.crashReports ?: false
    }
}
