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

package ch.protonmail.android.mailmessage.presentation.reducer

import android.net.Uri
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailmessage.presentation.model.webview.MessageBodyWebViewOperation
import ch.protonmail.android.mailmessage.presentation.model.webview.MessageBodyWebViewState
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class MessageBodyWebViewReducerTest {

    private val reducer = MessageBodyWebViewReducer()
    private val mockUri = mockk<Uri>()

    @Before
    fun setup() {
        mockkStatic(Uri::class)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should reduce the correct state`() {
        // Given
        val initialState = MessageBodyWebViewState.Initial
        val operation = MessageBodyWebViewOperation.MessageBodyWebViewEvent.LinkLongClicked(mockUri)
        val finalState = MessageBodyWebViewState(lastFocusedUri = mockUri, longClickLinkEffect = Effect.of(Unit))

        // When
        val actual = reducer.newStateFrom(initialState, operation)

        // Then
        assertEquals(finalState, actual)
    }
}
