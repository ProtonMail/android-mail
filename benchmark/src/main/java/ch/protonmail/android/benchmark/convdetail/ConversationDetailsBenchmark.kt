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

package ch.protonmail.android.benchmark.convdetail

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import ch.protonmail.android.benchmark.common.BenchmarkConfig
import ch.protonmail.android.benchmark.common.clickOnTheFirstEmailRowWaitDetailsShown
import ch.protonmail.android.benchmark.common.coreTraceSectionsList
import ch.protonmail.android.benchmark.common.performLogin
import ch.protonmail.android.benchmark.common.remoteApiTraceSectionsList
import ch.protonmail.android.benchmark.common.skipOnboarding
import ch.protonmail.android.benchmark.common.waitUntilFirstEmailRowShownOnMailboxList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ConversationDetailsBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * Start the application
     * Wait for the mailbox to be visible and messages loaded
     * Click on the first conversation
     * Wait for conversation details to be visible
     */
    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun testLoadingConversationDetailsScreen() {
        var firstStart = true

        benchmarkRule.measureRepeated(
            packageName = BenchmarkConfig.PackageName,
            metrics = listOf(
                StartupTimingMetric(),
                FrameTimingMetric()
            ) + coreTraceSectionsList() +
                remoteApiTraceSectionsList(),
            iterations = BenchmarkConfig.DefaultIterations,
            startupMode = StartupMode.COLD,
            setupBlock = {
                if (!firstStart) return@measureRepeated

                startActivityAndWait()
                performLogin()
                skipOnboarding()
                firstStart = false

                pressHome()
            }
        ) {

            startActivityAndWait()

            waitUntilFirstEmailRowShownOnMailboxList()

            clickOnTheFirstEmailRowWaitDetailsShown()
        }
    }
}
