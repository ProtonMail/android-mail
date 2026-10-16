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

package ch.protonmail.android.maildetail.presentation.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.maildetail.presentation.util.MessageBodyImageCache
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsMessageBodyImageCacheEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailmessage.domain.model.AttachmentDataError
import ch.protonmail.android.mailmessage.domain.model.MessageBodyImage
import ch.protonmail.android.mailmessage.domain.usecase.LoadMessageBodyImage
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class LoadImageAvoidDuplicatedExecution @Inject constructor(
    private val loadMessageBodyImage: LoadMessageBodyImage,
    private val imageCache: MessageBodyImageCache,
    @IsMessageBodyImageCacheEnabled private val isImageCacheEnabled: FeatureFlag<Boolean>
) {

    // Keyed like the cache, so a load is only shared by requests that would cache its result under the same key.
    private val loadMessageBodyImageJobMap =
        mutableMapOf<MessageBodyImageCache.Key, Deferred<Either<AttachmentDataError, MessageBodyImage>>>()

    // Read once, so the cache stays on or off for the whole life of the screen.
    @Volatile
    private var isCacheEnabled: Boolean? = null

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        url: String,
        shouldLoadImagesSafely: Boolean,
        coroutineContext: CoroutineContext
    ): Either<AttachmentDataError, MessageBodyImage> = runCatching {
        val cacheKey = MessageBodyImageCache.Key.of(messageId, url, shouldLoadImagesSafely)
        val shouldUseCache = isCacheEnabled()
        // Read before the lookup: the cache can be cleared while the image loads, for example when the
        // conversation leaves the screen, and a load that started before a clear must not write after it.
        val cacheGeneration = imageCache.generation

        // Checked on the calling thread, so a cached image or failure doesn't wait for the main thread.
        if (shouldUseCache && imageCache.isProxyFailure(cacheKey)) {
            return@runCatching AttachmentDataError.ProxyFailed.left()
        }
        val cachedImage = if (shouldUseCache) imageCache.get(cacheKey) else null
        if (cachedImage != null) {
            return@runCatching cachedImage.right()
        }

        withContext(coroutineContext) {
            val job = loadMessageBodyImageJobMap[cacheKey]?.takeIf { it.isActive }
                ?: async { loadMessageBodyImage(userId, messageId, url, shouldLoadImagesSafely) }.also {
                    loadMessageBodyImageJobMap[cacheKey] = it
                }
            try {
                job.await()
            } finally {
                // A finished load is not shared any more, so drop it rather than keep every result.
                if (job.isCompleted && loadMessageBodyImageJobMap[cacheKey] === job) {
                    loadMessageBodyImageJobMap.remove(cacheKey)
                }
            }
        }.onRight { image ->
            if (shouldUseCache) imageCache.put(cacheKey, image, cacheGeneration)
        }.onLeft { error ->
            if (shouldUseCache && error is AttachmentDataError.ProxyFailed) {
                imageCache.putProxyFailure(cacheKey, cacheGeneration)
            }
        }
    }.getOrElse { AttachmentDataError.Other(DataError.Local.NotFound).left() }

    fun clearCache() {
        imageCache.clear()
    }

    private suspend fun isCacheEnabled(): Boolean =
        isCacheEnabled ?: isImageCacheEnabled.get().also { isCacheEnabled = it }
}
