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

package ch.protonmail.android.uitest.e2e.account

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import ch.protonmail.android.R
import me.proton.core.test.android.robots.auth.AddAccountRobot

fun addAccountRobot(block: AddAccountRobot.() -> Unit) = AddAccountRobot().apply(block)

fun AddAccountRobot.Verify.isDisplayed() {
    // There are no efficient ways to check for this since it does not have a root view id.
    onView(withId(R.id.sign_in)).check(matches(ViewMatchers.isDisplayed()))
    onView(withId(R.id.sign_up)).check(matches(ViewMatchers.isDisplayed()))
}
