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

package ch.protonmail.android.mailnotifications

import java.time.Instant
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkerParameters
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcommon.presentation.system.NotificationProvider
import ch.protonmail.android.mailcommon.presentation.system.NotificationProvider.Companion.LOGIN_CHANNEL_ID
import ch.protonmail.android.mailnotifications.PushNotificationSample.getSampleLoginAlertNotification
import ch.protonmail.android.mailnotifications.data.local.ProcessPushNotificationDataWorker
import ch.protonmail.android.mailnotifications.domain.AppInBackgroundState
import ch.protonmail.android.mailnotifications.domain.NotificationsDeepLinkHelper
import ch.protonmail.android.mailnotifications.domain.proxy.NotificationManagerCompatProxy
import ch.protonmail.android.mailnotifications.domain.usecase.content.DecryptNotificationContent
import ch.protonmail.android.test.annotations.suite.SmokeTest
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.SessionManager
import me.proton.core.user.domain.UserManager
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@SmokeTest
internal class ProcessPushNotificationDataWorkerLoginTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val sessionManager = mockk<SessionManager>()
    private val decryptNotificationContent = mockk<DecryptNotificationContent>()
    private val appInBackgroundState = mockk<AppInBackgroundState>()
    private val notificationProvider = getNotificationProvider()
    private val userManager = mockk<UserManager>()
    private val notificationsDeepLinkHelper = mockk<NotificationsDeepLinkHelper>()
    private val notificationManagerCompatProxy = mockk<NotificationManagerCompatProxy>(relaxUnitFun = true)

    private val params: WorkerParameters = mockk {
        every { taskExecutor } returns mockk(relaxed = true)
        every { inputData.getString(ProcessPushNotificationDataWorker.KeyPushNotificationUid) } returns RawSessionId
        every { inputData.getString(ProcessPushNotificationDataWorker.KeyPushNotificationEncryptedMessage) } returns RawNotification
    }

    private val user = UserSample.Primary
    private val userId = user.userId
    private val worker = ProcessPushNotificationDataWorker(
        context,
        params,
        sessionManager,
        decryptNotificationContent,
        appInBackgroundState,
        notificationProvider,
        userManager,
        notificationsDeepLinkHelper,
        notificationManagerCompatProxy
    )

    private val baseLoginNotification = DecryptNotificationContent.DecryptedNotification(
        getSampleLoginAlertNotification()
    ).right()

    @Before
    fun setup() {
        mockkStatic(Instant::class)
        notificationProvider.initNotificationChannels() // Do not rely on initializers.
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun newLoginNotificationDataCreatesNewPushNotificationWhenAppIsInBackground() = runTest {
        // Given
        prepareSharedMocks(isAppInBackground = true)

        val expectedOpenUrlNotificationGroupId = "${userId.id}-openurl".hashCode()
        val expectedOpenUrlNotificationEntryId = Instant.now().hashCode()
        val notification = slot<Notification>()
        val groupNotification = slot<Notification>()

        // When
        worker.doWork()

        // Then
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

    @Test
    fun newLoginNotificationDataCreatesNewPushNotificationWhenAppIsInForeground() = runTest {
        // Given
        prepareSharedMocks(isAppInBackground = false)

        val expectedOpenUrlNotificationGroupId = "${userId.id}-openurl".hashCode()
        val expectedOpenUrlNotificationEntryId = Instant.now().hashCode()
        val notification = slot<Notification>()
        val groupNotification = slot<Notification>()

        // When
        worker.doWork()

        // Then
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

    private fun prepareSharedMocks(isAppInBackground: Boolean) {
        coEvery { sessionManager.getUserId(any()) } returns userId
        coEvery { decryptNotificationContent(any(), any()) } returns baseLoginNotification
        coEvery { userManager.getUser(any()) } returns UserSample.Primary
        coEvery { appInBackgroundState.isAppInBackground() } returns isAppInBackground
        every { Instant.now() } returns mockk { every { epochSecond } returns 123 }
    }

    private fun verifySingleNotification(notification: CapturingSlot<Notification>) {
        with(notification.captured) {
            assertEquals(LOGIN_CHANNEL_ID, channelId)
            assertTrue(actions.isNullOrEmpty())
            assertEquals(NotificationSingleTitle, title)
            assertEquals(NotificationSingleText, text)
            assertEquals(NotificationSingleText, bigText)
            assertEquals(NotificationSingleSummaryText, summaryText)
        }
    }

    private fun verifyGroupNotification(notification: CapturingSlot<Notification>) {
        with(notification.captured) {
            assertEquals(LOGIN_CHANNEL_ID, channelId)
            assertTrue(actions.isNullOrEmpty())
            assertEquals(NotificationGroupTitle, title)
            assertEquals(NotificationGroupText, text)
            assertEquals(NotificationGroupText, bigText)
            assertEquals(NotificationGroupSummaryText, summaryText)
        }
    }

    private fun getNotificationProvider(): NotificationProvider = NotificationProvider(
        context,
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    )

    private companion object {

        const val RawNotification = "notification"
        const val RawSessionId = "sessionId"

        const val NotificationSingleTitle = "Proton Mail"
        const val NotificationSingleText = "New login attempt"
        const val NotificationSingleSummaryText = "primary-email@pm.me"

        const val NotificationGroupTitle = "Proton Mail"
        const val NotificationGroupText = "New login alerts"
        const val NotificationGroupSummaryText = "primary-email@pm.me"
    }
}
