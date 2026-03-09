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

package ch.protonmail.android.mailsession.data.logging

import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.protocol.Message
import io.sentry.protocol.User
import uniffi.mail_issue_reporter_service_uniffi.IssueLevel
import uniffi.mail_issue_reporter_service_uniffi.IssueReporter
import javax.inject.Inject

class SentryIssueReporter @Inject constructor() : IssueReporter {


    override fun report(
        level: IssueLevel,
        userId: String?,
        message: String,
        keys: Map<String, String>
    ) {
        reportToSentry(level, message, keys, userId)
    }

    private fun reportToSentry(
        level: IssueLevel,
        message: String,
        keys: Map<String, String>,
        userId: String? = null
    ) {
        val event = SentryEvent()
        event.level = level.toSentryLevel()
        event.message = message.toSentryMessage()
        keys.forEach { key, value -> event.setExtra(key, value) }

        Sentry.captureEvent(event) { scope ->
            userId?.let {
                val user = User().apply { id = userId }
                scope.user = user
            }
        }
    }

    private fun IssueLevel.toSentryLevel() = when (this) {
        IssueLevel.CRITICAL -> SentryLevel.ERROR
        IssueLevel.ERROR -> SentryLevel.ERROR
        IssueLevel.WARNING -> SentryLevel.WARNING
    }

    private fun String.toSentryMessage(): Message {
        val sentryMessage = Message()
        sentryMessage.message = this
        return sentryMessage
    }
}
