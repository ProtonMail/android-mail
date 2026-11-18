/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailsession.domain.model

import ch.protonmail.android.testdata.user.UserTestData
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserTest {

    @Test
    fun `gainedSubscriptionSince is true when a free user gets Mail Plus`() {
        val free = UserTestData.build(subscribed = 0)
        val mailPlus = UserTestData.build(subscribed = 1)

        assertTrue(mailPlus.gainedSubscriptionSince(free))
    }

    @Test
    fun `gainedSubscriptionSince is true when Mail Plus becomes Unlimited`() {
        val mailPlus = UserTestData.build(subscribed = 1)
        val unlimited = UserTestData.build(subscribed = 7)

        assertTrue(unlimited.gainedSubscriptionSince(mailPlus))
    }

    @Test
    fun `gainedSubscriptionSince is false when a plan ends`() {
        val mailPlus = UserTestData.build(subscribed = 1)
        val free = UserTestData.build(subscribed = 0)

        assertFalse(free.gainedSubscriptionSince(mailPlus))
    }

    @Test
    fun `gainedSubscriptionSince is false when nothing changes`() {
        val mailPlus = UserTestData.build(subscribed = 1)

        assertFalse(mailPlus.gainedSubscriptionSince(mailPlus))
    }
}
