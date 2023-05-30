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

package ch.protonmail.android.uitest.robot.detail.section

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.model.Atoms.castOrDie
import androidx.test.espresso.web.model.Atoms.script
import androidx.test.espresso.web.sugar.Web
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.Locator
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.ui.MessageBodyTestTags
import ch.protonmail.android.uitest.util.awaitDisplayed
import ch.protonmail.android.uitest.util.onNodeWithText
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.core.Is.`is`
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class MessageBodySection(private val composeTestRule: ComposeTestRule) {

    private val webView: Web.WebInteraction<*> by lazy {
        onWebView(withClassName(equalTo("android.webkit.WebView"))).apply {
            forceJavascriptEnabled()
        }
    }

    fun waitUntilMessageIsShown(timeout: Duration = 30.seconds) {
        composeTestRule.waitForIdle()

        // Wait for the WebView to appear.
        composeTestRule.onNodeWithTag(MessageBodyTestTags.WebView).awaitDisplayed(composeTestRule, timeout)
    }

    internal fun verify(func: Verify.() -> Unit) = Verify().apply(func)

    internal inner class Verify {

        fun messageInWebViewContains(messageBody: String, tagName: String = "html") {
            webView.withElement(findElement(Locator.TAG_NAME, tagName))
                .check(webMatches(getText(), containsString(messageBody)))
        }

        fun loadingErrorMessageIsDisplayed(@StringRes errorMessage: Int) {
            composeTestRule.onNodeWithText(errorMessage)
                .assertIsDisplayed()
        }

        fun loadingErrorMessageIsDisplayed(errorMessage: String) {
            composeTestRule.onNodeWithText(errorMessage)
                .assertIsDisplayed()
        }

        fun bodyReloadButtonIsDisplayed() {
            composeTestRule.onNodeWithText(R.string.reload)
                .assertIsDisplayed()
        }

        fun bodyDecryptionErrorMessageIsDisplayed() {
            composeTestRule.onNodeWithText(R.string.decryption_error)
                .assertIsDisplayed()
        }

        fun hasRemoteImageLoaded(expected: Boolean) {
            composeTestRule.waitForIdle()

            val jsSnippet = """
                |var image = document.querySelector('img');
                |var isLoaded = image.complete && image.naturalWidth != 0 && image.naturalHeight != 0;
                |return isLoaded;
            """.trimMargin()

            runAndMatchJsCodeOutput(jsSnippet, expected)
        }

        private fun runAndMatchJsCodeOutput(script: String, expected: Boolean) {
            webView.check(
                webMatches(
                    script(script, castOrDie(Boolean::class.javaObjectType)),
                    `is`(equalTo(expected))
                )
            )
        }
    }
}
