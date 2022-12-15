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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * This is a container for single-use state.
 * Use this when you don't want an event to be repeated, for example while emitting an error to the ViewModel
 *
 * You usually wanna consume this into a `LaunchedEffect` block
 */
class Effect<T : Any> private constructor(private var event: T?) {

    /**
     * @return the [event] if not consumed, `null` otherwise
     */
    fun consume(): T? = event
        .also { event = null }

    override fun equals(other: Any?): Boolean = other is Effect<*> && this.event == other.event

    override fun hashCode(): Int = event?.hashCode() ?: 0

    companion object {

        fun <T : Any> of(event: T) = Effect(event)
        fun <T : Any> empty() = Effect<T>(null)
    }
}

/**
 * Executes a [LaunchedEffect] in the scope of [effect]
 * @param block will be called only when there is an [Effect.event] to consume
 */
@Composable
inline fun <T : Any> ConsumableLaunchedEffect(
    effect: Effect<T>,
    crossinline block: suspend CoroutineScope.(T) -> Unit
) {
    LaunchedEffect(effect) {
        effect.consume()?.let { event ->
            block(event)
        }
    }
}

/**
 * Executes [block] in the scope of [effect]
 * @param block will be called only when there is an [Effect.event] to consume, resolving the [TextUiModel] to a
 *  [String]
 */
@Composable
inline fun ConsumableTextEffect(
    effect: Effect<TextUiModel>,
    crossinline block: suspend CoroutineScope.(String) -> Unit
) {
    val scope = rememberCoroutineScope()
    effect.consume()?.let { textUiModel ->
        val string = textUiModel.string()
        scope.launch { block(string) }
    }
}
