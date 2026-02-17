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

package ch.protonmail.android.mailevents.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailevents.domain.model.AppEvent
import ch.protonmail.android.mailevents.domain.repository.EventsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

internal class TrackSubscriptionEventTest {

    private val eventsRepository = mockk<EventsRepository>()

    private val trackSubscriptionEvent = TrackSubscriptionEvent(eventsRepository)

    @Test
    fun `should send Subscription event with all parameters`() = runTest {
        // Given
        val contentList = listOf("mail-plus", "vpn-plus")
        val price = 9.99
        val currency = "USD"
        val cycle = 12
        val couponCode = "PROMO2026"
        val transactionId = "txn_123456"
        val isFirstPurchase = true
        val isFreeToPaid = true

        coEvery { eventsRepository.sendEvent(any()) } returns Unit.right()

        // When
        val result = trackSubscriptionEvent(
            contentList = contentList,
            price = price,
            currency = currency,
            cycle = cycle,
            couponCode = couponCode,
            transactionId = transactionId,
            isFirstPurchase = isFirstPurchase,
            isFreeToPaid = isFreeToPaid
        )

        // Then
        assertTrue(result.isRight())
        coVerify {
            eventsRepository.sendEvent(
                AppEvent.Subscription(
                    contentList = contentList,
                    price = price,
                    currency = currency,
                    cycle = cycle,
                    couponCode = couponCode,
                    transactionId = transactionId,
                    isFirstPurchase = isFirstPurchase,
                    isFreeToPaid = isFreeToPaid
                )
            )
        }
    }

    @Test
    fun `should send Subscription event without optional parameters`() = runTest {
        // Given
        val contentList = listOf("mail-plus")
        val price = 4.99
        val currency = "EUR"
        val cycle = 1
        val isFirstPurchase = false
        val isFreeToPaid = false

        coEvery { eventsRepository.sendEvent(any()) } returns Unit.right()

        // When
        val result = trackSubscriptionEvent(
            contentList = contentList,
            price = price,
            currency = currency,
            cycle = cycle,
            couponCode = null,
            transactionId = null,
            isFirstPurchase = isFirstPurchase,
            isFreeToPaid = isFreeToPaid
        )

        // Then
        assertTrue(result.isRight())
        coVerify {
            eventsRepository.sendEvent(
                AppEvent.Subscription(
                    contentList = contentList,
                    price = price,
                    currency = currency,
                    cycle = cycle,
                    couponCode = null,
                    transactionId = null,
                    isFirstPurchase = isFirstPurchase,
                    isFreeToPaid = isFreeToPaid
                )
            )
        }
    }

    @Test
    fun `should send Subscription event for first purchase`() = runTest {
        // Given
        val contentList = listOf("mail-plus")
        coEvery { eventsRepository.sendEvent(any()) } returns Unit.right()

        // When
        val result = trackSubscriptionEvent(
            contentList = contentList,
            price = 9.99,
            currency = "USD",
            cycle = 12,
            isFirstPurchase = true,
            isFreeToPaid = false
        )

        // Then
        assertTrue(result.isRight())
        coVerify {
            eventsRepository.sendEvent(
                match {
                    it is AppEvent.Subscription && it.isFirstPurchase && !it.isFreeToPaid
                }
            )
        }
    }

    @Test
    fun `should send Subscription event for free to paid conversion`() = runTest {
        // Given
        val contentList = listOf("mail-plus")
        coEvery { eventsRepository.sendEvent(any()) } returns Unit.right()

        // When
        val result = trackSubscriptionEvent(
            contentList = contentList,
            price = 9.99,
            currency = "USD",
            cycle = 12,
            isFirstPurchase = false,
            isFreeToPaid = true
        )

        // Then
        assertTrue(result.isRight())
        coVerify {
            eventsRepository.sendEvent(
                match {
                    it is AppEvent.Subscription && !it.isFirstPurchase && it.isFreeToPaid
                }
            )
        }
    }

    @Test
    fun `should return error when repository fails to send event`() = runTest {
        // Given
        val error = DataError.Remote.Unknown
        val contentList = listOf("mail-plus")
        coEvery { eventsRepository.sendEvent(any()) } returns error.left()

        // When
        val result = trackSubscriptionEvent(
            contentList = contentList,
            price = 9.99,
            currency = "USD",
            cycle = 12,
            isFirstPurchase = true,
            isFreeToPaid = false
        )

        // Then
        assertTrue(result.isLeft())
    }

    @Test
    fun `should send Subscription event with multiple content items`() = runTest {
        // Given
        val contentList = listOf("mail-plus", "vpn-plus", "drive-plus", "pass-plus")
        coEvery { eventsRepository.sendEvent(any()) } returns Unit.right()

        // When
        val result = trackSubscriptionEvent(
            contentList = contentList,
            price = 29.99,
            currency = "USD",
            cycle = 12,
            isFirstPurchase = true,
            isFreeToPaid = true
        )

        // Then
        assertTrue(result.isRight())
        coVerify {
            eventsRepository.sendEvent(
                match {
                    it is AppEvent.Subscription && it.contentList.size == 4
                }
            )
        }
    }
}
