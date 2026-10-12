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

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maildetail.presentation.util.MessageBodyImageCache
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailmessage.domain.model.AttachmentDataError
import ch.protonmail.android.mailmessage.domain.model.MessageBodyImage
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.usecase.LoadMessageBodyImage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LoadImageAvoidDuplicatedExecutionTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice
    private val url = "url"
    private val loadMessageBodyImage = mockk<LoadMessageBodyImage>()

    private val isImageCacheEnabled = mockk<FeatureFlag<Boolean>> {
        coEvery { this@mockk.get() } returns true
    }

    private val imageCache = MessageBodyImageCache(maxSizeBytes = 1024 * 1024)

    private val loadImageAvoidDuplicatedExecution = buildLoadImageAvoidDuplicatedExecution(imageCache)

    @Test
    fun `returns image result when loading image was successful`() = runTest {
        // Given
        val expectedByteArray = "I'm an image".toByteArray()
        val expectedImageResult = MessageBodyImage(data = expectedByteArray, mimeType = "").right()
        coEvery {
            loadMessageBodyImage(userId, messageId, url, shouldLoadImagesSafely = true)
        } returns expectedImageResult

        // When
        val result = loadImageAvoidDuplicatedExecution(
            userId = userId,
            messageId = messageId,
            url = url,
            shouldLoadImagesSafely = true,
            coroutineContext = coroutineContext
        )

        // Then
        assertEquals(expectedImageResult, result)
    }

    @Test
    fun `returns error when loading image failed`() = runTest {
        // Given
        val expectedResult = AttachmentDataError.Other(DataError.Local.NoDataCached).left()
        coEvery {
            loadMessageBodyImage(userId, messageId, url, shouldLoadImagesSafely = true)
        } returns expectedResult

        // When
        val result = loadImageAvoidDuplicatedExecution(
            userId = userId,
            messageId = messageId,
            url = url,
            shouldLoadImagesSafely = true,
            coroutineContext = coroutineContext
        )

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `verify load image is called only once while it is running`() = runTest {
        // Given
        val expectedByteArray = "I'm an image".toByteArray()
        val expectedImageResult = MessageBodyImage(data = expectedByteArray, mimeType = "")
        coEvery {
            loadMessageBodyImage(userId, messageId, url, shouldLoadImagesSafely = true)
        } coAnswers {
            expectedImageResult.right()
        }

        // When
        launch {
            loadImageAvoidDuplicatedExecution(userId, messageId, url, shouldLoadImagesSafely = true, coroutineContext)
        }
        launch {
            loadImageAvoidDuplicatedExecution(userId, messageId, url, shouldLoadImagesSafely = true, coroutineContext)
        }
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { loadMessageBodyImage(userId, messageId, url, shouldLoadImagesSafely = true) }
    }

    @Test
    fun `given an image was loaded, when it is requested again, then it is served from the cache`() = runTest {
        // Given
        val image = MessageBodyImage(data = "I'm an image".toByteArray(), mimeType = "image/png")
        coEvery { loadMessageBodyImage(userId, messageId, url, shouldLoadImagesSafely = true) } returns image.right()
        loadImage()

        // When
        val result = loadImage()

        // Then
        assertEquals(image.right(), result)
        coVerify(exactly = 1) { loadMessageBodyImage(userId, messageId, url, shouldLoadImagesSafely = true) }
    }

    @Test
    fun `given the image proxy failed to load an image, when it is requested again, then the failure is returned`() =
        runTest {
            // Given
            coEvery {
                loadMessageBodyImage(userId, messageId, url, shouldLoadImagesSafely = true)
            } returns AttachmentDataError.ProxyFailed.left()
            loadImage()

            // When
            val result = loadImage()

            // Then
            assertEquals(AttachmentDataError.ProxyFailed.left(), result)
            coVerify(exactly = 1) { loadMessageBodyImage(userId, messageId, url, shouldLoadImagesSafely = true) }
        }

    @Test
    fun `given an image failed to load for another reason, when it is requested again, then it is loaded again`() =
        runTest {
            // Given
            coEvery {
                loadMessageBodyImage(userId, messageId, url, shouldLoadImagesSafely = true)
            } returns AttachmentDataError.Other(DataError.Local.NoDataCached).left()
            loadImage()

            // When
            loadImage()

            // Then
            coVerify(exactly = 2) { loadMessageBodyImage(userId, messageId, url, shouldLoadImagesSafely = true) }
        }

    @Test
    fun `given the image proxy failed to load an image, when it is requested without the proxy, then it is loaded`() =
        runTest {
            // Given
            coEvery {
                loadMessageBodyImage(userId, messageId, url, shouldLoadImagesSafely = true)
            } returns AttachmentDataError.ProxyFailed.left()
            val image = MessageBodyImage(data = "I'm an image".toByteArray(), mimeType = "image/png")
            coEvery {
                loadMessageBodyImage(userId, messageId, url, shouldLoadImagesSafely = false)
            } returns image.right()
            loadImage()

            // When
            val result = loadImageAvoidDuplicatedExecution(
                userId, messageId, url, shouldLoadImagesSafely = false, coroutineContext = coroutineContext
            )

            // Then
            assertEquals(image.right(), result)
        }

    @Test
    fun `given the cache is disabled, when an image is requested again, then it is loaded again`() = runTest {
        // Given
        coEvery { isImageCacheEnabled.get() } returns false
        val image = MessageBodyImage(data = "I'm an image".toByteArray(), mimeType = "image/png")
        coEvery { loadMessageBodyImage(userId, messageId, url, shouldLoadImagesSafely = true) } returns image.right()
        val loadImageWithoutCache = buildLoadImageAvoidDuplicatedExecution()
        loadImage(loadImageWithoutCache)

        // When
        loadImage(loadImageWithoutCache)

        // Then
        coVerify(exactly = 2) { loadMessageBodyImage(userId, messageId, url, shouldLoadImagesSafely = true) }
    }

    @Test
    fun `given the cache was cleared, when an image is requested again, then it is loaded again`() = runTest {
        // Given
        val image = MessageBodyImage(data = "I'm an image".toByteArray(), mimeType = "image/png")
        coEvery { loadMessageBodyImage(userId, messageId, url, shouldLoadImagesSafely = true) } returns image.right()
        loadImage()
        loadImageAvoidDuplicatedExecution.clearCache()

        // When
        loadImage()

        // Then
        coVerify(exactly = 2) { loadMessageBodyImage(userId, messageId, url, shouldLoadImagesSafely = true) }
    }

    private suspend fun TestScope.loadImage(
        useCase: LoadImageAvoidDuplicatedExecution = loadImageAvoidDuplicatedExecution
    ) = useCase(userId, messageId, url, shouldLoadImagesSafely = true, coroutineContext = coroutineContext)

    private fun buildLoadImageAvoidDuplicatedExecution(
        cache: MessageBodyImageCache = MessageBodyImageCache(maxSizeBytes = 1024 * 1024)
    ) = LoadImageAvoidDuplicatedExecution(
        loadMessageBodyImage = loadMessageBodyImage,
        imageCache = cache,
        isImageCacheEnabled = isImageCacheEnabled
    )
}
