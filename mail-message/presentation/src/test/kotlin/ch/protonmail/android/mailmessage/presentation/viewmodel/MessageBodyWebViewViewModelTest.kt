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

package ch.protonmail.android.mailmessage.presentation.viewmodel

import android.net.Uri
import app.cash.turbine.test
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailmessage.presentation.model.webview.MessageBodyWebViewOperation
import ch.protonmail.android.mailmessage.presentation.model.webview.MessageBodyWebViewState
import ch.protonmail.android.mailmessage.presentation.reducer.MessageBodyWebViewReducer
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class MessageBodyWebViewViewModelTest {

    private val mockUri = mockk<Uri>()
    private val reducer = spyk<MessageBodyWebViewReducer>()
    private val viewModel = MessageBodyWebViewViewModel(reducer)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic(Uri::class)
    }

    @Test
    fun `should emit the initial state`() = runTest {
        // When + Then
        viewModel.state.test {
            assertEquals(MessageBodyWebViewState.Initial, awaitItem())
        }
    }

    @Test
    fun `emits correct state when user long clicks a link`() = runTest {
        // Given
        val action = MessageBodyWebViewOperation.MessageBodyWebViewAction.LongClickLink(mockUri)
        val expectedEvent = MessageBodyWebViewOperation.MessageBodyWebViewEvent.LinkLongClicked(mockUri)
        val expectedState = MessageBodyWebViewState.Initial.copy(
            lastFocusedUri = mockUri,
            longClickLinkEffect = Effect.of(Unit)
        )

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(action)

            assertEquals(expectedState, awaitItem())
            verify {
                reducer.newStateFrom(MessageBodyWebViewState.Initial, expectedEvent)
            }
        }
    }
}
