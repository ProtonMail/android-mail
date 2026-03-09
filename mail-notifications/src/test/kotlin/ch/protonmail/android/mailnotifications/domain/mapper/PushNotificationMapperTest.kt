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

package ch.protonmail.android.mailnotifications.domain.mapper

import ch.protonmail.android.mailnotifications.NotificationTestData
import ch.protonmail.android.mailnotifications.data.wrapper.DecryptedPushNotificationWrapper
import ch.protonmail.android.mailnotifications.domain.model.LocalPushNotification
import uniffi.mail_uniffi.DecryptedPushNotification
import kotlin.test.Test
import kotlin.test.assertEquals

class PushNotificationMapperTest {

    @Test
    fun `should map email data correctly for new message`() {
        // Given
        val newMessageDecryptedPushNotification = DecryptedPushNotificationWrapper(
            DecryptedPushNotification.Email(NotificationTestData.decryptedEmailPushNotification)
        )

        val expectedLocalPush = LocalPushNotification(
            userPushData = NotificationTestData.defaultUserPushData,
            pushNotificationData = NotificationTestData.defaultMessagePushData
        )

        // When
        val actual = PushNotificationMapper.toLocalPushNotification(
            NotificationTestData.defaultUserPushData,
            newMessageDecryptedPushNotification
        )

        // Then
        assertEquals(expectedLocalPush, actual)
    }

    @Test
    fun `should map email data correctly for message read`() {
        // Given
        val messageReadPushNotification = DecryptedPushNotificationWrapper(
            DecryptedPushNotification.Email(NotificationTestData.decryptedMessageReadPushNotification)
        )

        val expectedLocalPush = LocalPushNotification(
            userPushData = NotificationTestData.defaultUserPushData,
            pushNotificationData = NotificationTestData.defaultMessageReadPushData
        )

        // When
        val actual = PushNotificationMapper.toLocalPushNotification(
            NotificationTestData.defaultUserPushData,
            messageReadPushNotification
        )

        // Then
        assertEquals(expectedLocalPush, actual)
    }

    @Test
    fun `should map url data correctly`() {
        // Given
        val messageReadPushNotification = DecryptedPushNotificationWrapper(
            DecryptedPushNotification.OpenUrl(NotificationTestData.decryptedOpenUrlPushNotification)
        )

        val expectedLocalPush = LocalPushNotification(
            userPushData = NotificationTestData.defaultUserPushData,
            pushNotificationData = NotificationTestData.defaultOpenUrlPushData
        )

        // When
        val actual = PushNotificationMapper.toLocalPushNotification(
            NotificationTestData.defaultUserPushData,
            messageReadPushNotification
        )

        // Then
        assertEquals(expectedLocalPush, actual)
    }

    @Test
    fun `should handle unknown action data correctly`() {
        // Given
        val unknownPushNotification = DecryptedPushNotificationWrapper(
            DecryptedPushNotification.Email(NotificationTestData.decryptedMessageUnknownPushNotification)
        )

        val expectedLocalPush = LocalPushNotification(
            userPushData = NotificationTestData.defaultUserPushData,
            pushNotificationData = NotificationTestData.defaultMessageUnexpectedData
        )

        // When
        val actual = PushNotificationMapper.toLocalPushNotification(
            NotificationTestData.defaultUserPushData,
            unknownPushNotification
        )

        // Then
        assertEquals(expectedLocalPush, actual)
    }
}
