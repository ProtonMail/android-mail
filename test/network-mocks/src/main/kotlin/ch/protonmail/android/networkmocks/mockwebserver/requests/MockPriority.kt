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

package ch.protonmail.android.networkmocks.mockwebserver.requests

import ch.protonmail.android.networkmocks.mockwebserver.MockNetworkDispatcher

/**
 * A sealed class defining priorities used by a [MockNetworkDispatcher] to define the order that needs to be
 * respected when serving network requests.
 *
 * This is useful, for instance, when some API calls need to be consistently matched N times with wildcards,
 * but at some point one specific path needs to be matched differently while keeping the existing definitions in place.
 */
sealed class MockPriority(val value: Int) {

    object Lowest : MockPriority(value = Int.MIN_VALUE)
    object Highest : MockPriority(value = Int.MAX_VALUE)
    class Custom(value: Int) : MockPriority(value)
}
