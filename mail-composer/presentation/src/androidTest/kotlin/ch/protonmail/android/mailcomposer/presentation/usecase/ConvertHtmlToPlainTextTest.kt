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

package ch.protonmail.android.mailcomposer.presentation.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

internal class ConvertHtmlToPlainTextTest {

    private val convertHtmlToPlainText = ConvertHtmlToPlainText()

    @Test
    fun shouldParseHtmlIntoString() {
        // Given
        val expected = """
            A message
            with body & new lines.
            With some other characters at the end <> /\

        """.trimIndent()

        val htmlMessage = """
            <p dir="ltr">A message<br>
            with body &amp; new lines.</p>
            <p dir="ltr">With some other characters at the end &lt;&gt; /\</p>
        """.trimIndent()

        // When
        val actual = convertHtmlToPlainText(htmlMessage)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun shouldRemoveHeadFromHtml() {
        // Given
        val expected = """
            Sent to myself
        """.trimIndent()

        // When
        val actual = convertHtmlToPlainText(HTML_WITH_CSS_IN_HEAD)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun shouldRemoveStyleFromAnywhereInTheHtml() {
        // Given
        val expected = """
            I have no style 
        """.trimIndent()

        // When
        val actual = convertHtmlToPlainText(HTML_WITH_HEAD_AND_STYLE_IN_BODY)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun shouldRemoveOpenStyleFromAnywhereInTheHtml() {
        // Given
        val expected = """
            I have no style 
        """.trimIndent()

        // When
        val actual = convertHtmlToPlainText(HTML_WITH_HEAD_AND_OPEN_STYLE_IN_BODY)

        // Then
        assertEquals(expected, actual)
    }
}

private val HTML_WITH_HEAD_AND_OPEN_STYLE_IN_BODY = """
    |<head>
    | <style>/* for HTML 5 */
    |nav,
    |section {
    |  display: block;
    |}
    |blockquote blockquote blockquote {
    |  padding: 0 !important;
    |  border: none !important;
    |}
    |
    |[hidden] {
    |  display: none;
    |}
    |</style>
    |  <meta name="viewport" content="width=462, maximum-scale=2">
    |</head>
    |
    |<body>
    |<style type="text/css">@media only screen and (min-width:768px){.templateContainer{width: 600px !important;}}
    |@media only screen and (max-width: 480px){body,table,td,p,a,li{-webkit-text-size-adjust: none !important;}</style>
    |I have no style
    |</body>
""".trimMargin("|")

private val HTML_WITH_HEAD_AND_STYLE_IN_BODY = """
    |<head>
    | <style>/* for HTML 5 */
    |nav,
    |section {
    |  display: block;
    |}
    |blockquote blockquote blockquote {
    |  padding: 0 !important;
    |  border: none !important;
    |}
    |
    |[hidden] {
    |  display: none;
    |}
    |</style>
    |  <meta name="viewport" content="width=462, maximum-scale=2">
    |</head>
    |
    |<body>
    | <style>
    |blockquote blockquote blockquote {
    |  padding: 0 !important;
    |  border: none !important;
    |}
    |</style>
    |I have no style
    |</body>
""".trimMargin("|")

private val HTML_WITH_CSS_IN_HEAD = """
    |<head>
    | <style>/* for HTML 5 */
    |article,
    |aside,
    |datalist,
    |details,
    |dialog,
    |figure,
    |footer,
    |header,
    |main,
    |menu,
    |nav,
    |section {
    |  display: block;
    |}
    |
    |audio,
    |canvas,
    |progress,
    |video {
    |  display: inline-block;
    |}
    |
    |blockquote blockquote blockquote {
    |  padding: 0 !important;
    |  border: none !important;
    |}
    |
    |[hidden] {
    |  display: none;
    |}
    |</style>
    |  <meta name="viewport" content="width=462, maximum-scale=2">
    |</head>Sent to myself
""".trimMargin("|")
