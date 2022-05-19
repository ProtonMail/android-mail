/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailsettings.domain.usecase

import app.cash.turbine.test
import ch.protonmail.android.mailsettings.domain.model.SwipeActionsPreference
import ch.protonmail.android.testdata.mailsettings.MailSettingsTestData.buildMailSettings
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
        every { observeMailSettings() } returns flowOf(mailSettings)

        // when
        observeSwipeActionsPreference().test {

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
        every { observeMailSettings() } returns flowOf(null)

        // when
        observeSwipeActionsPreference().test {

            // then
            awaitComplete()
        }
    }

    @Test
    fun `throws exception when Swipe Left is null`() = runTest {
        // given
        val mailSettings = buildMailSettings(
            swipeLeft = null,
            swipeRight = SwipeAction.Archive
        )
        every { observeMailSettings() } returns flowOf(mailSettings)

        // when
        observeSwipeActionsPreference().test {

            // then
            val expectedMessage = "Swipe Left is null"
            val actual = awaitError()
            assert(actual is IllegalStateException)
            assertEquals(expectedMessage, actual.message)
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
        every { observeMailSettings() } returns flowOf(null, mailSettings)

        // when
        observeSwipeActionsPreference().test {

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
    fun `throws exception when Swipe Right is null`() = runTest {
        // given
        val mailSettings = buildMailSettings(
            swipeLeft = SwipeAction.MarkRead,
            swipeRight = null
        )
        every { observeMailSettings() } returns flowOf(mailSettings)

        // when
        observeSwipeActionsPreference().test {

            // then
            val expectedMessage = "Swipe Right is null"
            val actual = awaitError()
            assert(actual is IllegalStateException)
            assertEquals(expectedMessage, actual.message)
        }
    }
}
