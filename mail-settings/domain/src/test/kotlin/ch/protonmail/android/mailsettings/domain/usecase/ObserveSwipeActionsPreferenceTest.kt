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

package ch.protonmail.android.mailsettings.domain.usecase

import app.cash.turbine.test
import ch.protonmail.android.mailsettings.domain.model.SwipeActionsPreference
import ch.protonmail.android.testdata.mailsettings.MailSettingsTestData.buildMailSettings
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.mailsettings.domain.entity.SwipeAction
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ObserveSwipeActionsPreferenceTest {

    private val observeMailSettings: ObserveMailSettings = mockk()
    private val observeSwipeActionsPreference = ObserveSwipeActionsPreference(observeMailSettings)

    @Test
    fun `returns correct swipe preferences from Mail Settings`() = runTest {
        // given
        val swipeLeft = SwipeAction.MarkRead
        val swipeRight = SwipeAction.Archive
        val mailSettings = buildMailSettings(
            swipeLeft = swipeLeft,
            swipeRight = swipeRight
        )
        every { observeMailSettings(userId) } returns flowOf(mailSettings)

        // when
        observeSwipeActionsPreference(userId).test {

            // then
            val expected = SwipeActionsPreference(
                swipeLeft = swipeLeft,
                swipeRight = swipeRight
            )
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `doesn't emit when Mail Settings is null`() = runTest {
        // given
        every { observeMailSettings(userId) } returns flowOf(null)

        // when
        observeSwipeActionsPreference(userId).test {

            // then
            awaitComplete()
        }
    }

    @Test
    fun `defaults to action none when Swipe Left is null`() = runTest {
        // given
        val mailSettings = buildMailSettings(
            swipeLeft = null,
            swipeRight = SwipeAction.Archive
        )
        every { observeMailSettings(userId) } returns flowOf(mailSettings)

        // when
        observeSwipeActionsPreference(userId).test {

            // then
            val expected = SwipeActionsPreference(
                swipeLeft = SwipeAction.None,
                swipeRight = SwipeAction.Archive
            )
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns correct swipe preferences when the Mail Settings is restored from null`() = runTest {
        // given
        val swipeLeft = SwipeAction.MarkRead
        val swipeRight = SwipeAction.Archive
        val mailSettings = buildMailSettings(
            swipeLeft = swipeLeft,
            swipeRight = swipeRight
        )
        every { observeMailSettings(userId) } returns flowOf(null, mailSettings)

        // when
        observeSwipeActionsPreference(userId).test {

            // then
            val expected = SwipeActionsPreference(
                swipeLeft = swipeLeft,
                swipeRight = swipeRight
            )
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `defaults to action none when Swipe Right is null`() = runTest {
        // given
        val mailSettings = buildMailSettings(
            swipeLeft = SwipeAction.MarkRead,
            swipeRight = null
        )
        every { observeMailSettings(userId) } returns flowOf(mailSettings)

        // when
        observeSwipeActionsPreference(userId).test {

            // then
            val expected = SwipeActionsPreference(
                swipeLeft = SwipeAction.MarkRead,
                swipeRight = SwipeAction.None
            )
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }
}
