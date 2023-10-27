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

package ch.protonmail.android.mailnotifications.domain.usecase

import androidx.work.ListenableWorker
import ch.protonmail.android.mailnotifications.domain.model.LocalPushNotificationData
import ch.protonmail.android.mailnotifications.domain.model.MessageReadPushData
import ch.protonmail.android.mailnotifications.domain.proxy.NotificationManagerCompatProxy
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals

internal class ProcessMessageReadPushNotificationTest {

    private val notificationManagerCompatProxy = mockk<NotificationManagerCompatProxy>(relaxUnitFun = true)
    private val processMessageReadPushNotification = ProcessMessageReadPushNotification(notificationManagerCompatProxy)

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `when notification group not null and there is only one child notification, the group is also dismissed`() {
        // Given
        every { notificationManagerCompatProxy.getGroupKeyForNotification(NotificationId) } returns NotificationGroupKey

        // When
        val result = processMessageReadPushNotification(NotificationData)

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 1) {
            notificationManagerCompatProxy.getGroupKeyForNotification(MessageId.hashCode())
            notificationManagerCompatProxy.dismissNotification(NotificationId)
            notificationManagerCompatProxy.dismissNotificationGroupIfEmpty(NotificationGroupKey)
        }
        confirmVerified(notificationManagerCompatProxy)
    }

    @Test
    fun `when notification group is null, no dismissal at all is performed`() {
        // Given
        every { notificationManagerCompatProxy.getGroupKeyForNotification(NotificationId) } returns null

        // When
        val result = processMessageReadPushNotification(NotificationData)

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 1) {
            notificationManagerCompatProxy.getGroupKeyForNotification(MessageId.hashCode())
        }
        verify(exactly = 0) {
            notificationManagerCompatProxy.dismissNotification(any())
            notificationManagerCompatProxy.dismissNotificationGroupIfEmpty(any())
        }
        confirmVerified(notificationManagerCompatProxy)
    }

    private companion object {

        const val MessageId = "messageId"
        val NotificationData = LocalPushNotificationData.MessageRead(MessageReadPushData(messageId = MessageId))
        val NotificationId = MessageId.hashCode()
        const val NotificationGroupKey = "notificationGroupKey"
    }
}
