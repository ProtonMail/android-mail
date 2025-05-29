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

package ch.protonmail.android.mailnotifications.usecase

import java.time.Instant
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import ch.protonmail.android.mailcommon.presentation.system.NotificationProvider
import ch.protonmail.android.mailnotifications.bigText
import ch.protonmail.android.mailnotifications.domain.model.LocalPushNotificationData
import ch.protonmail.android.mailnotifications.domain.model.NewLoginPushData
import ch.protonmail.android.mailnotifications.domain.model.PushNotificationDismissPendingIntentData
import ch.protonmail.android.mailnotifications.domain.model.UserPushData
import ch.protonmail.android.mailnotifications.domain.proxy.NotificationManagerCompatProxy
import ch.protonmail.android.mailnotifications.domain.usecase.ProcessNewLoginPushNotification
import ch.protonmail.android.mailnotifications.domain.usecase.actions.CreateNotificationAction
import ch.protonmail.android.mailnotifications.summaryText
import ch.protonmail.android.mailnotifications.text
import ch.protonmail.android.mailnotifications.title
import ch.protonmail.android.test.annotations.suite.SmokeTest
import io.mockk.CapturingSlot
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@SmokeTest
internal class ProcessNewLoginPushNotificationTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().context

    private val notificationProvider = getNotificationProvider()
    private val notificationManagerCompatProxy = mockk<NotificationManagerCompatProxy>(relaxUnitFun = true)
    private val createNotificationAction = mockk<CreateNotificationAction>()

    private val processNewLoginPushNotification = ProcessNewLoginPushNotification(
        context,
        notificationProvider,
        notificationManagerCompatProxy,
        createNotificationAction
    )

    private val userData = UserPushData(NotificationUserId, NotificationEmail)
    private val pushData = NewLoginPushData(NotificationSender, NotificationSummary, NotificationUrl)
    private val data = LocalPushNotificationData.Login(userData, pushData)

    @Before
    fun setup() {
        mockkStatic(Instant::class)
        notificationProvider.initNotificationChannels() // Do not rely on initializers.
        mockInstant()
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun pushLoginNotificationsCreatedAfterDataProcessing() {
        // Given
        val expectedOpenUrlNotificationGroupId = "$NotificationUserId-openurl".hashCode()
        val expectedOpenUrlNotificationEntryId = Instant.now().hashCode()
        val notification = slot<Notification>()
        val groupNotification = slot<Notification>()

        every { createNotificationAction(any<PushNotificationDismissPendingIntentData>()) } returns mockk()

        // When
        val result = processNewLoginPushNotification(data)

        // Then
        assertEquals(ListenableWorker.Result.success(), result)

        verify(exactly = 1) {
            notificationManagerCompatProxy.showNotification(expectedOpenUrlNotificationEntryId, capture(notification))
            notificationManagerCompatProxy.showNotification(
                expectedOpenUrlNotificationGroupId,
                capture(groupNotification)
            )
        }

        confirmVerified(notificationManagerCompatProxy)
        verifySingleNotification(notification)
        verifyGroupNotification(groupNotification)
    }

    private fun mockInstant() {
        every { Instant.now() } returns mockk { every { epochSecond } returns 123 }
    }

    private fun verifySingleNotification(notification: CapturingSlot<Notification>) {
        with(notification.captured) {
            assertEquals(NotificationProvider.LOGIN_CHANNEL_ID, channelId)
            assertTrue(actions.isNullOrEmpty())
            assertEquals(NotificationSender, title)
            assertEquals(NotificationSummary, text)
            assertEquals(NotificationSummary, bigText)
            assertEquals(NotificationEmail, summaryText)
        }
    }

    private fun verifyGroupNotification(notification: CapturingSlot<Notification>) {
        with(notification.captured) {
            assertEquals(NotificationProvider.LOGIN_CHANNEL_ID, channelId)
            assertTrue(actions.isNullOrEmpty())
            assertEquals(NotificationSender, title)
            assertEquals(NotificationGroupSummary, text)
            assertEquals(NotificationGroupSummary, bigText)
            assertEquals(NotificationEmail, summaryText)
        }
    }

    private fun getNotificationProvider(): NotificationProvider = NotificationProvider(
        context,
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    )

    private companion object {

        const val NotificationSender = "Proton Mail"
        const val NotificationSummary = "New login attempt"
        const val NotificationGroupSummary = "New login alerts"
        const val NotificationEmail = "primary-email@pm.me"
        const val NotificationUserId = "user-id"
        const val NotificationUrl = "url"
    }
}
