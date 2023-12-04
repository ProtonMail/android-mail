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

package ch.protonmail.android.mailnotifications.data.local

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailnotifications.data.remote.resource.PushNotification
import ch.protonmail.android.mailnotifications.data.remote.resource.PushNotificationData
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailnotifications.domain.usecase.ProcessMessageReadPushNotification
import ch.protonmail.android.mailnotifications.domain.usecase.ProcessNewLoginPushNotification
import ch.protonmail.android.mailnotifications.domain.usecase.ProcessNewMessagePushNotification
import ch.protonmail.android.mailnotifications.domain.usecase.content.DecryptNotificationContent
import ch.protonmail.android.mailsettings.domain.model.BackgroundSyncPreference
import ch.protonmail.android.mailsettings.domain.usecase.notifications.GetExtendedNotificationsSetting
import ch.protonmail.android.mailsettings.domain.usecase.privacy.ObserveBackgroundSyncSetting
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.SessionManager
import me.proton.core.network.domain.session.SessionId
import me.proton.core.user.domain.UserManager
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class ProcessPushNotificationDataWorkerParsingTest {

    private val context = mockk<Context>(relaxUnitFun = true)
    private val sessionManager = mockk<SessionManager>()
    private val decryptNotificationContent = mockk<DecryptNotificationContent>()
    private val appInBackgroundState = mockk<AppInBackgroundState>()
    private val userManager = mockk<UserManager>()
    private val getExtendedNotificationsSetting = mockk<GetExtendedNotificationsSetting>()
    private val processNewMessagePushNotification = mockk<ProcessNewMessagePushNotification>()
    private val observeBackgroundSyncSetting = mockk<ObserveBackgroundSyncSetting>()
    private val processNewLoginPushNotification = mockk<ProcessNewLoginPushNotification>()
    private val processMessageReadPushNotification = mockk<ProcessMessageReadPushNotification>()

    private val params: WorkerParameters = mockk {
        every { taskExecutor } returns mockk(relaxed = true)

        every {
            inputData.getString(ProcessPushNotificationDataWorker.KeyPushNotificationUid)
        } returns RawSessionId

        every {
            inputData.getString(ProcessPushNotificationDataWorker.KeyPushNotificationEncryptedMessage)
        } returns RawNotification
    }

    private val sessionId = SessionId(RawSessionId)
    private val user = UserSample.Primary
    private val userId = user.userId
    private val worker = ProcessPushNotificationDataWorker(
        context,
        params,
        sessionManager,
        decryptNotificationContent,
        appInBackgroundState,
        userManager,
        getExtendedNotificationsSetting,
        observeBackgroundSyncSetting,
        processNewMessagePushNotification,
        processNewLoginPushNotification,
        processMessageReadPushNotification
    )

    @Before
    fun reset() {
        unmockkAll()
    }

    @Test
    fun `null user id makes the worker fail`() = runTest {
        // Given
        coEvery { sessionManager.getUserId(sessionId) } returns null

        // When
        val result = worker.doWork()

        // Then
        assertEquals(FailureResultUnknownUser, result)
    }

    @Test
    fun `null decrypted notification content makes the worker fail`() = runTest {
        // Given
        coEvery { sessionManager.getUserId(sessionId) } returns userId
        coEvery { decryptNotificationContent(userId, RawNotification).getOrNull() } returns null
        coEvery { userManager.getUser(userId) } returns UserSample.Primary

        // When
        val result = worker.doWork()

        // Then
        assertEquals(FailureResultNullDecryptedContent, result)
    }

    @Test
    fun `null push notification data makes the worker fail`() = runTest {
        // Given
        coEvery { sessionManager.getUserId(sessionId) } returns userId
        coEvery {
            decryptNotificationContent(userId, RawNotification)
        } returns DecryptNotificationContent.DecryptedNotification(PushNotification("open_url", 2, null)).right()
        coEvery { userManager.getUser(userId) } returns UserSample.Primary

        // When
        val result = worker.doWork()

        // Then
        assertEquals(FailureResultNullPushNotificationData, result)
    }

    @Test
    fun `unknown notification type does not create a new notification but returns a success`() = runTest {
        // Given
        coEvery { sessionManager.getUserId(sessionId) } returns userId
        coEvery { observeBackgroundSyncSetting() } returns flowOf(BackgroundSyncPreference(true).right())
        coEvery {
            decryptNotificationContent(userId, RawNotification)
        } returns DecryptNotificationContent.DecryptedNotification(
            PushNotification(type = "unknown", version = 2, data = mockk<PushNotificationData>())
        ).right()
        coEvery { userManager.getUser(userId) } returns UserSample.Primary

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
    }

    private companion object {

        const val RawNotification = "notification"
        const val RawSessionId = "sessionId"

        val FailureResultUnknownUser = ListenableWorker.Result.failure(
            workDataOf(
                ProcessPushNotificationDataWorker.KeyProcessPushNotificationDataError to
                    "User is unknown or inactive"
            )
        )

        val FailureResultNullDecryptedContent = ListenableWorker.Result.failure(
            workDataOf(
                ProcessPushNotificationDataWorker.KeyProcessPushNotificationDataError to
                    "Unable to decrypt notification content."
            )
        )

        val FailureResultNullPushNotificationData = ListenableWorker.Result.failure(
            workDataOf(
                ProcessPushNotificationDataWorker.KeyProcessPushNotificationDataError to
                    "Push Notification data is null."
            )
        )
    }
}
