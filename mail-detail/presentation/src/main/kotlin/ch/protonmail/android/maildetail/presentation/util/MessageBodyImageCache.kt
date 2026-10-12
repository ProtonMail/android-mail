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
import javax.inject.Inject

/**
 * In-memory LRU cache of loaded message body images, bounded by the total size of the image bytes. It also
 * remembers which images the image proxy failed to load, so reloading a body doesn't wait on the proxy again.
 *
 * Unscoped: each screen's ViewModel gets its own instance, so a conversation's images are only kept while that
 * conversation is shown. Thread safe, as WebViews request images from several threads at once.
 */
class MessageBodyImageCache(private val maxSizeBytes: Long) {

    // Access order, so iteration starts at the least recently used entry.
    private val entries = LinkedHashMap<Key, MessageBodyImage>(INITIAL_CAPACITY, LOAD_FACTOR, true)
    private var sizeBytes = 0L
    private val proxyFailures = mutableSetOf<Key>()

    @Inject
    constructor() : this(defaultMaxSizeBytes())

    @Synchronized
    fun get(key: Key): MessageBodyImage? = entries[key]

    @Synchronized
    fun put(key: Key, image: MessageBodyImage) {
        val imageSize = image.data.size.toLong()
        // A single large image would flush most of the cache, so it is not kept.
        if (imageSize > maxSizeBytes / MAX_ENTRY_FRACTION) return

        entries.put(key, image)?.let { sizeBytes -= it.data.size }
        sizeBytes += imageSize
        trimToSize()
    }

    @Synchronized
    fun isProxyFailure(key: Key): Boolean = key in proxyFailures

    @Synchronized
    fun putProxyFailure(key: Key) {
        if (proxyFailures.size < MAX_PROXY_FAILURES) proxyFailures.add(key)
    }

    @Synchronized
    fun clear() {
        entries.clear()
        sizeBytes = 0
        proxyFailures.clear()
    }

    private fun trimToSize() {
        val iterator = entries.values.iterator()
        while (sizeBytes > maxSizeBytes && iterator.hasNext()) {
            sizeBytes -= iterator.next().data.size
            iterator.remove()
        }
    }

    /**
     * Remote images are shared across the messages of a conversation, so they are keyed by URL only.
     * Embedded (cid:) URLs only identify an image within one message, so those are keyed by message as well.
     */
    data class Key(
        val messageId: MessageId?,
        val url: String,
        val shouldLoadImagesSafely: Boolean
    ) {

        companion object {

            fun of(
                messageId: MessageId,
                url: String,
                shouldLoadImagesSafely: Boolean
            ) = Key(
                messageId = if (url.isRemoteImageUrl()) null else messageId,
                url = url,
                shouldLoadImagesSafely = shouldLoadImagesSafely
            )

            private fun String.isRemoteImageUrl() = startsWith(REMOTE_URL_PREFIX)
        }
    }

    private companion object {

        const val INITIAL_CAPACITY = 64
        const val LOAD_FACTOR = 0.75f
        const val MAX_ENTRY_FRACTION = 8
        const val MAX_PROXY_FAILURES = 512
        const val MAX_HEAP_FRACTION = 16
        const val MIN_SIZE_BYTES = 4L * 1024 * 1024
        const val MAX_SIZE_BYTES = 32L * 1024 * 1024
        const val REMOTE_URL_PREFIX = "proton-http"

        fun defaultMaxSizeBytes() =
            (Runtime.getRuntime().maxMemory() / MAX_HEAP_FRACTION).coerceIn(MIN_SIZE_BYTES, MAX_SIZE_BYTES)
    }
}
