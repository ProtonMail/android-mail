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

package ch.protonmail.upselling.domain.usecase

import ch.protonmail.android.mailupselling.domain.usecase.UserHasPendingPurchases
import ch.protonmail.upselling.domain.UpsellingTestData.FakeAcknowledgedPurchase
import ch.protonmail.upselling.domain.UpsellingTestData.FakeCancelledPurchase
import ch.protonmail.upselling.domain.UpsellingTestData.FakeDeletedPurchase
import ch.protonmail.upselling.domain.UpsellingTestData.FakeFailedPurchase
import ch.protonmail.upselling.domain.UpsellingTestData.FakePendingPurchase
import ch.protonmail.upselling.domain.UpsellingTestData.FakePurchasedPurchase
import ch.protonmail.upselling.domain.UpsellingTestData.FakeSubscribedPurchase
import ch.protonmail.upselling.domain.UpsellingTestData.userId
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.SessionManager
import me.proton.core.network.domain.session.SessionId
import me.proton.core.payment.domain.entity.Purchase
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class UserHasPendingPurchasesTest {

    private val sessionManager = mockk<SessionManager>()
    private val userHasPendingPurchases = UserHasPendingPurchases(sessionManager)

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return false when purchases list is empty`() = runTest {
        // Given
        val purchases = emptyList<Purchase>()

        // When
        val actual = userHasPendingPurchases(purchases, userId)

        // Then
        assertFalse(actual)
    }

    @Test
    fun `should return false when sessionId cannot be determined`() = runTest {
        // Given
        val purchases = listOf(FakeAcknowledgedPurchase)
        coEvery { sessionManager.getSessionId(userId) } returns null

        // When
        val actual = userHasPendingPurchases(purchases, userId)

        // Then
        assertFalse(actual)
    }

    @Test
    fun `should return false when there are no purchased or subscribed state purchases`() = runTest {
        // Given
        val purchases = listOf(
            FakeAcknowledgedPurchase,
            FakeFailedPurchase,
            FakePendingPurchase,
            FakeCancelledPurchase,
            FakeDeletedPurchase
        )

        coEvery { sessionManager.getSessionId(userId) } returns SessionId(userId.id)

        // When
        val actual = userHasPendingPurchases(purchases, userId)

        // Then
        assertFalse(actual)
    }

    @Test
    fun `should return true when there are purchased state purchases`() = runTest {
        // Given
        val purchases = listOf(FakePurchasedPurchase)
        coEvery { sessionManager.getSessionId(userId) } returns SessionId(userId.id)

        // When
        val actual = userHasPendingPurchases(purchases, userId)

        // Then
        assertTrue(actual)
    }

    @Test
    fun `should return true when there are subscribed state purchases`() = runTest {
        // Given
        val purchases = listOf(FakeSubscribedPurchase)
        coEvery { sessionManager.getSessionId(userId) } returns SessionId(userId.id)

        // When
        val actual = userHasPendingPurchases(purchases, userId)

        // Then
        assertTrue(actual)
    }
}
