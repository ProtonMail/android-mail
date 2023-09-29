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

package ch.protonmail.android.uitest.robot.helpers

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import ch.protonmail.android.MainActivity
import ch.protonmail.android.test.ksp.annotations.AsDsl
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.test.robot.ProtonMailRobot
import ch.protonmail.android.uitest.util.ActivityScenarioHolder
import org.junit.Assert.assertEquals

@AsDsl
internal class DeviceRobot : ProtonMailRobot {

    private val activityScenario: ActivityScenario<MainActivity>
        get() = ActivityScenarioHolder.scenario

    fun pressBack() {
        Espresso.pressBackUnconditionally()
    }

    fun triggerActivityRecreation() {
        activityScenario.recreate()
    }

    @VerifiesOuter
    inner class Verify {

        fun isMainActivityNotDisplayed() {
            assertEquals(Lifecycle.State.DESTROYED, activityScenario.state)
        }
    }
}
