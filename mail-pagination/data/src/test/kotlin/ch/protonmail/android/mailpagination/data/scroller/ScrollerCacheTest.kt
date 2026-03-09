/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailpagination.data.scroller

import org.junit.Assert.assertEquals
import org.junit.Test
import uniffi.mail_uniffi.MailScrollerError
import uniffi.mail_uniffi.ProtonError

data class ScrollerItem(val id: String)

private fun item(id: String) = ScrollerItem(id)
private fun items(vararg ids: String): List<ScrollerItem> = ids.map(::item)

private fun assertSnapshotIds(expected: List<String>, actual: List<ScrollerItem>) {
    assertEquals(expected, actual.map { it.id })
}

private val error = MailScrollerError.Other(ProtonError.Network)

class ScrollerCacheTest {

    @Test
    fun `append on empty adds items in order`() {
        // Given
        val cache = ScrollerCache<ScrollerItem>()

        // When
        val snap = cache.applyUpdate(
            ScrollerUpdate.Append(scrollerId = DefaultScrollerId, items = items("A1", "B1", "C1"))
        )

        // Then
        assertSnapshotIds(listOf("A1", "B1", "C1"), snap)
    }

    @Test
    fun `multiple appends preserve ordering`() {
        // Given
        val cache = ScrollerCache<ScrollerItem>()
        cache.applyUpdate(ScrollerUpdate.Append(scrollerId = DefaultScrollerId, items = items("A1", "B1")))

        // When
        val snap = cache.applyUpdate(ScrollerUpdate.Append(scrollerId = DefaultScrollerId, items = items("C1", "D1")))

        // Then
        assertSnapshotIds(listOf("A1", "B1", "C1", "D1"), snap)
    }

    @Test
    fun `ReplaceFrom within bounds replaces tail from idx`() {
        // Given
        val cache = ScrollerCache<ScrollerItem>()
        cache.applyUpdate(
            ScrollerUpdate.Append(scrollerId = DefaultScrollerId, items = items("A1", "B1", "C1", "D1"))
        )

        // When
        val snap = cache.applyUpdate(
            ScrollerUpdate.ReplaceFrom(scrollerId = DefaultScrollerId, idx = 2, items = items("E1", "F1"))
        )

        // Then
        assertSnapshotIds(listOf("A1", "B1", "E1", "F1"), snap)
    }

    @Test
    fun `ReplaceFrom at size appends`() {
        // Given
        val cache = ScrollerCache<ScrollerItem>()
        cache.applyUpdate(ScrollerUpdate.Append(scrollerId = DefaultScrollerId, items = items("A1", "B1")))

        // When
        val snap = cache.applyUpdate(
            ScrollerUpdate.ReplaceFrom(scrollerId = DefaultScrollerId, idx = 2, items = items("C1", "D1"))
        )

        // Then
        assertSnapshotIds(listOf("A1", "B1", "C1", "D1"), snap)
    }

    @Test
    fun `ReplaceFrom idx 0 replaces all with given items including empty`() {
        // Given
        val cache = ScrollerCache<ScrollerItem>()
        cache.applyUpdate(ScrollerUpdate.Append(scrollerId = DefaultScrollerId, items = items("A1", "B1", "C1")))

        // When
        val snap1 = cache.applyUpdate(
            ScrollerUpdate.ReplaceFrom(scrollerId = DefaultScrollerId, idx = 0, items = items("D1"))
        )
        val snap2 = cache.applyUpdate(
            ScrollerUpdate.ReplaceFrom(scrollerId = DefaultScrollerId, idx = 0, items = emptyList())
        )

        // Then
        assertSnapshotIds(listOf("D1"), snap1)
        assertSnapshotIds(emptyList(), snap2)
    }

    @Test
    fun `ReplaceFrom negative or greater than size is ignored`() {
        // Given
        val cache = ScrollerCache<ScrollerItem>()
        cache.applyUpdate(ScrollerUpdate.Append(scrollerId = DefaultScrollerId, items = items("A1", "B1", "C1")))

        // When
        cache.applyUpdate(
            ScrollerUpdate.ReplaceFrom(scrollerId = DefaultScrollerId, idx = -1, items = items("X1"))
        )
        cache.applyUpdate(
            ScrollerUpdate.ReplaceFrom(scrollerId = DefaultScrollerId, idx = 10, items = items("Y1"))
        )

        // Then
        assertSnapshotIds(listOf("A1", "B1", "C1"), cache.snapshot)
    }

