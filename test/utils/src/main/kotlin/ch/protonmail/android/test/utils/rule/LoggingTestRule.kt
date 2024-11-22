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

package ch.protonmail.android.test.utils.rule

import android.util.Log
import ch.protonmail.android.test.utils.TestTree
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import timber.log.Timber
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoggingTestRule(
    private val testTree: TestTree = TestTree()
) : TestWatcher() {

    override fun starting(description: Description) {
        Timber.plant(testTree)
    }

    override fun finished(description: Description) {
        Timber.uproot(testTree)
    }

    fun assertErrorLogged(message: String) {
        val expectedLog = TestTree.Log(Log.ERROR, null, message, null)
        val errorLogs = testTree.logs.filter { it.priority == Log.ERROR }
        assertEquals(expectedLog, errorLogs.lastOrNull())
    }

    fun assertDebugLogged(message: String) {
        val expectedLog = TestTree.Log(Log.DEBUG, null, message, null)
        val debugLogs = testTree.logs.filter { it.priority == Log.DEBUG }
        assertEquals(expectedLog, debugLogs.lastOrNull())
    }
    fun assertWarningLogged(message: String) {
        val expectedLog = TestTree.Log(Log.WARN, null, message, null)
        val warningLogs = testTree.logs.filter { it.priority == Log.WARN }
        assertEquals(expectedLog, warningLogs.lastOrNull())
    }

    fun assertNoWarningLogs() {
        val logsWarningUp = testTree.logs.filter { it.priority >= Log.WARN }
        assertTrue(logsWarningUp.isEmpty(), "Excepted no warning logged, found: $logsWarningUp")
    }
}
