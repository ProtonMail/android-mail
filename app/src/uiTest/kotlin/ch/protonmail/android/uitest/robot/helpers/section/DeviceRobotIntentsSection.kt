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

package ch.protonmail.android.uitest.robot.helpers.section

import android.content.Intent
import android.net.Uri
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.times
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.helpers.DeviceRobot
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.any
import org.hamcrest.Matcher

@AttachTo(targets = [DeviceRobot::class], identifier = "intents")
internal class DeviceRobotIntentsSection : ComposeSectionRobot() {

    @VerifiesOuter
    inner class Verify {

        fun filePickerIntentWasLaunched(
            times: Int = 1,
            mimeType: String = "*/*",
            timeout: Long = 5000L
        ) {
            composeTestRule.waitUntil(timeout) {
                runCatching {
                    intended(allOf(hasAction(Intent.ACTION_GET_CONTENT), mimeType.asMimeTypeMatcher()), times(times))
                }.isSuccess
            }
        }

        fun actionViewUriIntentWasLaunched(
            times: Int = 1,
            url: String,
            timeout: Long = 10_000L
        ) {
            composeTestRule.waitUntil(timeout) {
                runCatching {
                    intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(Uri.parse(url))), times(times))
                }.isSuccess
            }
        }

        fun actionViewIntentWasLaunched(
            times: Int = 1,
            mimeType: String? = null,
            timeout: Long = 10_000L
        ) {
            composeTestRule.waitUntil(timeout) {
                runCatching {
                    intended(allOf(hasAction(Intent.ACTION_VIEW), mimeType.asMimeTypeMatcher()), times(times))
                }.isSuccess
            }
        }

        fun actionViewIntentWasNotLaunched(mimeType: String? = null) {
            actionViewIntentWasLaunched(times = 0, mimeType = mimeType)
        }

        private fun String?.asMimeTypeMatcher(): Matcher<Intent> {
            this ?: return hasType(any(String::class.java))
            return hasType(this)
        }
    }
}