    @Test
    fun `ReplaceBefore within bounds replaces head before idx`() {
        // Given
        val cache = ScrollerCache<ScrollerItem>()
        cache.applyUpdate(
            ScrollerUpdate.Append(scrollerId = DefaultScrollerId, items = items("A1", "B1", "C1", "D1"))
        )

        // When
        val snap = cache.applyUpdate(
            ScrollerUpdate.ReplaceBefore(scrollerId = DefaultScrollerId, idx = 2, items = items("E1", "F1"))
        )

        // Then
        assertSnapshotIds(listOf("E1", "F1", "C1", "D1"), snap)
    }

    @Test
    fun `ReplaceBefore at size replaces all`() {
        // Given
        val cache = ScrollerCache<ScrollerItem>()
        cache.applyUpdate(ScrollerUpdate.Append(scrollerId = DefaultScrollerId, items = items("A1", "B1", "C1")))

        // When
        val snap = cache.applyUpdate(
            ScrollerUpdate.ReplaceBefore(scrollerId = DefaultScrollerId, idx = 3, items = items("D1", "E1"))
        )

        // Then
        assertSnapshotIds(listOf("D1", "E1"), snap)
    }

    @Test
    fun `ReplaceBefore idx 0 prepends items`() {
        // Given
        val cache = ScrollerCache<ScrollerItem>()
        cache.applyUpdate(ScrollerUpdate.Append(scrollerId = DefaultScrollerId, items = items("C1", "D1")))

        // When
        val snap = cache.applyUpdate(
            ScrollerUpdate.ReplaceBefore(scrollerId = DefaultScrollerId, idx = 0, items = items("A1", "B1"))
        )

        // Then
        assertSnapshotIds(listOf("A1", "B1", "C1", "D1"), snap)
    }

    @Test
    fun `ReplaceBefore on empty with idx size (0) becomes replace all`() {
        // Given
        val cache = ScrollerCache<ScrollerItem>()

        // When
        val snap = cache.applyUpdate(
            ScrollerUpdate.ReplaceBefore(scrollerId = DefaultScrollerId, idx = 0, items = items("A1"))
        )

        // Then
        assertSnapshotIds(listOf("A1"), snap)
    }

    @Test
    fun `ReplaceBefore negative or greater than size is ignored`() {
        // Given
        val cache = ScrollerCache<ScrollerItem>()
        cache.applyUpdate(ScrollerUpdate.Append(scrollerId = DefaultScrollerId, items = items("A1", "B1", "C1")))

        // When
        cache.applyUpdate(
            ScrollerUpdate.ReplaceBefore(scrollerId = DefaultScrollerId, idx = -1, items = items("X1"))
        )
        cache.applyUpdate(
            ScrollerUpdate.ReplaceBefore(scrollerId = DefaultScrollerId, idx = 10, items = items("Y1"))
        )

        // Then
        assertSnapshotIds(listOf("A1", "B1", "C1"), cache.snapshot)
    }

    @Test
    fun `ReplaceRange replaces given items when they are in items bounds treating from inclusive, to exclusive`() {
        // Given
        val cache = ScrollerCache<ScrollerItem>()
        cache.applyUpdate(
            ScrollerUpdate.Append(scrollerId = DefaultScrollerId, items = items("A1", "B1", "C1", "D1"))
        )

        // When
        val snap = cache.applyUpdate(
            ScrollerUpdate.ReplaceRange(
                scrollerId = DefaultScrollerId, fromIdx = 1, toIdx = 3, items = items("B2", "C2")
            )
        )

        // Then
        assertSnapshotIds(listOf("A1", "B2", "C2", "D1"), snap)
    }

    @Test
    fun `ReplaceRange with negative from index is ignored`() {
        // Given
        val cache = ScrollerCache<ScrollerItem>()
        cache.applyUpdate(
            ScrollerUpdate.Append(scrollerId = DefaultScrollerId, items = items("A1", "B1", "C1", "D1"))
        )

        // When
        val snap = cache.applyUpdate(
            ScrollerUpdate.ReplaceRange(
                scrollerId = DefaultScrollerId, fromIdx = -1, toIdx = 2, items = items("B2", "C2")
            )
        )

        // Then
        assertSnapshotIds(listOf("A1", "B1", "C1", "D1"), snap)
    }

