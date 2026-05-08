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

package ch.protonmail.android.maildetail.presentation.mapper

import java.io.File
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import ch.protonmail.android.maildetail.presentation.mapper.MessageBodyContentUiModelMapper.Companion.LARGE_BODY_THRESHOLD_CHARS
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyContent
import ch.protonmail.android.mailmessage.presentation.ui.WEB_VIEW_SAFE_PHYSICAL_MAX_HEIGHT_PX
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MessageBodyContentUiModelMapperTest {

    private val context = mockk<Context>(relaxed = true)
    private val cacheDir = File("build/tmp/test/message-body-cache").apply {
        deleteRecursively()
        mkdirs()
    }
    private lateinit var mapper: MessageBodyContentUiModelMapper

    @Before
    fun setUp() {
        every { context.cacheDir } returns cacheDir
        every { context.packageName } returns PackageName

        mockkStatic(FileProvider::class)

        mapper = MessageBodyContentUiModelMapper(
            context = context,
            coroutineDispatcher = Dispatchers.Unconfined,
            densityProvider = { Density }
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(FileProvider::class)
        cacheDir.deleteRecursively()
    }

    @Test
    fun `mapper returns text when body is smaller than large body threshold`() = runTest {
        // Given
        val body = "<html>Small body</html>"

        // When
        val result = mapper.toUiContent(
            body = body,
            messageId = MessageId(MessageIdValue),
            shouldRestrictHeight = false
        )

        // Then
        val textResult = assertIs<MessageBodyContent.Text>(result)
        assertEquals(body, textResult.value)
    }

    @Test
    fun `toUiContent applies height restriction when requested`() = runTest {
        // Given
        val body = "<html>Small body</html>"
        val expectedCssMaxHeightPx = (WEB_VIEW_SAFE_PHYSICAL_MAX_HEIGHT_PX / Density).toInt()
        val expectedBody =
            "<style>body{max-height:${expectedCssMaxHeightPx}px !important;overflow:hidden !important;}</style>$body"

        // When
        val result = mapper.toUiContent(
            body = body,
            messageId = MessageId(MessageIdValue),
            shouldRestrictHeight = true
        )

        // Then
        val textResult = assertIs<MessageBodyContent.Text>(result)
        assertEquals(expectedBody, textResult.value)
    }

    @Test
    fun `mapper returns file content when body is larger than threshold`() = runTest {
        // Given
        val body = "a".repeat(LARGE_BODY_THRESHOLD_CHARS)
        val expectedUri = mockk<Uri>(relaxed = true)

        every {
            FileProvider.getUriForFile(
                context,
                "$PackageName.provider",
                any()
            )
        } returns expectedUri

        // When
        val result = mapper.toUiContent(
            body = body,
            messageId = MessageId(MessageIdValue),
            shouldRestrictHeight = false
        )

        // Then
        val fileResult = assertIs<MessageBodyContent.File>(result)
        assertEquals(expectedUri, fileResult.contentUri)

        val cachedFile = File(cacheDir, "body_cache/message_body_$MessageIdValue.html")
        assertTrue(cachedFile.exists())
        assertEquals(body, cachedFile.readText())
    }

    @Test
    fun `mapper falls back to text when file caching fails`() = runTest {
        // Given
        val body = "a".repeat(LARGE_BODY_THRESHOLD_CHARS)

        every {
            FileProvider.getUriForFile(
                context,
                "$PackageName.provider",
                any()
            )
        } throws IllegalArgumentException("FileProvider failed")

        // When
        val result = mapper.toUiContent(
            body = body,
            messageId = MessageId(MessageIdValue),
            shouldRestrictHeight = false
        )

        // Then
        val textResult = assertIs<MessageBodyContent.Text>(result)
        assertEquals(body, textResult.value)
    }

    private companion object {

        private const val PackageName = "ch.protonmail.android"
        private const val MessageIdValue = "message-id-123"
        private const val Density = 2f
    }
}
