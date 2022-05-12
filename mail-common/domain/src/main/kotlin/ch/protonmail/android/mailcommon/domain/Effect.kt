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

package ch.protonmail.android.mailcommon.domain

/**
 * This is a container for single-use state.
 * Use this when you don't want an event to be repeated, for example while emitting an error to the ViewModel
 *
 * You usually wanna consume this into a `LaunchedEffect` block
 */
class Effect<T>(private var event: T?) {

    /**
     * @return the [event] if not consumed, `null` otherwise
     */
    fun consume(): T? = event
        .also { event = null }

    companion object {

        fun <T> empty() = Effect<T>(null)
    }
}
