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
import ch.protonmail.android.mailmessage.domain.usecase.GetEmbeddedImage
import ch.protonmail.android.mailmessage.domain.usecase.GetEmbeddedImageResult
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetEmbeddedImageAvoidDuplicatedExecutionTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice
    private val contentId = "contentId"
    private val getEmbeddedImage = mockk<GetEmbeddedImage>()

    private val getEmbeddedImageAvoidDuplicatedExecution = GetEmbeddedImageAvoidDuplicatedExecution(getEmbeddedImage)


    @Test
    fun `returns embedded image result when getting embedded image was successful`() = runTest {
        // Given
        val expectedByteArray = "I'm an embedded image".toByteArray()
        val expectedEmbeddedImageResult = GetEmbeddedImageResult(data = expectedByteArray, mimeType = "")
        coEvery {
            getEmbeddedImage(userId, messageId, contentId)
        } returns expectedEmbeddedImageResult.right()

        // When
        val result = getEmbeddedImageAvoidDuplicatedExecution(userId, messageId, contentId, coroutineContext)

        // Then
        assertEquals(expectedEmbeddedImageResult, result)
    }

    @Test
    fun `returns null when getting embedded image failed`() = runTest {
        // Given
        coEvery {
            getEmbeddedImage(userId, messageId, contentId)
        } returns DataError.Local.NoDataCached.left()

        // When
        val result = getEmbeddedImageAvoidDuplicatedExecution(userId, messageId, contentId, coroutineContext)

        // Then
        assertNull(result)
    }

    @Test
    fun `verify get embedded image is called only once while it is running`() = runTest {
        // Given
        val expectedByteArray = "I'm an embedded image".toByteArray()
        val expectedEmbeddedImageResult = GetEmbeddedImageResult(data = expectedByteArray, mimeType = "")
        coEvery {
            getEmbeddedImage(userId, messageId, contentId)
        } coAnswers {
            expectedEmbeddedImageResult.right()
        }

        // When
        launch { getEmbeddedImageAvoidDuplicatedExecution(userId, messageId, contentId, coroutineContext) }
        launch { getEmbeddedImageAvoidDuplicatedExecution(userId, messageId, contentId, coroutineContext) }
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { getEmbeddedImage(userId, messageId, contentId) }
    }
}
