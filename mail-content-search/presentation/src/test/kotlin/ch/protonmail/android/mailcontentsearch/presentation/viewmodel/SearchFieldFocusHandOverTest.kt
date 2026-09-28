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

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchFieldFocusHandOverTest {

    private val sut = SearchFieldFocusHandOver()

    @Test
    fun `should grant the auto focus only once`() {
        // Re-entering the bar on the way back from a message or from selection mode asks again, and
        // must not raise the keyboard over the results the user came back to read.
        assertTrue(sut.consumeAutoFocus())
        assertFalse(sut.consumeAutoFocus())
    }

    @Test
    fun `should not report a set-up return when the user never left for set-up`() {
        assertFalse(sut.consumeSetUpReturn())
    }

    @Test
    fun `should report the set-up return only once per trip`() {
        sut.leaveForSetUp()

        assertTrue(sut.consumeSetUpReturn())
        assertFalse(sut.consumeSetUpReturn())
    }

    @Test
    fun `should spend the auto focus when leaving for set-up`() {
        // The banner going away on the way out must not raise the keyboard over the set-up page.
        sut.leaveForSetUp()

        assertFalse(sut.consumeAutoFocus())
    }
}
