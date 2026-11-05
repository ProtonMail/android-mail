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

package ch.protonmail.android.mailbyoe.presentation.navigation

import ch.protonmail.android.mailbyoe.domain.model.ByoeSurface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class ByoeSurfaceArgumentTest {

    @Test
    fun `given each surface, when passed as a route argument, then it reads back as the same surface`() {
        ByoeSurface.entries.forEach { surface ->
            // When
            val decoded = ByoeSurfaceArgument.decode(ByoeSurfaceArgument.encode(surface))

            // Then
            assertEquals(surface, decoded)
        }
    }

    @Test
    fun `given a value that names no surface, when read, then there is no surface`() {
        assertNull(ByoeSurfaceArgument.decode("Onboarding"))
        assertNull(ByoeSurfaceArgument.decode(null))
    }
}
