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
import ch.protonmail.android.mailcommon.presentation.system.NotificationProvider
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailnotifications.domain.NotificationsDeepLinkHelper
import ch.protonmail.android.mailnotifications.domain.model.LocalNotificationAction
import ch.protonmail.android.mailnotifications.domain.model.LocalPushNotificationData
import ch.protonmail.android.mailnotifications.domain.model.NewMessagePushData
import ch.protonmail.android.mailnotifications.domain.model.PushNotificationPendingIntentPayloadData
import ch.protonmail.android.mailnotifications.domain.model.UserPushData
import ch.protonmail.android.mailnotifications.domain.proxy.NotificationManagerCompatProxy
import ch.protonmail.android.mailnotifications.domain.usecase.ProcessNewMessagePushNotification
import ch.protonmail.android.mailnotifications.domain.usecase.actions.CreateNotificationAction
import ch.protonmail.android.mailnotifications.domain.usecase.intents.CreateNewMessageNavigationIntent
import ch.protonmail.android.mailnotifications.subText
import ch.protonmail.android.mailnotifications.text
import ch.protonmail.android.mailnotifications.title
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
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
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
    private val messageRepository = mockk<MessageRepository>()
    private val notificationsDeepLinkHelper = mockk<NotificationsDeepLinkHelper>()
    private val createNotificationAction = spyk(CreateNotificationAction(context, notificationsDeepLinkHelper))
    private val createNewMessageNavigationIntent = spyk(
        CreateNewMessageNavigationIntent(context, notificationsDeepLinkHelper)
    )

    private val processNewMessagePushNotification = ProcessNewMessagePushNotification(
        context,
        messageRepository,
        notificationProvider,
        notificationManagerCompatProxy,
        createNewMessageNavigationIntent,
        createNotificationAction,
        TestScope()
    )

    private val userData = UserPushData(UserId, UserEmail)
    private val pushData = NewMessagePushData(Sender, MessageId, Content)
    private val newMessageData = LocalPushNotificationData.NewMessage(userData, pushData)

    @Before
    fun setup() {
        initializeCommonMocks()
        notificationProvider.initNotificationChannels()
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun processNewMessageNotificationFlowDoesNotFailWorkerIfFails() = runTest {
        // Given
        coEvery { messageRepository.getRefreshedMessageWithBody(any(), any()) } returns null

        // When
        val result = processNewMessagePushNotification(newMessageData)

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun processNewMessageNotificationCreatesIntentsWithCorrectActions() = runTest {
        // Given
        coEvery { messageRepository.getRefreshedMessageWithBody(any(), any()) } returns null
        val expectedArchivePayload = PushNotificationPendingIntentPayloadData(
            pushData.messageId.hashCode(),
            userData.userId,
            userData.userId,
            pushData.messageId,
            LocalNotificationAction.MoveTo.Archive
        )
        val expectedTrashPayload = expectedArchivePayload.copy(action = LocalNotificationAction.MoveTo.Trash)
        val expectedReplyPayload = expectedArchivePayload.copy(action = LocalNotificationAction.Reply)

        // When
        val result = processNewMessagePushNotification(newMessageData)

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 1) {
            createNotificationAction(expectedArchivePayload)
            createNotificationAction(expectedTrashPayload)
            createNotificationAction(expectedReplyPayload)
        }

        confirmVerified(createNotificationAction)
    }

    @Test
    fun processNewMessageNotificationShowsNotificationWithActions() = runTest {
        // Given
        coEvery { messageRepository.getRefreshedMessageWithBody(any(), any()) } returns null
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
            assertEquals(NotificationProvider.EMAIL_CHANNEL_ID, channelId)
            assertEquals(Sender, title)
            assertEquals(Content, text)
            assertEquals(UserEmail, subText)

            assertTrue(actions.isNotEmpty())
            assertEquals("Archive", actions[0].title)
            assertEquals("Trash", actions[1].title)
            assertEquals("Reply", actions[2].title)
        }
    }

    private fun verifyGroupNotification(notification: CapturingSlot<Notification>) {
        with(notification.captured) {
            assertEquals(NotificationProvider.EMAIL_CHANNEL_ID, channelId)
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
        every { notificationsDeepLinkHelper.buildReplyToDeepLinkIntent(any(), any()) } returns mockedIntent
    }

    private companion object {

        const val UserId = "userId"
        const val UserEmail = "userEmail"
        const val Sender = "sender"
        const val MessageId = "messageId"
        const val Content = "content"
    }
}
