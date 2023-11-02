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

package ch.protonmail.android.benchmark.common

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.TraceSectionMetric
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import ch.protonmail.android.benchmark.common.BenchmarkConfig.WaitForMailboxTimeout

/**
 * Wait until the first row within the mailbox list is rendered.
 */
fun MacrobenchmarkScope.waitUntilFirstEmailRowShownOnMailboxList() {
    device.wait(Until.hasObject(By.res(TestTags.MailboxListTag)), WaitForMailboxTimeout)

    val mailboxList = device.findObject(By.res(TestTags.MailboxListTag))

    // Wait until the first row within the list is rendered
    mailboxList.wait(Until.hasObject(By.res(TestTags.FirstMailboxItemRow)), WaitForMailboxTimeout)
}

/**
 * Wait until the mailbox list is rendered but emails are not loaded.
 */
fun MacrobenchmarkScope.waitUntilMailboxShownButEmailsNotLoaded() {
    device.wait(Until.hasObject(By.res(TestTags.MailboxRootTag)), WaitForMailboxTimeout)
}

/**
 * Core trace sections to be benchmarked.
 */
@OptIn(ExperimentalMetricApi::class)
fun coreTraceSectionsList(): List<TraceSectionMetric> {
    return listOf(
        TraceSectionMetric("proton-app-init")
    )
}

/**
 * Remote Api trace sections to be benchmarked.
 */
@OptIn(ExperimentalMetricApi::class)
fun remoteApiTraceSectionsList(): List<TraceSectionMetric> {
    return listOf(
        TraceSectionMetric("proton-api-get-conversations"),
        TraceSectionMetric("proton-api-get-messages")
    )
}
