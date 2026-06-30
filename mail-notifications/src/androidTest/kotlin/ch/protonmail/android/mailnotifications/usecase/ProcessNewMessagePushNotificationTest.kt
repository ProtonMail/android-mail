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

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.system.NotificationChannelId
import ch.protonmail.android.mailcommon.presentation.system.NotificationProvider
import ch.protonmail.android.mailnotifications.domain.NotificationsDeepLinkHelper
import ch.protonmail.android.mailnotifications.domain.model.LocalNotificationAction
import ch.protonmail.android.mailnotifications.domain.model.LocalPushNotification
import ch.protonmail.android.mailnotifications.domain.model.LocalPushNotificationData
import ch.protonmail.android.mailnotifications.domain.model.PushNotificationDismissPendingIntentData
import ch.protonmail.android.mailnotifications.domain.model.PushNotificationPendingIntentPayloadData
import ch.protonmail.android.mailnotifications.domain.model.PushNotificationSenderData
import ch.protonmail.android.mailnotifications.domain.proxy.NotificationManagerCompatProxy
import ch.protonmail.android.mailnotifications.domain.usecase.ProcessNewMessagePushNotification
import ch.protonmail.android.mailnotifications.domain.usecase.actions.CreateNotificationAction
import ch.protonmail.android.mailnotifications.domain.usecase.intents.CreateNewMessageNavigationIntent
import ch.protonmail.android.mailnotifications.subText
import ch.protonmail.android.mailnotifications.text
import ch.protonmail.android.mailnotifications.title
import ch.protonmail.android.mailsettings.domain.model.ExtendedNotificationPreference
import ch.protonmail.android.mailsettings.domain.usecase.notifications.GetExtendedNotificationsSetting
import ch.protonmail.android.test.annotations.suite.SmokeTest
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.domain.entity.UserId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@SmokeTest
internal class ProcessNewMessagePushNotificationTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().context

    private val notificationProvider = getNotificationProvider()
    private val notificationManagerCompatProxy = mockk<NotificationManagerCompatProxy>(relaxUnitFun = true)
    private val notificationsDeepLinkHelper = mockk<NotificationsDeepLinkHelper>()
    private val getNotificationsExtendedPreference = mockk<GetExtendedNotificationsSetting>() {
        coEvery { this@mockk.invoke() } returns ExtendedNotificationPreference(true).right()
    }
    private val createNotificationAction = spyk(CreateNotificationAction(context))
    private val createNewMessageNavigationIntent = spyk(
        CreateNewMessageNavigationIntent(context, notificationsDeepLinkHelper)
    )

    private val testDispatcher = StandardTestDispatcher()

    private val processNewMessagePushNotification: ProcessNewMessagePushNotification
        get() = ProcessNewMessagePushNotification(
            context,
            notificationProvider,
            notificationManagerCompatProxy,
            getNotificationsExtendedPreference,
            createNewMessageNavigationIntent,
            createNotificationAction
        )

    private val userData = LocalPushNotificationData.UserPushData(UserId(RawUserId), RawUserEmail)
    private val sender = PushNotificationSenderData(
        senderName = "Proton Mail",
        senderAddress = "test@proton.me",
        senderGroup = ""
    )
    private val pushData = LocalPushNotificationData.MessagePushData.NewMessagePushData(
        sender,
        RawMessageId,
        RawContent
    )
    private val newMessageData = LocalPushNotification.Message.NewMessage(userData, pushData)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        initializeCommonMocks()
        notificationProvider.initNotificationChannels()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun processNewMessageNotificationFlowDoesNotFailWorkerIfFails() = runTest {
        // When
        val result = processNewMessagePushNotification(newMessageData)

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun processNewMessageNotificationCreatesIntentsWithCorrectActions() = runTest {
        // Given
        val expectedArchivePayload = PushNotificationPendingIntentPayloadData(
            pushData.messageId.hashCode(),
            userData.userId.id,
            userData.userId.id,
            pushData.messageId,
            LocalNotificationAction.MoveTo.Archive
        )
        val expectedTrashPayload = expectedArchivePayload.copy(action = LocalNotificationAction.MoveTo.Trash)
        val expectedMarkAsReadPayload = expectedArchivePayload.copy(action = LocalNotificationAction.MarkAsRead)

        val dismissNotificationPayload = PushNotificationDismissPendingIntentData.SingleNotification(
            userData.userId.id,
            pushData.messageId.hashCode()
        )
        val dismissGroupPayload = PushNotificationDismissPendingIntentData.GroupNotification(
            userData.userId.id
        )

        // When
        val result = processNewMessagePushNotification(newMessageData)
        advanceUntilIdle()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 1) {
            createNotificationAction(expectedArchivePayload)
            createNotificationAction(expectedTrashPayload)
            createNotificationAction(expectedMarkAsReadPayload)
            createNotificationAction(dismissNotificationPayload)
            createNotificationAction(dismissGroupPayload)
        }

        confirmVerified(createNotificationAction)
    }

    @Test
    fun processNewMessageNotificationShowsNotificationWithActions() = runTest {
        // Given
        val expectedNotificationId = pushData.messageId.hashCode()
        val expectedGroupNotificationId = userData.userId.hashCode()

        val notification = slot<Notification>()
        val groupNotification = slot<Notification>()

        // When
        val result = processNewMessagePushNotification(newMessageData)

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 1) {
            notificationManagerCompatProxy.showNotification(expectedNotificationId, capture(notification))
            notificationManagerCompatProxy.showNotification(expectedGroupNotificationId, capture(groupNotification))
        }

        confirmVerified(notificationManagerCompatProxy)
        verifySingleNotification(notification)
        verifyGroupNotification(groupNotification)
    }

    private fun verifySingleNotification(notification: CapturingSlot<Notification>) {
        with(notification.captured) {
            assertEquals(NotificationChannelId.Email, channelId)
            assertEquals(RawSender, title)
            assertEquals(RawContent, text)
            assertEquals(RawUserEmail, subText)

            assertTrue(actions.isNotEmpty())
            assertEquals("Archive", actions[0].title)
            assertEquals("Trash", actions[1].title)
            assertEquals("Mark read", actions[2].title)
        }
    }

    private fun verifyGroupNotification(notification: CapturingSlot<Notification>) {
        with(notification.captured) {
            assertEquals(NotificationChannelId.Email, channelId)
            assertTrue(actions.isNullOrEmpty())
        }
    }

    private fun getNotificationProvider(): NotificationProvider = NotificationProvider(
        context,
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    )

    private fun initializeCommonMocks() {
        val mockedIntent = Intent(Intent.ACTION_VIEW, Uri.EMPTY, context, this::class.java)
        every { notificationsDeepLinkHelper.buildMessageDeepLinkIntent(any(), any(), any()) } returns mockedIntent
        every { notificationsDeepLinkHelper.buildMessageGroupDeepLinkIntent(any(), any()) } returns mockedIntent
    }

    private companion object {

        const val RawUserId = "userId"
        const val RawUserEmail = "userEmail"
        const val RawSender = "Proton Mail"
        const val RawMessageId = "messageId"
        const val RawContent = "content"
    }
}
