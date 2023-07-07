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

package ch.protonmail.android.uitest.helpers.core

import java.util.logging.Logger
import ch.protonmail.android.uitest.util.InstrumentationHolder.instrumentation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import me.proton.core.presentation.utils.showToast
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * A custom [TestWatcher] that logs the beginning and the end of a test execution along with its [TestId]s (if any).
 *
 * At the beginning of the test, it also shows a Toast to help cross-reference the implementation
 * with the scenario described in the test management tool.
 */
internal class TestIdWatcher : TestWatcher() {

    private val Description.testIds: String?
        get() {
            val annotation = annotations.find { it is TestId } as? TestId
            return annotation?.values?.joinToString()
        }

    private val Description.classMethodName: String
        get() = "$className#$methodName"

    override fun starting(description: Description) {
        super.starting(description)

        val testIds = description.testIds ?: return

        // Needs to run on the main thread, otherwise it won't show anything.
        runBlocking(Dispatchers.Main) {
            instrumentation.targetContext.showToast("Test ID(s) - $testIds", DefaultToastLength)
        }

        logger.info("Starting Test ID(s) $testIds - ${description.classMethodName}.")
    }

    override fun finished(description: Description) {
        super.finished(description)

        description.testIds?.let { testIds ->
            logger.info("Finished Test ID(s) $testIds - ${description.classMethodName}.")
        }
    }

    companion object {

        private val logger = Logger.getLogger(this::class.java.name)
        private const val DefaultToastLength = 5000
    }
}
