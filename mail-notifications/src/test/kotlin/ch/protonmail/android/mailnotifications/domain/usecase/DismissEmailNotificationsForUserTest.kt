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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import me.proton.core.domain.entity.UserId
import org.junit.Test
import kotlin.random.Random

internal class DismissEmailNotificationsForUserTest {

    private val notificationManagerCompatProxy = mockk<NotificationManagerCompatProxy>(relaxUnitFun = true)
    private val dismissEmailNotificationsForUser = DismissEmailNotificationsForUser(notificationManagerCompatProxy)

    @Test
    fun `when dismissal is invoked, call is proxied when activeNotifications count is greater than 1`() {
        // Given
        val userId = UserId("user-id")

        // When
        dismissEmailNotificationsForUser(userId)

        // Then
        verifySequence {
            notificationManagerCompatProxy.dismissNotification(userId.id.hashCode())
        }

        confirmVerified(notificationManagerCompatProxy)
    }

    @Test
    fun `when dismissal is invoked for a standard notification, group is dismissed when group count is less than 2`() {
        // Given
        val userId = UserId("user-id")
        val notificationId = Random.nextInt()
        every { notificationManagerCompatProxy.activeNotifications } returns listOf(
            mockk { every { this@mockk.groupKey } returns "12312323|${userId.id}" }
        )

        // When
        dismissEmailNotificationsForUser(userId, notificationId, isSilentNotification = false)

        // Then
        verifySequence {
            notificationManagerCompatProxy.activeNotifications
            notificationManagerCompatProxy.dismissNotification(userId.id.hashCode())
        }
        confirmVerified(notificationManagerCompatProxy)
    }

    @Test
    fun `when dismissal is invoked for a standard notification, group is not dismissed on count greater than 2`() {
        // Given
        val userId = UserId("user-id")
        val notificationId = Random.nextInt()
        every { notificationManagerCompatProxy.activeNotifications } returns listOf(
            mockk { every { this@mockk.groupKey } returns "12312323|${userId.id}" },
            mockk { every { this@mockk.groupKey } returns "12312323|${userId.id}" },
            mockk { every { this@mockk.groupKey } returns "12312323|${userId.id}" }
        )

        // When
        dismissEmailNotificationsForUser(userId, notificationId, isSilentNotification = false)

        // Then
        verifySequence {
            notificationManagerCompatProxy.activeNotifications
            notificationManagerCompatProxy.dismissNotification(notificationId)
        }
        confirmVerified(notificationManagerCompatProxy)
    }

    @Test
    fun `when dismissal is invoked for a silent notification, dismiss group only if notification is active`() {
        // Given
        val userId = UserId("user-id")
        val notificationId = Random.nextInt()
        every { notificationManagerCompatProxy.activeNotifications } returns listOf(
            mockk {
                every { this@mockk.groupKey } returns "12312323|${userId.id}"
                every { this@mockk.id } returns notificationId
            },
            mockk {
                every { this@mockk.groupKey } returns "12312323|${userId.id}"
                every { this@mockk.id } returns notificationId + 1
            }
        )

        // When
        dismissEmailNotificationsForUser(userId, notificationId, isSilentNotification = true)

        // Then
        verify(exactly = 2) { notificationManagerCompatProxy.activeNotifications }
        verify { notificationManagerCompatProxy.dismissNotification(userId.id.hashCode()) }
        confirmVerified(notificationManagerCompatProxy)
    }

    @Test
    fun `when dismissal is invoked for a silent notification, do not dismiss group if others are active`() {
        // Given
        val userId = UserId("user-id")
        val notificationId = Random.nextInt()
        every { notificationManagerCompatProxy.activeNotifications } returns listOf(
            mockk {
                every { this@mockk.groupKey } returns "12312323|${userId.id}"
                every { this@mockk.id } returns notificationId
            },
            mockk {
                every { this@mockk.groupKey } returns "12312323|${userId.id}"
                every { this@mockk.id } returns "another-id".hashCode()
            },
            mockk {
                every { this@mockk.groupKey } returns "12312323|${userId.id}"
                every { this@mockk.id } returns "another-id-2".hashCode()
            }
        )

        // When
        dismissEmailNotificationsForUser(userId, notificationId, isSilentNotification = true)

        // Then
        verify(exactly = 2) { notificationManagerCompatProxy.activeNotifications }
        verify { notificationManagerCompatProxy.dismissNotification(notificationId) }
        confirmVerified(notificationManagerCompatProxy)
    }

    @Test
    fun `when dismissal is invoked for a silent notification, do not dismiss group if notification is not active`() {
        // Given
        val userId = UserId("user-id")
        val notificationId = Random.nextInt()
        every { notificationManagerCompatProxy.activeNotifications } returns listOf(
            mockk {
                every { this@mockk.groupKey } returns "12312323|${userId.id}"
                every { this@mockk.id } returns "another-id".hashCode()
            }
        )

        // When
        dismissEmailNotificationsForUser(userId, notificationId, isSilentNotification = true)

        // Then
        verify { notificationManagerCompatProxy.activeNotifications }
        verify { notificationManagerCompatProxy.dismissNotification(notificationId) }
        confirmVerified(notificationManagerCompatProxy)
    }
}
