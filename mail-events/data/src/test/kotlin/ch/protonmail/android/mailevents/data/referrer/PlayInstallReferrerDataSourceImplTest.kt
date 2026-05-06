/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailevents.data.referrer

import android.content.Context
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailevents.domain.model.InstallReferrer
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

internal class PlayInstallReferrerDataSourceImplTest {

    private val context = mockk<Context>()
    private val builder = mockk<InstallReferrerClient.Builder>()
    private val client = mockk<InstallReferrerClient>(relaxUnitFun = true)
    private val listenerSlot = slot<InstallReferrerStateListener>()

    private val testDispatcher = StandardTestDispatcher()
    private val dataSource = PlayInstallReferrerDataSourceImpl(context, testDispatcher)

    @BeforeTest
    fun setUp() {
        mockkStatic(InstallReferrerClient::class)
        every { InstallReferrerClient.newBuilder(context) } returns builder
        every { builder.build() } returns client
        every { client.startConnection(capture(listenerSlot)) } just Runs
        every { client.endConnection() } just Runs
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(InstallReferrerClient::class)
    }

    @Test
    fun `should return install referrer when response is OK`() = runTest(testDispatcher) {
        // Given
        val details = mockk<ReferrerDetails> {
            every { installReferrer } returns "utm_source=test"
            every { referrerClickTimestampSeconds } returns 1_000L
            every { installBeginTimestampSeconds } returns 2_000L
            every { googlePlayInstantParam } returns false
        }
        every { client.installReferrer } returns details
        every { client.startConnection(capture(listenerSlot)) } answers {
            listenerSlot.captured.onInstallReferrerSetupFinished(InstallReferrerClient.InstallReferrerResponse.OK)
        }

        // When
        val result = dataSource.getInstallReferrer()

        // Then
        val expected = InstallReferrer(
            referrerUrl = "utm_source=test",
            referrerClickTimestampMs = 1_000_000L,
            installBeginTimestampMs = 2_000_000L,
            isGooglePlayInstant = false
        )
        assertEquals(expected.right(), result)
        verify { client.endConnection() }
    }

    @Test
    fun `should return error when feature is not supported`() = runTest(testDispatcher) {
        // Given
        every { client.startConnection(capture(listenerSlot)) } answers {
            listenerSlot.captured.onInstallReferrerSetupFinished(
                InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED
            )
        }

        // When
        val result = dataSource.getInstallReferrer()

        // Then
        assertEquals(DataError.Local.Unknown.left(), result)
        verify { client.endConnection() }
    }

    @Test
    fun `should return error when service is unavailable`() = runTest(testDispatcher) {
        // Given
        every { client.startConnection(capture(listenerSlot)) } answers {
            listenerSlot.captured.onInstallReferrerSetupFinished(
                InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE
            )
        }

        // When
        val result = dataSource.getInstallReferrer()

        // Then
        assertEquals(DataError.Local.Unknown.left(), result)
        verify { client.endConnection() }
    }

    @Test
    fun `should return error for unknown response code`() = runTest(testDispatcher) {
        // Given
        every { client.startConnection(capture(listenerSlot)) } answers {
            listenerSlot.captured.onInstallReferrerSetupFinished(42)
        }

        // When
        val result = dataSource.getInstallReferrer()

        // Then
        assertEquals(DataError.Local.Unknown.left(), result)
        verify { client.endConnection() }
    }

    @Test
    fun `should return error when reading install referrer throws`() = runTest(testDispatcher) {
        // Given
        every { client.installReferrer } throws RuntimeException("exception")
        every { client.startConnection(capture(listenerSlot)) } answers {
            listenerSlot.captured.onInstallReferrerSetupFinished(InstallReferrerClient.InstallReferrerResponse.OK)
        }

        // When
        val result = dataSource.getInstallReferrer()

        // Then
        assertEquals(DataError.Local.Unknown.left(), result)
        verify { client.endConnection() }
    }

    @Test
    fun `should return error when connection times out`() = runTest(testDispatcher) {
        // Given - startConnection never invokes the listener
        every { client.startConnection(any()) } just Runs

        // When
        val deferred = async { dataSource.getInstallReferrer() }
        advanceTimeBy(11_000L.milliseconds)
        val result = deferred.await()

        // Then
        assertEquals(DataError.Local.Unknown.left(), result)
        verify { client.endConnection() }
    }
}
