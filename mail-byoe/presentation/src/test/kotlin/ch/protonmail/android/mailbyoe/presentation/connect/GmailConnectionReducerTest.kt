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

package ch.protonmail.android.mailbyoe.presentation.connect

import ch.protonmail.android.mailbyoe.domain.model.GmailConnectionResult
import ch.protonmail.android.mailbyoe.presentation.connect.GmailConnectionOperation.GmailConnectionAction
import ch.protonmail.android.mailbyoe.presentation.connect.GmailConnectionOperation.GmailConnectionEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class GmailConnectionReducerTest {

    private val reducer = GmailConnectionReducer()

    @Test
    fun `given the page is ready, when reduced, then the page is opened with its url`() {
        // When
        val state = reducer.newStateFrom(GmailConnectionState.Loading, GmailConnectionEvent.PageReady(URL))

        // Then
        assertIs<GmailConnectionState.OpeningPage>(state)
        assertEquals(URL, state.openPage.consume())
    }

    @Test
    fun `given the preparation failed, when reduced, then the state is failed`() {
        // When
        val state = reducer.newStateFrom(GmailConnectionState.Loading, GmailConnectionEvent.PreparationFailed)

        // Then
        assertEquals(GmailConnectionState.Failed, state)
    }

    @Test
    fun `given the connection failed, when reduced, then the state is failed`() {
        // When
        val state = reducer.newStateFrom(GmailConnectionState.Loading, GmailConnectionEvent.ConnectionFailed)

        // Then
        assertEquals(GmailConnectionState.Failed, state)
    }

    @Test
    fun `given the connection finished, when reduced, then the screen closes`() {
        // When
        val state = reducer.newStateFrom(GmailConnectionState.Loading, GmailConnectionEvent.ConnectionFinished)

        // Then
        assertIs<GmailConnectionState.Finished>(state)
        assertEquals(Unit, state.close.consume())
    }

    @Test
    fun `given a failure, when retrying, then the state is loading`() {
        // When
        val state = reducer.newStateFrom(GmailConnectionState.Failed, GmailConnectionAction.RetryClicked)

        // Then
        assertEquals(GmailConnectionState.Loading, state)
    }

    @Test
    fun `given the page is closed, when reduced, then the state is unchanged`() {
        // When
        val state = reducer.newStateFrom(
            GmailConnectionState.Loading,
            GmailConnectionAction.PageClosed(GmailConnectionResult.Connected())
        )

        // Then
        assertEquals(GmailConnectionState.Loading, state)
    }

    private companion object {

        const val URL = "https://account.proton.me/lite"
    }
}
