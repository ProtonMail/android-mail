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

package ch.protonmail.android.mailcomposer.presentation.mapper.modifications

import ch.protonmail.android.mailcomposer.presentation.model.ComposerState
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.AccessoriesStateModification
import io.mockk.mockk
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@RunWith(Parameterized::class)
internal class AccessoriesStateModificationTest(
    @Suppress("unused") private val testName: String,
    private val initialState: ComposerState.Accessories,
    private val modification: AccessoriesStateModification,
    private val expectedState: ComposerState.Accessories
) {

    @Test
    fun `should apply the modification`() {
        val updatedState = modification.apply(initialState)
        assertEquals(expectedState, updatedState)
    }

    companion object {

        private val initialState = ComposerState.Accessories.initial()

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(
                "set the password from initial state",
                initialState,
                AccessoriesStateModification.MessagePasswordUpdated(mockk()),
                initialState.copy(isMessagePasswordSet = true)
            ),
            arrayOf(
                "remove the password when already set",
                initialState.copy(isMessagePasswordSet = true),
                AccessoriesStateModification.MessagePasswordUpdated(null),
                initialState.copy(isMessagePasswordSet = false)
            ),
            arrayOf(
                "set the expiration from initial state",
                initialState,
                AccessoriesStateModification.MessageExpirationUpdated(1.hours),
                initialState.copy(messageExpiresIn = 1.hours)
            ),
            arrayOf(
                "update the expiration from an existing value",
                initialState.copy(messageExpiresIn = 30.minutes),
                AccessoriesStateModification.MessageExpirationUpdated(1.days),
                initialState.copy(messageExpiresIn = 1.days)
            )
        )
    }
}
