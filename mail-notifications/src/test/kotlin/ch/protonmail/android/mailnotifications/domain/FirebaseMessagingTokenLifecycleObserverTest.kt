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

package ch.protonmail.android.mailnotifications.domain

import androidx.lifecycle.LifecycleOwner
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailnotifications.data.FirebaseNotificationsTokenChannel
import ch.protonmail.android.mailnotifications.data.local.RegisterDeviceTokenWorker
import ch.protonmail.android.mailnotifications.data.remote.FirebaseMessagingProxy
import ch.protonmail.android.mailnotifications.data.repository.DeviceRegistrationRepository
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class FirebaseMessagingTokenLifecycleObserverTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()
    private val testCoroutineScope = CoroutineScope(dispatcherRule.testDispatcher)

    private val deviceRegistrationRepository = mockk<DeviceRegistrationRepository>()
    private val enqueuer = mockk<Enqueuer>()
    private val firebaseMessagingProxy = mockk<FirebaseMessagingProxy>()
    private val firebaseNotificationsTokenChannel = mockk<FirebaseNotificationsTokenChannel>()
    private val registerWithWorkerEnabled = mockk<FeatureFlag<Boolean>>()

    private lateinit var observer: FirebaseMessagingTokenLifecycleObserver

    private val lifecycleOwner = mockk<LifecycleOwner>()


    @BeforeTest
    fun setup() {
        observer = FirebaseMessagingTokenLifecycleObserver(
            firebaseMessagingProxy,
            firebaseNotificationsTokenChannel,
            deviceRegistrationRepository,
            enqueuer,
            registerWithWorkerEnabled,
            testCoroutineScope
        )
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `given worker flag enabled, when token received, then enqueues device token registration`() = runTest {
        // Given
        val testToken = "test_token"
        val tokenFlow = MutableSharedFlow<String>()
        val constraints = mockk<Constraints>()

        coEvery { registerWithWorkerEnabled.get() } returns true
        every { firebaseNotificationsTokenChannel.tokenFlow } returns tokenFlow
        coEvery { firebaseMessagingProxy.fetchToken() } returns testToken.right()
        coEvery { firebaseNotificationsTokenChannel.sendToken(any()) } just runs
        every { enqueuer.buildDefaultConstraints() } returns constraints
        every {
            enqueuer.enqueueUniqueWork(any<String>(), any(), any(), any(), any())
        } just runs

        // When
        observer.onCreate(lifecycleOwner)
        tokenFlow.emit(testToken)

        advanceUntilIdle()

        // Then
        coVerify { firebaseNotificationsTokenChannel.sendToken(testToken) }
        verify {
            enqueuer.enqueueUniqueWork(
                workerId = RegisterDeviceTokenWorker.UniqueWorkerId,
                worker = RegisterDeviceTokenWorker::class.java,
                existingWorkPolicy = ExistingWorkPolicy.REPLACE,
                constraints = constraints,
                params = RegisterDeviceTokenWorker.params(testToken)
            )
        }
        coVerify(exactly = 0) { deviceRegistrationRepository.registerDeviceToken(any()) }
    }

    @Test
    fun `given worker flag disabled, when token received, then registers device token directly`() = runTest {
        // Given
        val testToken = "test_token"
        val tokenFlow = MutableSharedFlow<String>()

        coEvery { registerWithWorkerEnabled.get() } returns false
        every { firebaseNotificationsTokenChannel.tokenFlow } returns tokenFlow
        coEvery { firebaseMessagingProxy.fetchToken() } returns testToken.right()
        coEvery { firebaseNotificationsTokenChannel.sendToken(any()) } just runs
        coEvery { deviceRegistrationRepository.registerDeviceToken(testToken) } returns Unit.right()

        // When
        observer.onCreate(lifecycleOwner)
        tokenFlow.emit(testToken)

        advanceUntilIdle()

        // Then
        coVerify { firebaseNotificationsTokenChannel.sendToken(testToken) }
        coVerify { deviceRegistrationRepository.registerDeviceToken(testToken) }
        verify(exactly = 0) {
            enqueuer.enqueueUniqueWork(any<String>(), any(), any(), any(), any())
        }
    }

    @Test
    fun `should not register if token can't be fetched`() = runTest {
        // Given
        val tokenFlow = MutableSharedFlow<String>()

        every { firebaseNotificationsTokenChannel.tokenFlow } returns tokenFlow
        coEvery { firebaseMessagingProxy.fetchToken() } returns DataError.Remote.Unknown.left()

        // When
        observer.onCreate(lifecycleOwner)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { firebaseNotificationsTokenChannel.sendToken(any()) }
        verify(exactly = 0) {
            enqueuer.enqueueUniqueWork(any<String>(), any(), any(), any(), any())
        }
        coVerify(exactly = 0) { deviceRegistrationRepository.registerDeviceToken(any()) }
    }
}
