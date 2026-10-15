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

package ch.protonmail.android.mailcontentsearch.presentation.viewmodel

import app.cash.turbine.test
import ch.protonmail.android.mailcontentsearch.presentation.model.ResultsTotalUiState
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxLoadingBarStateController
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.LoadingBarUiState
import ch.protonmail.android.mailpagination.domain.model.PaginatorTotal
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ResultsTotalControllerTest {

    private val cycleMs = MailboxLoadingBarStateController.DEFAULT_CYCLE_MS

    private fun TestScope.controller() = ResultsTotalController(
        loadingBar = MailboxLoadingBarStateController(backgroundScope),
        currentTimeMs = { testScheduler.currentTime }
    )

    @Test
    fun `should show the loading bar and no number while the total is pending`() = runTest {
        val sut = controller()

        sut.state.test {
            assertEquals(ResultsTotalUiState.Initial, awaitItem())

            sut.onTotal(PaginatorTotal.Pending)

            val pending = awaitItem()
            assertIs<LoadingBarUiState.Show>(pending.loadingBar)
            assertNull(pending.shownCount)
        }
    }

    @Test
    fun `should show the known total only once the loading bar has hidden`() = runTest {
        val sut = controller()

        sut.state.test {
            skipItems(1)
            sut.onTotal(PaginatorTotal.Pending)
            skipItems(1)

            sut.onTotal(PaginatorTotal.Known(14_314))

            // The number is stored, but the header keeps saying the search runs until the bar hides.
            val known = awaitItem()
            assertEquals(14_314, known.count)
            assertNull(known.shownCount)

            advanceTimeBy(cycleMs + 1)
            assertEquals(14_314, awaitItem().shownCount)
        }
    }

    @Test
    fun `should show a known total with no pending one before it straight away`() = runTest {
        val sut = controller()

        sut.state.test {
            skipItems(1)

            sut.onTotal(PaginatorTotal.Known(1_036))

            assertEquals(ResultsTotalUiState(loadingBar = LoadingBarUiState.Hide, count = 1_036), awaitItem())
        }
    }

    @Test
    fun `should hide the shown total again when a new remote search starts`() = runTest {
        val sut = controller()

        sut.state.test {
            skipItems(1)
            sut.onTotal(PaginatorTotal.Known(1_036))
            skipItems(1)

            // Toggling the include filter runs the remote search again.
            sut.onTotal(PaginatorTotal.Pending)

            assertNull(awaitItem().shownCount)
        }
    }

    @Test
    fun `should forget the total on reset`() = runTest {
        val sut = controller()

        sut.state.test {
            skipItems(1)
            sut.onTotal(PaginatorTotal.Known(300))
            skipItems(1)

            sut.reset()

            assertEquals(ResultsTotalUiState.Initial, awaitItem())
        }
    }
}
