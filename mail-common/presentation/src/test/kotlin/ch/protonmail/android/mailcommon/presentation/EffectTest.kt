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

package ch.protonmail.android.mailcommon.presentation

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull

internal class EffectTest {

    @Test
    fun `event is returned correctly on consume`() {
        // given
        val event = "hello"
        val effect = Effect.of(event)

        // when - then
        assertEquals(event, effect.consume())
    }

    @Test
    fun `event is cleared on consume`() {
        // given
        val effect = Effect.of("hello")

        // when
        effect.consume()

        // then
        assertNull(effect.consume())
    }

    @Test
    fun `state flow should not emit the same effect instance after it's consumed`() = runTest {
        // given
        val effect = Effect.of(42)
        val stateFlow = MutableStateFlow(effect)

        // when/then
        stateFlow.test {
            val initialState = awaitItem()
            assertEquals(effect, initialState)

            effect.consume()
            stateFlow.value = effect
            expectNoEvents()
        }
    }

    @Test
    fun `should compare the event value and not the instance when doing the equals check`() {
        // given
        val firstEffect = Effect.of(42)
        val secondEffect = Effect.of(42)

        // when/then
        assertEquals(firstEffect, secondEffect)
        assertNotSame(firstEffect, secondEffect)
    }
}
