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

package ch.protonmail.android.maildetail.domain.usecase

import arrow.core.left
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.DataErrorSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class MarkConversationAsUnreadTest {

    private val markUnread = MarkConversationAsUnread()

    @Test
    fun `returns error`() = runTest {
        // given
        val expected = DataErrorSample.NoCache.left()

        // when
        val result = markUnread(UserIdSample.Primary, ConversationIdSample.Invoices)

        // then
        assertEquals(expected, result)
    }
}
