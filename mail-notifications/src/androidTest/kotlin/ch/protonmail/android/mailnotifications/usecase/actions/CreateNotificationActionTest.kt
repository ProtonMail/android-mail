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

package ch.protonmail.android.mailnotifications.usecase.actions

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import ch.protonmail.android.mailnotifications.domain.NotificationsDeepLinkHelper
import ch.protonmail.android.mailnotifications.domain.model.LocalNotificationAction
import ch.protonmail.android.mailnotifications.domain.model.PushNotificationPendingIntentPayloadData
import ch.protonmail.android.mailnotifications.domain.usecase.actions.CreateNotificationAction
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class CreateNotificationActionTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().context

    private val notificationsDeepLinkHelper = mockk<NotificationsDeepLinkHelper>()
    private val createNotificationAction = CreateNotificationAction(context, notificationsDeepLinkHelper)

    @Test
    fun moveToArchiveNotificationActionRequiresAuthentication() {
        // When
        val result = createNotificationAction(archivePayloadData)

        // Then
        assertEquals("Archive", result.title)
        assertTrue(result.isAuthenticationRequired)
    }

    @Test
    fun moveToTrashNotificationActionRequiresAuthentication() {
        // When
        val result = createNotificationAction(trashPayloadData)

        // Then
        assertEquals("Trash", result.title)
        assertTrue(result.isAuthenticationRequired)
    }

    @Test
    fun replyNotificationActionRequiresAuthentication() {
        // When
        val result = createNotificationAction(replyPayloadData)

        // Then
        assertEquals("Reply", result.title)
        assertTrue(result.isAuthenticationRequired)
    }

    private companion object {

        val archivePayloadData = PushNotificationPendingIntentPayloadData(
            notificationId = 0,
            notificationGroup = "notificationGroup",
            userId = "userId",
            messageId = "messageId",
            action = LocalNotificationAction.MoveTo.Archive
        )
        val trashPayloadData = archivePayloadData.copy(action = LocalNotificationAction.MoveTo.Trash)
        val replyPayloadData = archivePayloadData.copy(action = LocalNotificationAction.Reply)
    }
}
