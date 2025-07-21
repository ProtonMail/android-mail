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
import ch.protonmail.android.mailnotifications.domain.model.UserPushData
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import me.proton.core.domain.entity.UserId
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals

internal class ProcessMessageReadPushNotificationTest {

    private val dismissEmailNotificationsForUser = mockk<DismissEmailNotificationsForUser>(relaxUnitFun = true)
    private val processMessageReadPushNotification =
        ProcessMessageReadPushNotification(dismissEmailNotificationsForUser)

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `when touched notification data is processed, dismissal is called for a notification with the same id`() {
        // When
        val result = processMessageReadPushNotification(NotificationData)

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 1) {
            dismissEmailNotificationsForUser(
                userId = UserId,
                notificationId = NotificationId,
                checkIfNotificationExists = true
            )
        }
        confirmVerified(dismissEmailNotificationsForUser)
    }

    private companion object {

        const val MessageId = "messageId"
        const val UserEmail = "userId@proton.me"
        val UserId = UserId("userId")
        val NotificationData = LocalPushNotificationData.MessageRead(
            UserPushData(UserId.id, UserEmail),
            MessageReadPushData(MessageId)
        )
        val NotificationId = MessageId.hashCode()
    }
}
