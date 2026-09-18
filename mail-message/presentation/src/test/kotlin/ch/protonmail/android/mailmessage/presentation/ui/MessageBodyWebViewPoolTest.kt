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

package ch.protonmail.android.mailmessage.presentation.ui

import android.view.ViewGroup
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertSame

/**
 * Unit tests for [MessageBodyWebViewPool]: weight-budget retention, the LRU instance cap, renderer-death
 * recovery, and the memory-pressure trim.
 *
 * A ZoomableWebView is mocked; its `parent` decides attached (visible) vs detached (off-screen, evictable).
 */
class MessageBodyWebViewPoolTest {

    @AfterTest
    fun tearDown() = unmockkAll()

    private fun webView(attached: Boolean = false): ZoomableWebView {
        val webView = mockk<ZoomableWebView>(relaxUnitFun = true)
        every { webView.parent } returns if (attached) mockk<ViewGroup>(relaxUnitFun = true) else null
        return webView
    }

    // region evictByWeight - retention accounting

    @Test
    fun `given a nonfitting nearer body, when evicting, then cheaper farther bodies that still fit are kept`() {
        // Given
        val pool = MessageBodyWebViewPool(maxSize = 10)
        val a = webView(); val b = webView(); val c = webView(); val d = webView()
        pool.acquire("a") { a }; pool.acquire("b") { b }; pool.acquire("c") { c }; pool.acquire("d") { d }

        // When
        pool.evictByWeight(
            distanceByKey = mapOf("a" to 0, "b" to 100, "c" to 200, "d" to 300),
            weightByKey = mapOf("a" to 4, "b" to 8, "c" to 1, "d" to 1),
            budget = 8
        )

        // Then
        verify(exactly = 0) { a.destroy() }
        verify(exactly = 1) { b.destroy() }
        verify(exactly = 0) { c.destroy() }
        verify(exactly = 0) { d.destroy() }
    }

    @Test
    fun `given only detached bodies, when evicting, then attached-safe eviction keeps the nearest within budget`() {
        // Given
        val pool = MessageBodyWebViewPool(maxSize = 10)
        val near = webView(); val far = webView()
        pool.acquire("near") { near }; pool.acquire("far") { far }

        // When
        pool.evictByWeight(
            distanceByKey = mapOf("near" to 0, "far" to 100),
            weightByKey = mapOf("near" to 4, "far" to 4),
            budget = 4
        )

        // Then
        verify(exactly = 0) { near.destroy() }
        verify(exactly = 1) { far.destroy() }
    }

    @Test
    fun `given an attached body outside the keep set, when evicting, then it survives but its cost is unbudgeted`() {
        // Given
        val pool = MessageBodyWebViewPool(maxSize = 10)
        val near = webView(attached = false)
        val visibleHeavy = webView(attached = true)
        val offscreen = webView(attached = false)
        pool.acquire("near") { near }
        pool.acquire("visible") { visibleHeavy }
        pool.acquire("off") { offscreen }

        // When
        pool.evictByWeight(
            distanceByKey = mapOf("near" to 0, "visible" to 100, "off" to 200),
            weightByKey = mapOf("near" to 1, "visible" to 8, "off" to 8),
            budget = 1
        )

        // Then
        verify(exactly = 0) { near.destroy() }
        verify(exactly = 0) { visibleHeavy.destroy() }
        verify(exactly = 1) { offscreen.destroy() }
    }

    @Test
    fun `given the nearest body alone exceeds the budget, when evicting, then it is still kept`() {
        // Given
        val pool = MessageBodyWebViewPool(maxSize = 10)
        val nearest = webView()
        pool.acquire("nearest") { nearest }

        // When
        pool.evictByWeight(mapOf("nearest" to 0), mapOf("nearest" to 8), budget = 6)

        // Then
        verify(exactly = 0) { nearest.destroy() }
    }

    // endregion

    // region evictIfNeeded - the LRU / instance cap

