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

package ch.protonmail.android.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This is an example benchmark that scrolls the mailbox list up and down and measures how long the frames took.
 *
 * It navigates to the device's home screen, and launches the default activity. Note that it does not go through
 * the login, it expects a user to be already logged in before the benchmark is ran.
 *
 * In order to run, select the alphaBenchmark build variant for the app module.
 */
@RunWith(AndroidJUnit4::class)
@Ignore("The benchmark does not run by default right now. If you want to measure performance, run it locally.")
class ScrollMailboxBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startup() = benchmarkRule.measureRepeated(
        packageName = PackageName,
        metrics = listOf(FrameTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD,
        setupBlock = {
            pressHome()
            startActivityAndWait()

            device.wait(Until.hasObject(mailboxList()), WaitForMailboxTimeout)
        }
    ) {
        with(device.findObject(mailboxList())) {
            setGestureMargin((device.displayWidth * GestureMarginScreenWidthPercentage).toInt())
            repeat(NumberOfListFlings) {
                fling(Direction.DOWN)
                fling(Direction.UP)
            }
        }
    }

    private fun mailboxList() = By.res(MailboxListTag)

    private companion object {
        const val PackageName = "ch.protonmail.android.alpha"
        const val MailboxListTag = "MailboxList"
        const val WaitForMailboxTimeout = 20_000L
        const val NumberOfListFlings = 5
        const val GestureMarginScreenWidthPercentage = 0.2
    }
}
