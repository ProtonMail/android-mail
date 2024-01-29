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

package ch.protonmail.android.benchmark.scroll

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import ch.protonmail.android.benchmark.common.BenchmarkConfig
import ch.protonmail.android.benchmark.common.coreTraceSectionsList
import ch.protonmail.android.benchmark.common.performLogin
import ch.protonmail.android.benchmark.common.remoteApiTraceSectionsList
import ch.protonmail.android.benchmark.common.skipOnboarding
import ch.protonmail.android.benchmark.common.waitUntilFirstEmailRowShownOnMailboxList
import junit.framework.TestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This is a benchmark that scrolls the mailbox list up and down and measures how long the frames took.
 *
 * It navigates to the device's home screen, and launches the default activity. Note that it does not go through
 * the login, it expects a user to be already logged in before the benchmark is ran.
 *
 * In order to run, select the alphaBenchmark build variant for the app module.
 */
@RunWith(AndroidJUnit4::class)
class ScrollMailboxBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun scrollMailbox() = scroll()

    @OptIn(ExperimentalMetricApi::class)
    private fun scroll() {
        var firstStart = true
        benchmarkRule.measureRepeated(
            packageName = BenchmarkConfig.PackageName,
            metrics = listOf(
                StartupTimingMetric(),
                FrameTimingMetric()
            ) + coreTraceSectionsList() +
                remoteApiTraceSectionsList(),
            startupMode = null,
            iterations = BenchmarkConfig.DefaultIterations,
            setupBlock = {
                if (!firstStart) return@measureRepeated

                startActivityAndWait()
                performLogin()
                skipOnboarding()
                firstStart = false
            }
        ) {
            waitUntilFirstEmailRowShownOnMailboxList()

            val scrollableObject = device.findObject(By.scrollable(true))
            if (scrollableObject == null) {
                TestCase.fail("No scrollable view found in hierarchy")
            }
            scrollableObject.setGestureMargin(device.displayWidth / GestureMarginRatio)
            scrollableObject?.apply {
                repeat(NumberOfListFlings) {
                    fling(Direction.DOWN)
                }
                repeat(NumberOfListFlings) {
                    fling(Direction.UP)
                }
            }
        }
    }

    companion object {

        const val GestureMarginRatio = 10
        const val NumberOfListFlings = 5
    }
}
