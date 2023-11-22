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

package ch.protonmail.android.mailsettings.domain.usecase

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.sample.AccountSample
import ch.protonmail.android.mailsettings.domain.repository.LocalStorageDataRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

internal class ObserveOverallLocalStorageUsageTest {

    private val localStorageDataRepository = mockk<LocalStorageDataRepository>()
    private val accountManager = mockk<AccountManager>()
    private val observeOverallLocalStorageUsage =
        ObserveOverallLocalStorageUsage(accountManager, localStorageDataRepository)

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return the combined size of message data and attachment data when invoked`() = runTest {
        // Given
        every { accountManager.getAccounts() } returns flowOf(listOf(BaseAccount))
        every { localStorageDataRepository.observeMessageDataTotalRawSize() } returns flowOf(MessageDataSize)
        coEvery {
            localStorageDataRepository.getAttachmentDataSizeForUserId(BaseAccount.userId)
        } returns AttachmentsDataSize

        // When + Then
        observeOverallLocalStorageUsage().test {
            assertEquals(MessageDataSize + AttachmentsDataSize, awaitItem().value)
            awaitComplete()
        }
    }

    @Test
    fun `should return the combined size of message data and attachment data for all user ids when invoked`() =
        runTest {
            // Given
            every { accountManager.getAccounts() } returns flowOf(listOf(BaseAccount, SecondaryAccount))
            every { localStorageDataRepository.observeMessageDataTotalRawSize() } returns flowOf(MessageDataSize)
            coEvery {
                localStorageDataRepository.getAttachmentDataSizeForUserId(BaseAccount.userId)
            } returns AttachmentsDataSize
            coEvery {
                localStorageDataRepository.getAttachmentDataSizeForUserId(SecondaryAccount.userId)
            } returns SecondaryAttachmentsDataSize

            // When + Then
            observeOverallLocalStorageUsage().test {
                assertEquals(MessageDataSize + AttachmentsDataSize + SecondaryAttachmentsDataSize, awaitItem().value)
                awaitComplete()
            }
        }

    private companion object {

        val BaseAccount = AccountSample.Primary
        val SecondaryAccount = AccountSample.Primary.copy(userId = UserId("secondaryId"))
        const val MessageDataSize: Long = 100L
        const val AttachmentsDataSize: Long = 400L
        const val SecondaryAttachmentsDataSize: Long = 600L
    }
}
