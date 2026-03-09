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

package ch.protonmail.android.mailsnooze.data

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalLabelId
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailsnooze.domain.model.LaterThisWeek
import ch.protonmail.android.mailsnooze.domain.model.NextWeek
import ch.protonmail.android.mailsnooze.domain.model.SnoozeError
import ch.protonmail.android.mailsnooze.domain.model.SnoozeWeekStart
import ch.protonmail.android.mailsnooze.domain.model.ThisWeekend
import ch.protonmail.android.mailsnooze.domain.model.Tomorrow
import ch.protonmail.android.mailsnooze.domain.model.UnsnoozeError
import ch.protonmail.android.mailsnooze.domain.model.UpgradeRequired
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import uniffi.mail_uniffi.Id
import uniffi.mail_uniffi.NonDefaultWeekStart
import uniffi.mail_uniffi.SnoozeActions
import uniffi.mail_uniffi.SnoozeTime
import kotlin.time.Instant

class RustSnoozeRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dataSource = mockk<RustSnoozeDataSource> {
        coEvery {
            this@mockk.getAvailableSnoozeActionsForConversation(
                SampleData.userId,
                SampleData.weekStart,
                SampleData.conversationIds
            )
        } returns SampleData.mockSnoozeActions.right()

        coEvery {
            this@mockk.snoozeConversation(
                SampleData.userId, SampleData.inputLabelId, SampleData.conversationIds,
                SampleData.snoozeTime
            )
        } returns Unit.right()

        coEvery {
            this@mockk.unSnoozeConversation(
                SampleData.userId, SampleData.inputLabelId, SampleData.conversationIds
            )
        } returns Unit.right()
    }
    private val sut = RustSnoozeRepositoryImpl(dataSource)

    @Test
    fun `when getAvailableSnoozeActionsForConversation then returns Result right`() = runTest {

        // when
        val result = sut.getAvailableSnoozeActions(
            SampleData.userId, SampleData.inputWeekStart,
            SampleData.inputConversationIds
        )

        // then
        assertEquals(
            listOf(
                Tomorrow(SampleData.expectedInstant),
                NextWeek(SampleData.expectedInstant),
                LaterThisWeek(SampleData.expectedInstant),
                ThisWeekend(SampleData.expectedInstant),
                UpgradeRequired
            ).right(),
            result
        )
    }

    @Test
    fun `when getAvailableSnoozeActionsForConversation then returns Result left`() = runTest {
        val error = SnoozeError.Other().left()
        coEvery {
            dataSource.getAvailableSnoozeActionsForConversation(
                SampleData.userId,
                SampleData.weekStart,
                SampleData.conversationIds
            )
        } returns error

        val result = sut.getAvailableSnoozeActions(
            SampleData.userId, SampleData.inputWeekStart,
            SampleData.inputConversationIds
        )

        assertEquals(error, result)
    }

    @Test
    fun `when snoozeConversations then returns Result right`() = runTest {

        val result = sut.snoozeConversation(
            SampleData.userId, SampleData.labelId, SampleData.inputConversationIds,
            Tomorrow(snoozeTime = SampleData.snoozeTime)
        )

        assertEquals(
            Unit.right(),
            result
        )
    }

    @Test
    fun `when snoozeConversations then returns Result error - left`() = runTest {
        val error = SnoozeError.SnoozeIsInThePast.left()
        coEvery {
            dataSource.snoozeConversation(
                SampleData.userId, SampleData.inputLabelId, SampleData.conversationIds,
                SampleData.snoozeTime
            )
        } returns error

        val result = sut.snoozeConversation(
            SampleData.userId, SampleData.labelId, SampleData.inputConversationIds,
            Tomorrow(snoozeTime = SampleData.snoozeTime)
        )

        assertEquals(
            error,
            result
        )
    }

    @Test
    fun `when unsnoozeConversations then returns Result right`() = runTest {

        val result = sut.unSnoozeConversation(
            SampleData.userId, SampleData.labelId, SampleData.inputConversationIds
        )

        assertEquals(
            Unit.right(),
            result
        )
    }

    @Test
    fun `when unsnoozeConversations then returns Result error - left`() = runTest {
        val error = UnsnoozeError.Other().left()
        coEvery {
            dataSource.unSnoozeConversation(
                SampleData.userId, SampleData.inputLabelId, SampleData.conversationIds
            )
        } returns error

        val result = sut.unSnoozeConversation(
            SampleData.userId, SampleData.labelId, SampleData.inputConversationIds
        )

        assertEquals(
            error,
            result
        )
    }

    object SampleData {

        val snoozeTime = Instant.fromEpochMilliseconds(1_754_638_586_075L)
        val inputLabelId = LocalLabelId(2L.toULong())
        val labelId = LabelId("2")
        val userId = UserId("UserId")
        val inputWeekStart = SnoozeWeekStart.MONDAY
        val weekStart = NonDefaultWeekStart.MONDAY
        private const val conversationIdFirst = 12
        private const val conversationIdSecond = 22
        val inputConversationIds =
            listOf(ConversationId(conversationIdFirst.toString()), ConversationId(conversationIdSecond.toString()))
        val conversationIds = listOf(Id(conversationIdFirst.toULong()), Id(conversationIdSecond.toULong()))
        const val inputSeconds = 1_754_394_159L
        val expectedInstant = Instant.fromEpochSeconds(inputSeconds)
        val mockSnoozeActions =
            SnoozeActions(
                options = listOf(
                    SnoozeTime.Tomorrow(inputSeconds.toULong()),
                    SnoozeTime.NextWeek(inputSeconds.toULong()),
                    SnoozeTime.LaterThisWeek(inputSeconds.toULong()),
                    SnoozeTime.ThisWeekend(inputSeconds.toULong())
                ),
                showUnsnooze = false
            )
    }
}
