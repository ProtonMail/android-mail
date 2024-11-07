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

package ch.protonmail.android.maildetail.presentation.mapper

import android.content.Context
import android.content.res.Resources
import ch.protonmail.android.testdata.message.MessageTestData
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MessageBannersUiModelMapperTest {

    private val resourcesMock = mockk<Resources>()
    private val contextMock = mockk<Context> {
        every { resources } returns resourcesMock
    }

    private val messageBannersUiModelMapper = MessageBannersUiModelMapper(contextMock)

    @Test
    fun `should map to ui model that allows showing a phishing banner when message is auto marked as phishing`() {
        // When
        val result = messageBannersUiModelMapper.createMessageBannersUiModel(
            MessageTestData.autoPhishingMessage
        )

        // Then
        assertTrue(result.shouldShowPhishingBanner)
    }

    @Test
    fun `should map to ui model that doesn't allow showing a phishing banner when message isn't marked as phishing`() {
        // When
        val result = messageBannersUiModelMapper.createMessageBannersUiModel(
            MessageTestData.message
        )

        // Then
        assertFalse(result.shouldShowPhishingBanner)
    }

    @Test
    fun `should map to ui model with expiration banner if expiration is in the future`() {
        // Given
        expectResourceStrings()

        // When
        val result = messageBannersUiModelMapper.createMessageBannersUiModel(
            MessageTestData.expiringMessage
        )

        // Then
        assertNotNull(result.expirationBannerText)
    }

    @Test
    fun `should map to ui model with no expiration banner if message is auto-delete`() {
        // Given
        expectResourceStrings()

        // When
        val result = messageBannersUiModelMapper.createMessageBannersUiModel(
            MessageTestData.autoDeleteMessage
        )

        // Then
        assertNull(result.expirationBannerText)
    }

    @Test
    fun `should map to ui model with no expiration banner if expiration is in the past`() {
        // When
        val result = messageBannersUiModelMapper.createMessageBannersUiModel(
            MessageTestData.message
        )

        // Then
        assertNull(result.expirationBannerText)
    }

    @Test
    fun `should map to ui model with no auto-delete banner if expiration is in the past`() {
        // When
        val result = messageBannersUiModelMapper.createMessageBannersUiModel(
            MessageTestData.message
        )

        // Then
        assertNull(result.autoDeleteBannerText)
    }

    @Test
    fun `should map to ui model with no expiration banner if frozen flag is false`() {
        // Given
        expectResourceStrings()

        // When
        val result = messageBannersUiModelMapper.createMessageBannersUiModel(
            MessageTestData.autoDeleteMessage
        )

        // Then
        assertNull(result.expirationBannerText)
    }

    @Test
    fun `should map to ui model with auto-delete banner if frozen flag is false`() {
        // Given
        expectResourceStrings()

        // When
        val result = messageBannersUiModelMapper.createMessageBannersUiModel(
            MessageTestData.autoDeleteMessage
        )

        // Then
        assertNotNull(result.autoDeleteBannerText)
    }

    private fun expectResourceStrings() {
        every { resourcesMock.getQuantityString(any(), any(), any()) } returns "formatted duration"
        every { resourcesMock.getString(any(), any()) } returns "message expires in"
        every { resourcesMock.getString(any()) } returns "auto delete in"
    }
}
