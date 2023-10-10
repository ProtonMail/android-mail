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

package ch.protonmail.android.uitest.rule

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import org.junit.rules.ExternalResource

internal class MockIntentsRule(private val captureIntents: Boolean) : ExternalResource() {

    private val fakeActivityResult = ActivityResult(Activity.RESULT_OK, Intent())

    override fun before() {
        if (!captureIntents) return

        Intents.init()

        // Attachment viewing
        Intents.intending(hasAction(Intent.ACTION_VIEW)).respondWith(fakeActivityResult)
    }

    override fun after() {
        if (!captureIntents) return

        Intents.release()
    }
}
