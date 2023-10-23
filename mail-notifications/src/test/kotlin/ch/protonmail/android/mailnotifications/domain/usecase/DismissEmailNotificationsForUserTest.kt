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

import ch.protonmail.android.mailnotifications.domain.proxy.NotificationManagerCompatProxy
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import me.proton.core.domain.entity.UserId
import org.junit.Test

internal class DismissEmailNotificationsForUserTest {

    private val notificationManagerCompatProxy = mockk<NotificationManagerCompatProxy>(relaxUnitFun = true)
    private val dismissEmailNotificationsForUser = DismissEmailNotificationsForUser(notificationManagerCompatProxy)

    @Test
    fun `when dismissal is invoked, the call is proxied to the notification manager`() {
        // Given
        val userId = UserId("user-id")

        // When
        dismissEmailNotificationsForUser(userId)

        // Then
        verify(exactly = 1) {
            notificationManagerCompatProxy.dismissNotification(userId.id.hashCode())
        }

        confirmVerified(notificationManagerCompatProxy)
    }
}
