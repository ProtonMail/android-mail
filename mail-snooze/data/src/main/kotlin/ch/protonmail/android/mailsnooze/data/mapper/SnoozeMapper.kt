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

import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.data.mapper.LocalNonDefaultWeekStart
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailsnooze.domain.model.CustomUnset
import ch.protonmail.android.mailsnooze.domain.model.LaterThisWeek
import ch.protonmail.android.mailsnooze.domain.model.NextWeek
import ch.protonmail.android.mailsnooze.domain.model.SnoozeError
import ch.protonmail.android.mailsnooze.domain.model.SnoozeOption
import ch.protonmail.android.mailsnooze.domain.model.SnoozeWeekStart
import ch.protonmail.android.mailsnooze.domain.model.ThisWeekend
import ch.protonmail.android.mailsnooze.domain.model.Tomorrow
import ch.protonmail.android.mailsnooze.domain.model.UnSnooze
import ch.protonmail.android.mailsnooze.domain.model.UnsnoozeError
import ch.protonmail.android.mailsnooze.domain.model.UpgradeRequired
import timber.log.Timber
import uniffi.mail_uniffi.SnoozeActions
import uniffi.mail_uniffi.SnoozeErrorReason
import uniffi.mail_uniffi.SnoozeTime
import kotlin.time.Instant
import uniffi.mail_uniffi.SnoozeError as SnoozeErrorRemote

fun SnoozeErrorRemote.toSnoozeError(): SnoozeError = when (this) {
    is SnoozeErrorRemote.Other -> SnoozeError.Other(this.v1.toDataError())
    is SnoozeErrorRemote.Reason -> when (v1) {
        SnoozeErrorReason.SNOOZE_TIME_IN_THE_PAST -> SnoozeError.SnoozeIsInThePast
        SnoozeErrorReason.INVALID_SNOOZE_LOCATION -> SnoozeError.InvalidSnoozeLocation
    }
}

fun ConversationId.toLocalConversationId(): LocalConversationId = LocalConversationId(this.id.toULong())

fun SnoozeErrorRemote.toUnsnoozeError(): UnsnoozeError = when (this) {
    is SnoozeErrorRemote.Other -> UnsnoozeError.Other(this.v1.toDataError())
    is SnoozeErrorRemote.Reason -> when (v1) {
        SnoozeErrorReason.INVALID_SNOOZE_LOCATION -> UnsnoozeError.InvalidSnoozeLocation
        else -> UnsnoozeError.Other()
    }
}

fun SnoozeWeekStart.toLocalWeekStart() = when (this) {
    SnoozeWeekStart.MONDAY -> LocalNonDefaultWeekStart.MONDAY
    SnoozeWeekStart.SATURDAY -> LocalNonDefaultWeekStart.SATURDAY
    SnoozeWeekStart.SUNDAY -> LocalNonDefaultWeekStart.SUNDAY
    else -> {
        Timber.w("Unsupported week start given $this defaulting to Monday")
        LocalNonDefaultWeekStart.MONDAY
    }
}

fun SnoozeActions.toSnoozeActions(): List<SnoozeOption> {
    fun ULong.toInstant() = Instant.fromEpochSeconds(this.toLong())
    return this.options.map {
        when (it) {
            is SnoozeTime.Tomorrow -> Tomorrow(it.v1.toInstant())
            is SnoozeTime.ThisWeekend -> ThisWeekend(it.v1.toInstant())
            is SnoozeTime.LaterThisWeek -> LaterThisWeek(it.v1.toInstant())
            is SnoozeTime.NextWeek -> NextWeek(it.v1.toInstant())
            is SnoozeTime.Custom -> CustomUnset
        }
    }.toMutableList().apply {
        if (!options.contains(SnoozeTime.Custom)) {
            add(UpgradeRequired)
        }
        if (showUnsnooze) {
            add(UnSnooze)
        }
    }
}