    @Test
    fun `ReplaceRange with to index greater than size is ignored`() {
        // Given
        val cache = ScrollerCache<ScrollerItem>()
        cache.applyUpdate(
            ScrollerUpdate.Append(scrollerId = DefaultScrollerId, items = items("A1", "B1", "C1", "D1"))
        )

        // When
        val snap = cache.applyUpdate(
            ScrollerUpdate.ReplaceRange(
                scrollerId = DefaultScrollerId, fromIdx = 0, toIdx = 5, items = items("B2", "C2")
            )
        )

        // Then
        assertSnapshotIds(listOf("A1", "B1", "C1", "D1"), snap)
    }

    @Test
    fun `ReplaceRange with from index greater than to index is ignored`() {
        // Given
        val cache = ScrollerCache<ScrollerItem>()
        cache.applyUpdate(
            ScrollerUpdate.Append(scrollerId = DefaultScrollerId, items = items("A1", "B1", "C1", "D1"))
        )

        // When
        val snap = cache.applyUpdate(
            ScrollerUpdate.ReplaceRange(
                scrollerId = DefaultScrollerId, fromIdx = 2, toIdx = 1, items = items("B2", "C2")
            )
        )

        // Then
        assertSnapshotIds(listOf("A1", "B1", "C1", "D1"), snap)
    }

    @Test
    fun `None is a no-op`() {
        // Given
        val cache = ScrollerCache<ScrollerItem>()
        cache.applyUpdate(ScrollerUpdate.Append(scrollerId = DefaultScrollerId, items = items("A1")))

        // When
        val snap = cache.applyUpdate(ScrollerUpdate.None(scrollerId = DefaultScrollerId))

        // Then
        assertSnapshotIds(listOf("A1"), snap)
    }

    @Test
    fun `Error is a no-op`() {
        // Given
        val cache = ScrollerCache<ScrollerItem>()
        cache.applyUpdate(ScrollerUpdate.Append(scrollerId = DefaultScrollerId, items = items("A1", "B1")))

        // When
        val snap = cache.applyUpdate(ScrollerUpdate.Error(scrollerId = DefaultScrollerId, error = error))

        // Then
        assertSnapshotIds(listOf("A1", "B1"), snap)
    }

    @Test
    fun `mixed sequence of updates behaves correctly`() {
        // Given
        val cache = ScrollerCache<ScrollerItem>()

        // When
        // 1) Start with A1,B1,C1
        cache.applyUpdate(ScrollerUpdate.Append(scrollerId = DefaultScrollerId, items = items("A1", "B1", "C1")))
        // 2) ReplaceFrom from idx=1 with D1,E1  => A1, D1, E1
        cache.applyUpdate(
            ScrollerUpdate.ReplaceFrom(scrollerId = DefaultScrollerId, idx = 1, items = items("D1", "E1"))
        )
        // 3) Prepend via ReplaceBefore idx=0 with B2 => B2, A1, D1, E1
        cache.applyUpdate(
            ScrollerUpdate.ReplaceBefore(scrollerId = DefaultScrollerId, idx = 0, items = items("B2"))
        )
        // 4) Append F2,G2 => B2, A1, D1, E1, F2, G2
        cache.applyUpdate(ScrollerUpdate.Append(scrollerId = DefaultScrollerId, items = items("F2", "G2")))
        // 5) ReplaceBefore idx=3 with H2 => H2, E1, F2, G2
        cache.applyUpdate(
            ScrollerUpdate.ReplaceBefore(scrollerId = DefaultScrollerId, idx = 3, items = items("H2"))
        )
        // 6) Error & None do nothing
        cache.applyUpdate(ScrollerUpdate.Error(scrollerId = DefaultScrollerId, error = error))
        cache.applyUpdate(ScrollerUpdate.None(scrollerId = DefaultScrollerId))
        // 7) ReplaceFrom idx=size appends I2 => H2,E1,F2,G2,I2
        cache.applyUpdate(
            ScrollerUpdate.ReplaceFrom(scrollerId = DefaultScrollerId, idx = cache.snapshot.size, items = items("I2"))
        )

        // Then
        assertSnapshotIds(listOf("H2", "E1", "F2", "G2", "I2"), cache.snapshot)
    }

    companion object {
        private const val DefaultScrollerId = "scroller-id"
    }
}
