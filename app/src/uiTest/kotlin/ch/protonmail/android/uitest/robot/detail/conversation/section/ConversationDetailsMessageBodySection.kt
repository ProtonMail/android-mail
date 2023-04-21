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

package ch.protonmail.android.uitest.robot.detail.conversation.section

import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.Locator
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo

internal class ConversationDetailsMessageBodySection {

    internal fun verify(func: Verify.() -> Unit) = Verify().apply(func)

    internal inner class Verify {

        fun messageBodyIsDisplayedInWebView(messageBody: String, tagName: String = "html") {
            onWebView(ViewMatchers.withClassName(equalTo("android.webkit.WebView")))
                .forceJavascriptEnabled()
                .withElement(findElement(Locator.TAG_NAME, tagName))
                .check(webMatches(getText(), containsString(messageBody)))
        }
    }
}
