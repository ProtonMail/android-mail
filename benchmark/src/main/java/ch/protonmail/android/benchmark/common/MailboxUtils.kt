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

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

/**
 * Wait until the first row within the mailbox list is rendered.
 */
fun MacrobenchmarkScope.waitUntilFirstEmailRowShownOnMailboxList() {
    device.wait(Until.hasObject(By.res(TestTags.MailboxListTag)), BenchmarkConfig.WaitForMailboxTimeout)

    val mailboxList = device.findObject(By.res(TestTags.MailboxListTag))

    // Wait until the first row within the list is rendered
    mailboxList.wait(Until.hasObject(By.res(TestTags.FirstMailboxItemRow)), BenchmarkConfig.WaitForMailboxTimeout)
}


/**
 * Click on the first row and wait until message details are shown.
 */
fun MacrobenchmarkScope.clickOnTheFirstEmailRowWaitDetailsShown() {

    val mailboxList = device.findObject(By.res(TestTags.MailboxListTag))

    val firstEmailRow = mailboxList.findObject(By.res(TestTags.FirstMailboxItemRow))

    firstEmailRow.click()

    device.wait(Until.hasObject(By.res(TestTags.MessageBodyNoWebView)), BenchmarkConfig.WaitForMessageDetailsTimeout)
}

/**
 * Wait until the mailbox list is rendered but emails are not loaded.
 */
fun MacrobenchmarkScope.waitUntilMailboxShownButEmailsNotLoaded() {
    device.wait(Until.hasObject(By.res(TestTags.MailboxRootTag)), BenchmarkConfig.WaitForMailboxTimeout)
}
