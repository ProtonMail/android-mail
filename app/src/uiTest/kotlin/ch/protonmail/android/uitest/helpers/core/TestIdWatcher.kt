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

import ch.protonmail.android.uitest.util.InstrumentationHolder.instrumentation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import me.proton.core.presentation.utils.showToast
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.logging.Logger

/**
 * A custom [TestWatcher] that logs the beginning and the end of a test execution along with its [TestId] (if any).
 *
 * At the beginning of the test, it also shows a Toast to help cross-reference the implementation
 * with the scenario described in the test management tool.
 */
internal class TestIdWatcher : TestWatcher() {

    override fun starting(description: Description) {
        super.starting(description)

        val testIdAnnotation = description.testId ?: return

        // Needs to run on the main thread, otherwise it won't show anything.
        runBlocking(Dispatchers.Main) {
            instrumentation.targetContext.showToast("Test ID - ${testIdAnnotation.value}", DEFAULT_TOAST_LENGTH)
        }

        logger.info("Starting Test ID ${testIdAnnotation.value} - ${description.methodName}.")
    }

    override fun finished(description: Description) {
        super.finished(description)

        val testIdAnnotation = description.testId ?: return
        logger.info("Finished Test ID ${testIdAnnotation.value} - ${description.methodName}.")
    }

    private val Description.testId: TestId?
        get() = annotations.find { it is TestId } as? TestId

    companion object {

        private val logger = Logger.getLogger(this::class.java.name)
        private const val DEFAULT_TOAST_LENGTH = 5000
    }
}
