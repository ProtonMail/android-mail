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
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for [MessageBodyWebViewPool]'s content-signature bookkeeping (the reuse-skip guard), the
 * attached/detached ownership model that decides which entries are evictable, and the diagnostic snapshot.
 *
 * A ZoomableWebView is mocked; its `parent` decides attached (visible) vs detached (off-screen, evictable).
 */
class MessageBodyWebViewPoolSignatureTest {

    @AfterTest
    fun tearDown() = unmockkAll()

    private fun webView(attached: Boolean = false): ZoomableWebView {
        val webView = mockk<ZoomableWebView>(relaxUnitFun = true)
        every { webView.parent } returns if (attached) mockk<ViewGroup>(relaxUnitFun = true) else null
        return webView
    }

    // region content signature

    @Test
    fun `given a freshly acquired key, then its loaded signature is null`() {
        // Given
        val pool = MessageBodyWebViewPool(maxSize = 10)

        // When
        pool.acquire("a") { webView() }

        // Then
        assertNull(pool.loadedSignature("a"))
    }

    @Test
    fun `given a key marked loaded, then its loaded signature is returned`() {
        // Given
        val pool = MessageBodyWebViewPool(maxSize = 10)
        pool.acquire("a") { webView() }

        // When
        pool.markLoaded("a", "sig-A")

        // Then
        assertEquals("sig-A", pool.loadedSignature("a"))
    }

    @Test
    fun `given a loaded key re-acquired, then the signature survives so reuse can skip the reload`() {
        // Given
        val pool = MessageBodyWebViewPool(maxSize = 10)
        pool.acquire("a") { webView() }
        pool.markLoaded("a", "sig-A")

        // When
        pool.acquire("a") { webView() }

        // Then
        assertEquals("sig-A", pool.loadedSignature("a"))
    }

    @Test
    fun `given a key destroyed by renderer death and re-acquired, then its signature is reset to null`() {
        // Given
        val pool = MessageBodyWebViewPool(maxSize = 10)
        val first = webView()
        pool.acquire("a") { first }
        pool.markLoaded("a", "sig-A")

        // When
        pool.onRenderProcessGone(first)
        pool.acquire("a") { webView() }

        // Then
        assertNull(pool.loadedSignature("a"))
    }

    @Test
    fun `given markLoaded for an absent key, then it is a no-op and the signature stays null`() {
        // Given
        val pool = MessageBodyWebViewPool(maxSize = 10)

        // When
        pool.markLoaded("missing", "sig-A")

        // Then
        assertNull(pool.loadedSignature("missing"))
    }

    // endregion

    // region ownership model

    @Test
    fun `given a just-acquired detached body ranked far, when evicting, then it is destroyed (no lease guard)`() {
        // Given
        val pool = MessageBodyWebViewPool(maxSize = 10)
        val near = webView()
        val justAcquiredFar = webView()
        pool.acquire("near") { near }
        pool.acquire("far") { justAcquiredFar }

        // When
        pool.evictByWeight(
            distanceByKey = mapOf("near" to 0, "far" to 500),
            weightByKey = mapOf("near" to 1, "far" to 8),
            budget = 1
        )

        // Then
        verify(exactly = 0) { near.destroy() }
        verify(exactly = 1) { justAcquiredFar.destroy() }
    }

    @Test
    fun `given attached and detached bodies, then snapshot reports the split and the cap`() {
        // Given
        val pool = MessageBodyWebViewPool(maxSize = 12)
        pool.acquire("a") { webView(attached = true) }
        pool.acquire("b") { webView() }
        pool.acquire("c") { webView() }

        // When
        val snapshot = pool.snapshot()

        // Then
        assertEquals(
            MessageBodyWebViewPool.Snapshot(size = 3, attached = 1, detached = 2, maxSize = 12),
            snapshot
        )
    }

    @Test
    fun `given every message collapsed, when evicting, then the whole pool drains to zero`() {
        // Given
        val pool = MessageBodyWebViewPool(maxSize = 10)
        val a = webView()
        val b = webView()
        pool.acquire("a") { a }
        pool.acquire("b") { b }

        // When
        pool.evictByWeight(distanceByKey = emptyMap(), weightByKey = emptyMap(), budget = 8)

        // Then
        verify(exactly = 1) { a.destroy() }
        verify(exactly = 1) { b.destroy() }
    }

    // endregion
}
