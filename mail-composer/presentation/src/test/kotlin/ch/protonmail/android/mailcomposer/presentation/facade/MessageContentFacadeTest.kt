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

package ch.protonmail.android.mailcomposer.presentation.facade

import ch.protonmail.android.mailcomposer.domain.model.OriginalHtmlQuote
import ch.protonmail.android.mailcomposer.presentation.usecase.ConvertHtmlToPlainText
import ch.protonmail.android.mailcomposer.presentation.usecase.StyleQuotedHtml
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.BeforeTest

internal class MessageContentFacadeTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val convertHtmlToPlainText = mockk<ConvertHtmlToPlainText>(relaxed = true)
    private val styleQuotedHtml = mockk<StyleQuotedHtml>(relaxed = true)

    private lateinit var messageContentFacade: MessageContentFacade

    @BeforeTest
    fun setup() {
        messageContentFacade = MessageContentFacade(
            convertHtmlToPlainText,
            styleQuotedHtml,
            testDispatcher
        )
    }

    @Test
    fun `should proxy convertHtmlToPlainText accordingly`() = runTest {
        // Given
        val html = "some-html"

        // Then
        messageContentFacade.convertHtmlToPlainText(html)

        // When
        coVerify { convertHtmlToPlainText.invoke(html) }
    }

    @Test
    fun `should proxy styleQuotedHtml accordingly`() = runTest {
        // Given
        val quotedHtml = OriginalHtmlQuote("original-text-html")

        // Then
        messageContentFacade.styleQuotedHtml(quotedHtml)

        // When
        coVerify { styleQuotedHtml.invoke(quotedHtml) }
    }
}
