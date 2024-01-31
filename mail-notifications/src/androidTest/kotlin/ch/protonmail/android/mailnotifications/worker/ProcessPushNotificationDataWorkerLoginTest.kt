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

import java.time.Instant
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailnotifications.PushNotificationSample.getSampleLoginAlertNotification
import ch.protonmail.android.mailnotifications.data.local.ProcessPushNotificationDataWorker
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailnotifications.domain.model.LocalPushNotificationData
import ch.protonmail.android.mailnotifications.domain.model.NewLoginPushData
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
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.SessionManager
import me.proton.core.user.domain.UserManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@SmokeTest
@SdkSuppress(maxSdkVersion = 33)
class ProcessPushNotificationDataWorkerLoginTest {

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

    private val baseLoginNotification = DecryptNotificationContent.DecryptedNotification(
        getSampleLoginAlertNotification()
    ).right()

    @Before
    fun setup() {
        mockkStatic(Instant::class)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun newLoginNotificationDataCreatesNewPushNotificationWhenAppIsInBackground() = runTest {
        // Given
        prepareSharedMocks(isAppInBackground = true)

        val userData = UserPushData("primary", "primary-email@pm.me")
        val pushData = NewLoginPushData("Proton Mail", "New login attempt", "")
        val loginNotification = LocalPushNotificationData.Login(userData, pushData)

        // When
        val result = worker.doWork()

        // Then
        verify(exactly = 1) {
            processNewLoginPushNotification.invoke(loginNotification)
        }

        confirmVerified(processNewLoginPushNotification)
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun newLoginNotificationDataCreatesNewPushNotificationWhenAppIsInForeground() = runTest {
        // Given
        prepareSharedMocks(isAppInBackground = false)

        val userData = UserPushData("primary", "primary-email@pm.me")
        val pushData = NewLoginPushData("Proton Mail", "New login attempt", "")
        val loginNotification = LocalPushNotificationData.Login(userData, pushData)

        // When
        val result = worker.doWork()

        // Then
        verify(exactly = 1) {
            processNewLoginPushNotification.invoke(loginNotification)
        }

        confirmVerified(processNewLoginPushNotification)
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun newLoginNotificationDataCreatesNewPushNotificationWhenAppIsInForegroundAndBackgroundSyncIsDisabled() = runTest {
        // Given
        prepareSharedMocks(isAppInBackground = false, hasBackgroundSyncEnabled = false)

        val userData = UserPushData("primary", "primary-email@pm.me")
        val pushData = NewLoginPushData("Proton Mail", "New login attempt", "")
        val loginNotification = LocalPushNotificationData.Login(userData, pushData)

        // When
        val result = worker.doWork()

        // Then
        verify(exactly = 1) {
            processNewLoginPushNotification.invoke(loginNotification)
        }

        confirmVerified(processNewLoginPushNotification)
        assertEquals(ListenableWorker.Result.success(), result)
    }

    private fun prepareSharedMocks(isAppInBackground: Boolean, hasBackgroundSyncEnabled: Boolean = true) {
        coEvery { sessionManager.getUserId(any()) } returns userId
        coEvery { decryptNotificationContent(any(), any()) } returns baseLoginNotification
        coEvery { userManager.getUser(any()) } returns UserSample.Primary
        every { appInBackgroundState.isAppInBackground() } returns isAppInBackground
        coEvery {
            observeBackgroundSyncSetting()
        } returns flowOf(BackgroundSyncPreference(hasBackgroundSyncEnabled).right())
        every { processNewLoginPushNotification.invoke(any()) } returns ListenableWorker.Result.success()
        every { Instant.now() } returns mockk { every { epochSecond } returns 123 }
    }

    private companion object {

        const val RawNotification = "notification"
        const val RawSessionId = "sessionId"
    }
}
