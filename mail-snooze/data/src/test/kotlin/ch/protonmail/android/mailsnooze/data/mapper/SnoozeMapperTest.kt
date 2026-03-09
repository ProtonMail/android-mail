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

import ch.protonmail.android.mailcommon.data.mapper.LocalNonDefaultWeekStart
import ch.protonmail.android.mailcommon.domain.model.DataError
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
import org.junit.Assert
import uniffi.mail_uniffi.ProtonError
import uniffi.mail_uniffi.SnoozeActions
import uniffi.mail_uniffi.SnoozeErrorReason
import uniffi.mail_uniffi.SnoozeTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import uniffi.mail_uniffi.SnoozeError as SnoozeErrorRemote

class SnoozeMapperTest {

    @Test
    fun `map snooze actions with custom option AND unsnooze`() {
        val snoozeActions = SnoozeActions(
            options = sampleSnoozeOptions.toMutableList().apply { add(SnoozeTime.Custom) },
            showUnsnooze = true
        )
        Assert.assertEquals(
            expectedSnoozeActions.toMutableList().apply {
                add(CustomUnset)
                add(UnSnooze)
            },
            snoozeActions.toSnoozeActions().toMutableList()
        )
    }

    @Test
    fun `map snooze actions with custom option AND unsnooze false`() {
        val snoozeActions = SnoozeActions(
            options = sampleSnoozeOptions.toMutableList().apply { add(SnoozeTime.Custom) },
            showUnsnooze = false
        )
        Assert.assertEquals(
            expectedSnoozeActions.toMutableList().apply {
                add(CustomUnset)
            },
            snoozeActions.toSnoozeActions().toMutableList()
        )
    }

    @Test
    fun `map snooze actions AND upsell`() {
        val snoozeActions = SnoozeActions(
            options = sampleSnoozeOptions.toMutableList(),
            showUnsnooze = false
        )
        Assert.assertEquals(
            expectedSnoozeActions.toMutableList().apply {
                add(UpgradeRequired)
            },
            snoozeActions.toSnoozeActions().toMutableList()
        )
    }

    @Test
    fun `map proton error to snooze error`() {
        val error = SnoozeErrorRemote.Other(ProtonError.Network)
        assertEquals(
            SnoozeError.Other(DataError.Remote.NoNetwork),
            error.toSnoozeError()
        )
    }

    @Test
    fun `map remote snooze error to snooze error`() {
        val error = SnoozeErrorRemote.Reason(SnoozeErrorReason.SNOOZE_TIME_IN_THE_PAST)
        assertEquals(
            SnoozeError.SnoozeIsInThePast,
            error.toSnoozeError()
        )
    }

    @Test
    fun `map proton error to unsnooze error`() {
        val error = SnoozeErrorRemote.Other(ProtonError.Network)
        assertEquals(
            UnsnoozeError.Other(DataError.Remote.NoNetwork),
            error.toUnsnoozeError()
        )
    }

    @Test
    fun `map remote unsnooze error to snooze error`() {
        val error = SnoozeErrorRemote.Reason(SnoozeErrorReason.SNOOZE_TIME_IN_THE_PAST)
        assertEquals(
            UnsnoozeError.Other(),
            error.toUnsnoozeError()
        )
    }


    @Test
    fun `map SnoozeWeekStart to remote week start `() {
        val expectedWeekStart =
            listOf(
                LocalNonDefaultWeekStart.MONDAY,
                LocalNonDefaultWeekStart.SATURDAY,
                LocalNonDefaultWeekStart.SUNDAY
            )
        assertEquals(
            expectedWeekStart,
            listOf(
                SnoozeWeekStart.MONDAY,
                SnoozeWeekStart.SATURDAY,
                SnoozeWeekStart.SUNDAY
            ).map { it.toLocalWeekStart() }
        )

    }

    @Test
    fun `map NON_SUPPORTED SnoozeWeekStart to default `() {
        val expectedWeekStart =
            listOf(
                LocalNonDefaultWeekStart.MONDAY,
                LocalNonDefaultWeekStart.MONDAY,
                LocalNonDefaultWeekStart.MONDAY,
                LocalNonDefaultWeekStart.MONDAY
            )
        assertEquals(
            expectedWeekStart,
            listOf(
                SnoozeWeekStart.TUESDAY,
                SnoozeWeekStart.WEDNESDAY,
                SnoozeWeekStart.THURSDAY,
                SnoozeWeekStart.FRIDAY
            ).map { it.toLocalWeekStart() }
        )

    }

    companion object {

        val expectedInstant = Instant.fromEpochSeconds(1_754_643_858_578L)
        const val inputMs = 1_754_643_858_578L

        val sampleSnoozeOptions = listOf(
            SnoozeTime.Tomorrow(inputMs.toULong()),
            SnoozeTime.NextWeek(inputMs.toULong()),
            SnoozeTime.LaterThisWeek(inputMs.toULong()),
            SnoozeTime.ThisWeekend(inputMs.toULong())
        )

        val expectedSnoozeActions = listOf<SnoozeOption>(
            Tomorrow(expectedInstant),
            NextWeek(expectedInstant),
            LaterThisWeek(expectedInstant),
            ThisWeekend(expectedInstant)
        )
    }
}