    @Test
    fun `given detached entries over maxSize, when acquiring, then the oldest detached view is destroyed`() {
        // Given
        val pool = MessageBodyWebViewPool(maxSize = 2)
        val a = webView(); val b = webView(); val c = webView()

        // When
        pool.acquire("a") { a }; pool.acquire("b") { b }; pool.acquire("c") { c }

        // Then
        verify(exactly = 1) { a.destroy() }
        verify(exactly = 0) { b.destroy() }
        verify(exactly = 0) { c.destroy() }
    }

    @Test
    fun `given every over-limit entry is attached, when acquiring, then the maxSize cap does not evict`() {
        // Given
        val pool = MessageBodyWebViewPool(maxSize = 2)
        val a = webView(attached = true); val b = webView(attached = true); val c = webView(attached = true)

        // When
        pool.acquire("a") { a }; pool.acquire("b") { b }; pool.acquire("c") { c }

        // Then
        verify(exactly = 0) { a.destroy() }
        verify(exactly = 0) { b.destroy() }
        verify(exactly = 0) { c.destroy() }
    }

    @Test
    fun `given a pooled key, when acquiring it again, then the same instance is returned without recreating`() {
        // Given
        val pool = MessageBodyWebViewPool(maxSize = 10)
        val first = webView()

        // When
        val a1 = pool.acquire("a") { first }
        val a2 = pool.acquire("a") { webView() }

        // Then
        assertSame(first, a1)
        assertSame(first, a2)
    }

    // endregion

    // region onRenderProcessGone - renderer death recovery

    @Test
    fun `given several pooled bodies, when the renderer dies, then the whole pool is destroyed and emptied`() {
        // Given
        val pool = MessageBodyWebViewPool(maxSize = 10)
        val a = webView(attached = true); val b = webView(); val c = webView()
        pool.acquire("a") { a }; pool.acquire("b") { b }; pool.acquire("c") { c }

        // When
        pool.onRenderProcessGone(a)

        // Then
        verify(exactly = 1) { a.destroy() }
        verify(exactly = 1) { b.destroy() }
        verify(exactly = 1) { c.destroy() }
        val replacement = webView()
        assertSame(replacement, pool.acquire("a") { replacement })
    }

    @Test
    fun `given a stale second renderer-death callback, then a freshly acquired replacement survives`() {
        // Given
        val pool = MessageBodyWebViewPool(maxSize = 10)
        val a = webView(); val b = webView()
        pool.acquire("a") { a }; pool.acquire("b") { b }
        pool.onRenderProcessGone(a)
        val replacement = webView()
        pool.acquire("c") { replacement }

        // When
        pool.onRenderProcessGone(b)

        // Then
        verify(exactly = 0) { replacement.destroy() }
    }

    @Test
    fun `given the renderer death of one view, when its callback fires twice, then the view is destroyed only once`() {
        // Given
        val pool = MessageBodyWebViewPool(maxSize = 10)
        val a = webView()
        pool.acquire("a") { a }

        // When
        pool.onRenderProcessGone(a)
        pool.onRenderProcessGone(a)

        // Then
        verify(exactly = 1) { a.destroy() }
    }

    // endregion

    // region trimToFloor - memory-pressure backstop

    @Test
    fun `given more detached bodies than the floor, when trimming, then the oldest detached views are destroyed`() {
        // Given
        val pool = MessageBodyWebViewPool(maxSize = 10)
        val a = webView(); val b = webView(); val c = webView()
        pool.acquire("a") { a }; pool.acquire("b") { b }; pool.acquire("c") { c }

        // When
        pool.trimToFloor(1)

        // Then
        verify(exactly = 1) { a.destroy() }
        verify(exactly = 1) { b.destroy() }
        verify(exactly = 0) { c.destroy() }
    }

    @Test
    fun `given attached bodies below the floor, when trimming, then no visible view is destroyed`() {
        // Given
        val pool = MessageBodyWebViewPool(maxSize = 10)
        val v1 = webView(attached = true); val v2 = webView(attached = true)
        pool.acquire("v1") { v1 }; pool.acquire("v2") { v2 }

        // When
        pool.trimToFloor(0)

        // Then
        verify(exactly = 0) { v1.destroy() }
        verify(exactly = 0) { v2.destroy() }
    }

    // endregion
}
