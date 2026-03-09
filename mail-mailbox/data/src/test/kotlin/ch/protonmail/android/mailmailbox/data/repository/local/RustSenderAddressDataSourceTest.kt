/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailmailbox.data.repository.local

import app.cash.turbine.test
import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmailbox.data.local.RustSenderAddressDataSourceImpl
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import uniffi.mail_uniffi.AsyncLiveQueryCallback
import uniffi.mail_uniffi.MailUserSession
import uniffi.mail_uniffi.MailUserSessionHasValidSenderAddressResult
import uniffi.mail_uniffi.MailUserSessionWatchAddressesResult
import uniffi.mail_uniffi.ProtonError
import uniffi.mail_uniffi.WatchHandle
import kotlin.test.Test
import kotlin.test.assertEquals

class RustSenderAddressDataSourceTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val testCoroutineScope = CoroutineScope(mainDispatcherRule.testDispatcher)
    private val mailUserSessionWrapper: MailUserSessionWrapper = mockk()
    private val rustMailUserSession: MailUserSession = mockk()
    private val mockWatchHandle: WatchHandle = mockk(relaxed = true)

    private lateinit var dataSource: RustSenderAddressDataSourceImpl

    private val mockDataError = DataError.Remote.NoNetwork
    private val callbackSlot = slot<AsyncLiveQueryCallback>()

    @Before
    fun setup() {
        dataSource = RustSenderAddressDataSourceImpl(
            coroutineScope = testCoroutineScope,
            ioDispatcher = mainDispatcherRule.testDispatcher
        )

        every { mailUserSessionWrapper.getRustUserSession() } returns rustMailUserSession
        every {
            rustMailUserSession.watchAddresses(capture(callbackSlot))
        } returns MailUserSessionWatchAddressesResult.Ok(mockWatchHandle)

    }

    @Test
    fun `given user session returns OK(true) initially WHEN observing then flow emits Right(true)`() = runTest {
        // given
        coEvery { rustMailUserSession.hasValidSenderAddress() } returns MailUserSessionHasValidSenderAddressResult.Ok(
            true
        )

        // when
        dataSource.observeUserHasValidSenderAddress(mailUserSessionWrapper)
            .test {
                val result = awaitItem()
                assertEquals(Either.Right(true), result)

                cancel()
            }

        // then
        verify { mockWatchHandle.disconnect() }
    }


    @Test
    fun `GIVEN rust session returns Error(dataError) initially WHEN observing then flow emits Left(DataError)`() =
        runTest {
            // given
            coEvery {
                rustMailUserSession.hasValidSenderAddress()
            } returns MailUserSessionHasValidSenderAddressResult.Error(
                ProtonError.Network
            )

            // when
            dataSource.observeUserHasValidSenderAddress(mailUserSessionWrapper)
                .test {
                    // then
                    val result = awaitItem()
                    assertEquals(Either.Left(mockDataError), result)

                    cancel()
                }
        }

    @Test
    fun `given initial Ok(false) WHEN callback updates to OK(true) then flow emits both values`() = runTest {
        // given
        coEvery { rustMailUserSession.hasValidSenderAddress() } returns
            MailUserSessionHasValidSenderAddressResult.Ok(false) andThen
            MailUserSessionHasValidSenderAddressResult.Ok(true)

        // when
        dataSource.observeUserHasValidSenderAddress(mailUserSessionWrapper)
            .test {
                // then
                assertEquals(Either.Right(false), awaitItem())

                val capturedCallback = callbackSlot.captured
                // when Simulate an external update notification
                capturedCallback.onUpdate()

                testScheduler.advanceUntilIdle()

                // then
                assertEquals(Either.Right(true), awaitItem())

                cancel()
            }
    }

    @Test
    fun `given initial Ok(true) WHEN callback updates to Error then flow emits error`() = runTest {
        // given
        coEvery { rustMailUserSession.hasValidSenderAddress() } returns
            MailUserSessionHasValidSenderAddressResult.Ok(true) andThen
            MailUserSessionHasValidSenderAddressResult.Error(ProtonError.Network)

        // given
        dataSource.observeUserHasValidSenderAddress(mailUserSessionWrapper)
            .test {
                // then
                assertEquals(Either.Right(true), awaitItem())

                val capturedCallback = callbackSlot.captured
                // given
                capturedCallback.onUpdate()

                // then
                assertEquals(Either.Left(mockDataError), awaitItem())

                cancel()
            }
    }
}
