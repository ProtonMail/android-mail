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
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
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
import ch.protonmail.android.mailmessage.presentation.ui.MessageBodyWebViewTestTags
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.detail.ConversationDetailRobot
import ch.protonmail.android.uitest.robot.detail.MessageDetailRobot
import ch.protonmail.android.uitest.util.awaitDisplayed
import ch.protonmail.android.uitest.util.onNodeWithText
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.core.Is.`is`
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@AttachTo(targets = [ConversationDetailRobot::class, MessageDetailRobot::class])
internal class MessageBodySection : ComposeSectionRobot() {

    private val webView: Web.WebInteraction<*> by lazy {
        onWebView(withClassName(equalTo(WebViewClassName))).apply {
            forceJavascriptEnabled()
        }
    }

    private val webViewWrapper: SemanticsNodeInteraction by lazy {
        composeTestRule.onNodeWithTag(MessageBodyWebViewTestTags.WebView)
    }

    private val webViewAlternative: SemanticsNodeInteraction by lazy {
        composeTestRule.onNodeWithTag(MessageBodyTestTags.WebViewAlternative)
    }

    fun waitUntilMessageIsShown(timeout: Duration = 30.seconds) {
        composeTestRule.waitForIdle()

        // Wait for the WebView to appear.
        webViewWrapper.awaitDisplayed(timeout = timeout)
    }

    @VerifiesOuter
    internal inner class Verify {

        fun isShowingMissingWebViewWarning() {
            webViewAlternative.awaitDisplayed()
            webViewWrapper.assertDoesNotExist()
        }

        fun messageInWebViewContains(messageBody: String, tagName: String = "html") {
            webView.withElement(findElement(Locator.TAG_NAME, tagName))
                .check(webMatches(getText(), containsString(messageBody)))
            webViewAlternative.assertDoesNotExist()
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
            val jsSnippet = """
                |var image = document.querySelector('img');
                |var isLoaded = image.complete && image.naturalWidth != 0 && image.naturalHeight != 0;
                |return isLoaded;
            """.trimMargin()

            runAndMatchJsCodeOutput(jsSnippet, expected)
        }

        fun hasHtmlContentSanitised() {
            val jsSnippet = """
                |var onLoad = document.querySelector('body').onload;
                |var form = document.querySelector('form');
                |var relLinks = document.querySelector('link');
                |var iframe = document.querySelector('iframe');
                |var ping = document.querySelector('a').ping;
                |return onLoad == null && form == null && relLinks == null && iframe == null && ping == "";
            """.trimMargin()

            runAndMatchJsCodeOutput(jsSnippet, true)
        }

        fun hasEmbeddedImages(expectedNumber: Int) {
            val jsSnippet = """
                |var embeddedImages = Array.from(document.getElementsByClassName('proton-embedded'));
                |return embeddedImages.length == $expectedNumber; 
            """.trimMargin()

            runAndMatchJsCodeOutput(jsSnippet, true)
        }

        fun hasEmbeddedImagesSuccessfullyLoaded(expected: Boolean) {
            val jsSnippet = """
                |var embeddedImages = Array.from(document.getElementsByClassName('proton-embedded'));
                |if (embeddedImages.length == 0) {
                |   return false;
                |}
                |var imagesLoaded = embeddedImages.every(value => value.complete && value.naturalHeight != 0 && value.naturalWidth != 0);
                |return imagesLoaded;
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

    private companion object {

        const val WebViewClassName = "android.webkit.WebView"
    }
}
