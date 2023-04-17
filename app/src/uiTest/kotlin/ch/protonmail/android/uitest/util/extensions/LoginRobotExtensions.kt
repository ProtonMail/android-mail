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

package ch.protonmail.android.uitest.util.extensions

import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreenTestTags
import ch.protonmail.android.uitest.util.UiDeviceHolder.uiDevice
import me.proton.core.test.android.robots.auth.login.LoginRobot

// This is needed as from the Login screen to the Mailbox, we switch from XML views to Compose layouts.
// If the Compose layout is not ready yet, checks performed on the Compose test rule
// might throw an IllegalStateException, making the test fail.
@Suppress("UnusedReceiverParameter")
fun LoginRobot.waitForMailboxScreen(timeout: Long = 15_000L) {
    uiDevice.wait(Until.hasObject(By.res(MailboxScreenTestTags.Root)), timeout)
}
