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
import io.mockk.verifySequence
import me.proton.core.domain.entity.UserId
import org.junit.Test
import kotlin.random.Random

internal class DismissEmailNotificationsForUserTest {

    private val notificationManagerCompatProxy = mockk<NotificationManagerCompatProxy>(relaxUnitFun = true)
    private val dismissEmailNotificationsForUser = DismissEmailNotificationsForUser(notificationManagerCompatProxy)

    @Test
    fun `when dismissal is invoked with userId only, group notification is dismissed`() {
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
    fun `when dismissal is invoked and notification exists with only one child, dismiss group`() {
        // Given
        val userId = UserId("user-id")
        val notificationId = Random.nextInt()
        val groupId = userId.id.hashCode()

        every { notificationManagerCompatProxy.activeNotifications } returns listOf(
            mockk {
                every { this@mockk.groupKey } returns "12312323|${userId.id}"
                every { this@mockk.id } returns notificationId
            },
            mockk {
                every { this@mockk.groupKey } returns "12312323|${userId.id}"
                every { this@mockk.id } returns groupId
            }
        )

        // When
        dismissEmailNotificationsForUser(userId, notificationId, checkIfNotificationExists = true)

        // Then
        verifySequence {
            notificationManagerCompatProxy.activeNotifications // Check if exists
            notificationManagerCompatProxy.activeNotifications // Count children
            notificationManagerCompatProxy.dismissNotification(groupId)
        }
        confirmVerified(notificationManagerCompatProxy)
    }

    @Test
    fun `when dismissal is invoked and notification exists with multiple children, dismiss individual`() {
        // Given
        val userId = UserId("user-id")
        val notificationId = Random.nextInt()
        val groupId = userId.id.hashCode()

        every { notificationManagerCompatProxy.activeNotifications } returns listOf(
            mockk {
                every { this@mockk.groupKey } returns "12312323|${userId.id}"
                every { this@mockk.id } returns notificationId
            },
            mockk {
                every { this@mockk.groupKey } returns "12312323|${userId.id}"
                every { this@mockk.id } returns notificationId + 1
            },
            mockk {
                every { this@mockk.groupKey } returns "12312323|${userId.id}"
                every { this@mockk.id } returns groupId
            }
        )

        // When
        dismissEmailNotificationsForUser(userId, notificationId, checkIfNotificationExists = true)

        // Then
        verifySequence {
            notificationManagerCompatProxy.activeNotifications // Check if exists
            notificationManagerCompatProxy.activeNotifications // Count children
            notificationManagerCompatProxy.dismissNotification(notificationId)
        }
        confirmVerified(notificationManagerCompatProxy)
    }

    @Test
    fun `when dismissal is invoked and notification doesn't exist with no remaining children, dismiss group`() {
        // Given
        val userId = UserId("user-id")
        val notificationId = Random.nextInt()
        val groupId = userId.id.hashCode()

        every { notificationManagerCompatProxy.activeNotifications } returns listOf(
            mockk {
                every { this@mockk.groupKey } returns "12312323|${userId.id}"
                every { this@mockk.id } returns groupId // Only group remains
            }
        )

        // When
        dismissEmailNotificationsForUser(userId, notificationId, checkIfNotificationExists = true)

        // Then
        verifySequence {
            notificationManagerCompatProxy.activeNotifications // Check if exists
            notificationManagerCompatProxy.activeNotifications // Count remaining children
            notificationManagerCompatProxy.dismissNotification(groupId)
        }
        confirmVerified(notificationManagerCompatProxy)
    }

    @Test
    fun `when dismissal is invoked and notification doesn't exist with remaining children, dismiss individual`() {
        // Given
        val userId = UserId("user-id")
        val notificationId = Random.nextInt()
        val groupId = userId.id.hashCode()

        every { notificationManagerCompatProxy.activeNotifications } returns listOf(
            mockk {
                every { this@mockk.groupKey } returns "12312323|${userId.id}"
                every { this@mockk.id } returns notificationId + 1 // Different notification
            },
            mockk {
                every { this@mockk.groupKey } returns "12312323|${userId.id}"
                every { this@mockk.id } returns groupId
            }
        )

        // When
        dismissEmailNotificationsForUser(userId, notificationId, checkIfNotificationExists = true)

        // Then
        verifySequence {
            notificationManagerCompatProxy.activeNotifications // Check if exists
            notificationManagerCompatProxy.activeNotifications // Count remaining children
            notificationManagerCompatProxy.dismissNotification(notificationId)
        }
        confirmVerified(notificationManagerCompatProxy)
    }

    @Test
    fun `when dismissal is invoked without checking existence and only one child, dismiss group`() {
        // Given
        val userId = UserId("user-id")
        val notificationId = Random.nextInt()
        val groupId = userId.id.hashCode()

        every { notificationManagerCompatProxy.activeNotifications } returns listOf(
            mockk {
                every { this@mockk.groupKey } returns "12312323|${userId.id}"
                every { this@mockk.id } returns notificationId
            },
            mockk {
                every { this@mockk.groupKey } returns "12312323|${userId.id}"
                every { this@mockk.id } returns groupId
            }
        )

        // When
        dismissEmailNotificationsForUser(userId, notificationId, checkIfNotificationExists = false)

        // Then
        verifySequence {
            notificationManagerCompatProxy.activeNotifications // Count children
            notificationManagerCompatProxy.dismissNotification(groupId)
        }
        confirmVerified(notificationManagerCompatProxy)
    }

    @Test
    fun `when dismissal is invoked without checking existence and multiple children, dismiss individual`() {
        // Given
        val userId = UserId("user-id")
        val notificationId = Random.nextInt()
        val groupId = userId.id.hashCode()

        every { notificationManagerCompatProxy.activeNotifications } returns listOf(
            mockk {
                every { this@mockk.groupKey } returns "12312323|${userId.id}"
                every { this@mockk.id } returns notificationId
            },
            mockk {
                every { this@mockk.groupKey } returns "12312323|${userId.id}"
                every { this@mockk.id } returns notificationId + 1
            },
            mockk {
                every { this@mockk.groupKey } returns "12312323|${userId.id}"
                every { this@mockk.id } returns groupId
            }
        )

        // When
        dismissEmailNotificationsForUser(userId, notificationId, checkIfNotificationExists = false)

        // Then
        verifySequence {
            notificationManagerCompatProxy.activeNotifications // Count children
            notificationManagerCompatProxy.dismissNotification(notificationId)
        }
        confirmVerified(notificationManagerCompatProxy)
    }
}
