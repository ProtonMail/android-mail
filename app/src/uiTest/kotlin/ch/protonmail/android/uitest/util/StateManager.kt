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

package ch.protonmail.android.uitest.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import kotlinx.coroutines.flow.MutableStateFlow

class StateManager<State>(private val states: NonEmptyList<State>) {

    private var index = 0
    val flow: MutableStateFlow<State> = MutableStateFlow(states[index])

    fun emit(state: State) {
        flow.value = state
    }

    fun emitNext() {
        if (index == states.lastIndex) {
            throw IllegalStateException("No more states to emit")
        }
        flow.value = states[++index]
    }

    companion object {

        fun <State> of(initialState: State, vararg nextStates: State): StateManager<State> =
            StateManager(nonEmptyListOf(initialState, *nextStates))

        fun <State> of(states: NonEmptyList<State>): StateManager<State> =
            StateManager(states = states)
    }
}

interface ManagedStateScope<State> {

    fun emitState(state: State)

    fun emitNextState()
}

@Composable
fun <State> ManagedState(
    stateManager: StateManager<State>,
    content: @Composable ManagedStateScope<State>.(state: State) -> Unit
) {
    val scope = object : ManagedStateScope<State> {
        override fun emitState(state: State) {
            stateManager.emit(state)
        }

        override fun emitNextState() {
            stateManager.emitNext()
        }
    }
    val state by stateManager.flow.collectAsState()
    scope.content(state)
}

fun <State> ComposeContentTestRule.setManagedStateContent(
    stateManager: StateManager<State>,
    content: @Composable ManagedStateScope<State>.(state: State) -> Unit
) {
    setContent {
        ManagedState(stateManager, content)
    }
}
