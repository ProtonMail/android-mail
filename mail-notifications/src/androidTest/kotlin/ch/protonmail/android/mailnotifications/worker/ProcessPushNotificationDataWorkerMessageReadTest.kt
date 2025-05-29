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

package ch.protonmail.android.mailnotifications.worker

import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailnotifications.PushNotificationSample
import ch.protonmail.android.mailnotifications.data.local.ProcessPushNotificationDataWorker
import ch.protonmail.android.mailnotifications.domain.model.LocalPushNotificationData
import ch.protonmail.android.mailnotifications.domain.model.MessageReadPushData
import ch.protonmail.android.mailnotifications.domain.model.UserPushData
import ch.protonmail.android.mailnotifications.domain.usecase.ProcessMessageReadPushNotification
import ch.protonmail.android.mailnotifications.domain.usecase.ProcessNewLoginPushNotification
import ch.protonmail.android.mailnotifications.domain.usecase.ProcessNewMessagePushNotification
import ch.protonmail.android.mailnotifications.domain.usecase.content.DecryptNotificationContent
import ch.protonmail.android.mailsettings.domain.model.BackgroundSyncPreference
import ch.protonmail.android.mailsettings.domain.usecase.notifications.GetExtendedNotificationsSetting
import ch.protonmail.android.mailsettings.domain.usecase.privacy.ObserveBackgroundSyncSetting
import ch.protonmail.android.test.annotations.suite.SmokeTest
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.SessionManager
import me.proton.core.user.domain.UserManager
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals

@SmokeTest
@SdkSuppress(maxSdkVersion = 33)
internal class ProcessPushNotificationDataWorkerMessageReadTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val sessionManager = mockk<SessionManager>()
    private val decryptNotificationContent = mockk<DecryptNotificationContent>()
    private val appInBackgroundState = mockk<AppInBackgroundState>()
    private val userManager = mockk<UserManager>()
    private val getNotificationsExtendedPreference = mockk<GetExtendedNotificationsSetting>()
    private val observeBackgroundSyncSetting = mockk<ObserveBackgroundSyncSetting>()
    private val processNewMessagePushNotification = mockk<ProcessNewMessagePushNotification>(relaxUnitFun = true)
    private val processNewLoginPushNotification = mockk<ProcessNewLoginPushNotification>(relaxUnitFun = true)
    private val processMessageReadPushNotification = mockk<ProcessMessageReadPushNotification>(relaxUnitFun = true)

    private val params: WorkerParameters = mockk {
        every { taskExecutor } returns mockk(relaxed = true)
        every { inputData.getString(ProcessPushNotificationDataWorker.KeyPushNotificationUid) } returns RawSessionId
        every {
            inputData.getString(ProcessPushNotificationDataWorker.KeyPushNotificationEncryptedMessage)
        } returns RawNotification
    }

    private val user = UserSample.Primary
    private val userId = user.userId
    private val worker = ProcessPushNotificationDataWorker(
        context,
        params,
        sessionManager,
        decryptNotificationContent,
        appInBackgroundState,
        userManager,
        getNotificationsExtendedPreference,
        observeBackgroundSyncSetting,
        processNewMessagePushNotification,
        processNewLoginPushNotification,
        processMessageReadPushNotification
    )

    private val baseMessageReadNotification = DecryptNotificationContent.DecryptedNotification(
        PushNotificationSample.getSampleMessageReadNotification()
    ).right()

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun newMessageReadNotificationDismissesNotificationWhenAppIsInBackground() = runTest {
        // Given
        prepareSharedMocks(isAppInBackground = true)

        val userData = UserPushData(userId.id, "")
        val pushData = MessageReadPushData("messageId")
        val messageReadPushNotificationData = LocalPushNotificationData.MessageRead(userData, pushData)

        // When
        val result = worker.doWork()

        // Then
        verify(exactly = 1) {
            processMessageReadPushNotification.invoke(messageReadPushNotificationData)
        }
        confirmVerified(processMessageReadPushNotification)
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun newMessageReadNotificationDismissesNotificationWhenAppIsInForeground() = runTest {
        // Given
        prepareSharedMocks(isAppInBackground = false)
        val userData = UserPushData(userId.id, "")
        val pushData = MessageReadPushData("messageId")
        val messageReadPushNotificationData = LocalPushNotificationData.MessageRead(userData, pushData)

        // When
        val result = worker.doWork()

        // Then
        verify(exactly = 1) {
            processMessageReadPushNotification.invoke(messageReadPushNotificationData)
        }
        confirmVerified(processMessageReadPushNotification)
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun newMessageReadNotificationIsProcessedWhenBackgroundSyncIsDisabled() = runTest {
        // Given
        prepareSharedMocks(isAppInBackground = false, hasBackgroundSyncEnabled = false)
        val userData = UserPushData(userId.id, "")
        val pushData = MessageReadPushData("messageId")
        val messageReadPushNotificationData = LocalPushNotificationData.MessageRead(userData, pushData)

        // When
        val result = worker.doWork()

        // Then
        verify(exactly = 1) {
            processMessageReadPushNotification.invoke(messageReadPushNotificationData)
        }
        confirmVerified(processMessageReadPushNotification)
        assertEquals(ListenableWorker.Result.success(), result)
    }

    private fun prepareSharedMocks(isAppInBackground: Boolean, hasBackgroundSyncEnabled: Boolean = true) {
        coEvery { sessionManager.getUserId(any()) } returns userId
        coEvery { decryptNotificationContent(any(), any()) } returns baseMessageReadNotification
        coEvery { userManager.getUser(any()) } returns UserSample.Primary
        every { appInBackgroundState.isAppInBackground() } returns isAppInBackground
        coEvery {
            observeBackgroundSyncSetting()
        } returns flowOf(BackgroundSyncPreference(hasBackgroundSyncEnabled).right())
        every { processMessageReadPushNotification.invoke(any()) } returns ListenableWorker.Result.success()
    }

    private companion object {

        const val RawNotification = "notification"
        const val RawSessionId = "sessionId"
    }
}
