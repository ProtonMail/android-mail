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

package ch.protonmail.android.mailupselling.presentation.usecase

import ch.protonmail.android.mailupselling.presentation.model.UpsellContentTheme
import io.mockk.every
import io.mockk.mockk
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Clock
import kotlin.time.Instant

internal class ResolveActiveUpsellThemeTest {

    private val clock = mockk<Clock>()
    private val resolveActiveUpsellTheme = ResolveActiveUpsellTheme(clock)
    private val userId = UserId("user-1")

    private fun atDay(day: Long) {
        every { clock.now() } returns Instant.fromEpochMilliseconds(day * MILLIS_PER_DAY)
    }

    @Test
    fun `keeps the same theme within a 14 day window and changes after it`() {
        atDay(0L)
        val first = resolveActiveUpsellTheme(userId)

        atDay(13L)
        assertEquals(first, resolveActiveUpsellTheme(userId))

        atDay(14L)
        assertNotEquals(first, resolveActiveUpsellTheme(userId))
    }

    @Test
    fun `shows every theme once before the selection repeats`() {
        val themesInCycle = UpsellContentTheme.entries.indices.map { window ->
            atDay(window * ROTATION_DAYS)
            resolveActiveUpsellTheme(userId)
        }

        assertEquals(UpsellContentTheme.entries.toSet(), themesInCycle.toSet())
    }

    private companion object {

        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
        const val ROTATION_DAYS = 14L
    }
}
