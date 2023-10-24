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

package ch.protonmail.android.test.idlingresources

import java.util.concurrent.atomic.AtomicInteger
import timber.log.Timber
import javax.inject.Inject

/**
 * An Idling Resource that uses an [AtomicInteger] under the hood to determine the idle state.
 *
 * In release builds, the provided class is a no-op.
 */
open class AtomicIntegerIdlingResource @Inject constructor() : ComposeIdlingResource {

    private var atomicIntegerCounter = AtomicInteger(StartingValue)

    override val isIdleNow: Boolean
        get() = atomicIntegerCounter.get() == 0

    override fun clear() = atomicIntegerCounter.set(StartingValue)

    fun increment() {
        atomicIntegerCounter.getAndIncrement()
    }

    fun decrement() {
        if (atomicIntegerCounter.get() <= 0) {
            Timber.w("Invalid state: idle resource count is not greater than 0.")
            return
        }

        atomicIntegerCounter.getAndDecrement()
    }

    private companion object {

        const val StartingValue = 0
    }
}
