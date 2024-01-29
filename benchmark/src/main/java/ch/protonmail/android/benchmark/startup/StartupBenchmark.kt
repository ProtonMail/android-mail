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

package ch.protonmail.android.benchmark.startup

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import ch.protonmail.android.benchmark.common.BenchmarkConfig
import ch.protonmail.android.benchmark.common.coreTraceSectionsList
import ch.protonmail.android.benchmark.common.performLogin
import ch.protonmail.android.benchmark.common.remoteApiTraceSectionsList
import ch.protonmail.android.benchmark.common.skipOnboarding
import ch.protonmail.android.benchmark.common.waitUntilFirstEmailRowShownOnMailboxList
import ch.protonmail.android.benchmark.common.waitUntilMailboxShownButEmailsNotLoaded
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * Measure the cold startup time when the mailbox is visible but emails are not loaded.
     */
    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun coldStartMailboxVisibleNoEmailsLoaded() = benchmarkRule.measureRepeated(
        packageName = BenchmarkConfig.PackageName,
        metrics = listOf(
            StartupTimingMetric(),
            FrameTimingMetric()
        ) + coreTraceSectionsList(),
        iterations = BenchmarkConfig.DefaultIterations,
        startupMode = StartupMode.COLD,
        setupBlock = {
            pressHome()
        }
    ) {

        startActivityAndWait()

        waitUntilMailboxShownButEmailsNotLoaded()
    }

    /**
     * Measure the cold startup time when the mailbox is visible and emails are loaded.
     * We do not wait for all emails to be loaded from network, we only wait for the first
     * one to be shown in the mailbox.
     *
     * At that point,we mark MainActivity to be fully drawn.
     */
    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun coldStartMailboxVisibleWithEmailsLoaded() {
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
        }
    }
}
