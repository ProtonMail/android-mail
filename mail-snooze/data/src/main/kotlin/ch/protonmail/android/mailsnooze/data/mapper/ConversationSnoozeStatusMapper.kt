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

package ch.protonmail.android.mailsnooze.data.mapper

import ch.protonmail.android.mailsnooze.domain.model.ConversationSnoozeStatus
import ch.protonmail.android.mailsnooze.domain.model.NoSnooze
import ch.protonmail.android.mailsnooze.domain.model.SnoozeReminder
import ch.protonmail.android.mailsnooze.domain.model.Snoozed
import uniffi.mail_uniffi.Conversation
import uniffi.mail_uniffi.Message
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private fun ULong.instantFromEpochSeconds() =
    Instant.fromEpochMilliseconds(this.toLong() * 1.seconds.inWholeMilliseconds)

fun Conversation.toSnoozeInformation(): ConversationSnoozeStatus {
    return when {
        displaySnoozeReminder -> SnoozeReminder
        snoozedUntil != null -> Snoozed(until = snoozedUntil!!.instantFromEpochSeconds())
        else -> NoSnooze
    }
}

fun Message.toSnoozeInformation(): ConversationSnoozeStatus {
    return when {
        displaySnoozeReminder -> SnoozeReminder
        snoozedUntil != null -> Snoozed(until = snoozedUntil!!.instantFromEpochSeconds())
        else -> NoSnooze
    }
}
