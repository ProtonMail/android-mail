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

package ch.protonmail.android.uitest.robot.helpers.section

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.service.notification.StatusBarNotification
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.helpers.DeviceRobot
import ch.protonmail.android.uitest.robot.helpers.models.NotificationEntry
import ch.protonmail.android.uitest.util.InstrumentationHolder
import kotlin.test.assertNotNull

@AttachTo(targets = [DeviceRobot::class], identifier = "notificationsSection")
internal class DeviceRobotNotificationsSection : ComposeSectionRobot() {

    private val notificationManager: NotificationManager
        get() = InstrumentationHolder.instrumentation.targetContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @VerifiesOuter
    inner class Verify {

        private val StatusBarNotification.title: String?
            get() = notification.extras.getString(Notification.EXTRA_TITLE)

        private val StatusBarNotification.body: String?
            get() = notification.extras.getString(Notification.EXTRA_TEXT)

        fun hasNotificationDisplayed(entry: NotificationEntry) {
            composeTestRule.waitForIdle()

            composeTestRule.waitUntil(NotificationTimeout) {
                notificationManager.activeNotifications.isNotEmpty()
            }

            val notification = notificationManager.activeNotifications.find {
                entry.title == it.title && entry.body == it.body && entry.isClearable == it.isClearable
            }

            assertNotNull(notification)
        }

        fun hasNoNotificationsDisplayed() {
            composeTestRule.waitForIdle()

            composeTestRule.waitUntil(NotificationTimeout) {
                notificationManager.activeNotifications.isEmpty()
            }
        }
    }

    private companion object {

        const val NotificationTimeout = 5000L
    }
}
