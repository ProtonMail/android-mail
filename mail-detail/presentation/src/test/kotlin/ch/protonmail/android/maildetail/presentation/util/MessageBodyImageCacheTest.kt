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

package ch.protonmail.android.maildetail.presentation.util

import ch.protonmail.android.mailmessage.domain.model.MessageBodyImage
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class MessageBodyImageCacheTest {

    private val cache = MessageBodyImageCache(maxSizeBytes = 80)

    @Test
    fun `given an image was cached, when it is requested, then it is returned`() {
        // Given
        val image = imageOfSize(10)
        cache.put(keyA, image, cache.generation)

        // When
        val result = cache.get(keyA)

        // Then
        assertEquals(image, result)
    }

    @Test
    fun `given the cache is full, when another image is cached, then the least recently used image is dropped`() {
        // Given
        val keys = (1..9).map { keyFor("proton-https://example.com/$it.png") }
        keys.take(8).forEach { cache.put(it, imageOfSize(10), cache.generation) }
        cache.get(keys.first())

        // When
        cache.put(keys.last(), imageOfSize(10), cache.generation)

        // Then
        assertNull(cache.get(keys[1]))
        assertEquals(imageOfSize(10), cache.get(keys.first()))
        assertEquals(imageOfSize(10), cache.get(keys.last()))
    }

    @Test
    fun `given an image is larger than an eighth of the cache, when it is cached, then it is not kept`() {
        // Given
        val largeImage = imageOfSize(11)

        // When
        cache.put(keyA, largeImage, cache.generation)

        // Then
        assertNull(cache.get(keyA))
    }

    @Test
    fun `given images were cached, when the cache is cleared, then no image is returned`() {
        // Given
        cache.put(keyA, imageOfSize(10), cache.generation)

        // When
        cache.clear()

        // Then
        assertNull(cache.get(keyA))
    }

    @Test
    fun `given a proxy failure was cached, when the cache is cleared, then it is no longer a failure`() {
        // Given
        cache.putProxyFailure(keyA, cache.generation)

        // When
        cache.clear()

        // Then
        assertFalse(cache.isProxyFailure(keyA))
    }

    @Test
    fun `given the cache was cleared while an image loaded, when the image is cached, then it is not kept`() {
        // Given
        val generationAtLoadStart = cache.generation
        cache.clear()

        // When
        cache.put(keyA, imageOfSize(10), generationAtLoadStart)

        // Then
        assertNull(cache.get(keyA))
    }

    @Test
    fun `given the cache was cleared while an image loaded, when its proxy failure is cached, then it is not kept`() {
        // Given
        val generationAtLoadStart = cache.generation
        cache.clear()

        // When
        cache.putProxyFailure(keyA, generationAtLoadStart)

        // Then
        assertFalse(cache.isProxyFailure(keyA))
    }

    @Test
    fun `given a remote image, when keys are built for two messages, then they are equal`() {
        // When
        val first = MessageBodyImageCache.Key.of(MessageId("1"), REMOTE_URL, shouldLoadImagesSafely = true)
        val second = MessageBodyImageCache.Key.of(MessageId("2"), REMOTE_URL, shouldLoadImagesSafely = true)

        // Then
        assertEquals(first, second)
    }

    @Test
    fun `given an embedded image, when keys are built for two messages, then they differ`() {
        // When
        val first = MessageBodyImageCache.Key.of(MessageId("1"), EMBEDDED_URL, shouldLoadImagesSafely = true)
        val second = MessageBodyImageCache.Key.of(MessageId("2"), EMBEDDED_URL, shouldLoadImagesSafely = true)

        // Then
        assertNotEquals(first, second)
    }

    private fun imageOfSize(size: Int) = MessageBodyImage(data = ByteArray(size), mimeType = "image/png")

    private companion object {

        const val REMOTE_URL = "proton-https://example.com/logo.png"
        const val EMBEDDED_URL = "cid:logo"

        val keyA = keyFor("proton-https://example.com/a.png")

        fun keyFor(url: String) = MessageBodyImageCache.Key.of(MessageId("1"), url, shouldLoadImagesSafely = true)
    }
}
