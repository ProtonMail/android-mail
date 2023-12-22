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

package ch.protonmail.android.mailsettings.presentation.settings.autolock.usecase

import ch.protonmail.android.mailcommon.domain.sample.AccountSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import org.junit.Test

internal class ClearPinDataAndForceLogoutTest {

    private val accountManager = mockk<AccountManager>(relaxUnitFun = true)
    private val resetAutoLockDefaults = mockk<ResetAutoLockDefaults>()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val scope = CoroutineScope(testDispatcher)

    private val clearPinDataAndForceLogout = ClearPinDataAndForceLogout(
        accountManager, resetAutoLockDefaults, scope
    )

    @Test
    fun `should disable all accounts and reset to auto lock defaults when invoked`() = runTest {
        // Given
        val firstAccount = AccountSample.Primary.copy(userId = UserId("one"))
        val secondAccount = AccountSample.Primary.copy(userId = UserId("two"))

        coEvery {
            accountManager.getAccounts()
        } returns flowOf(listOf(firstAccount, secondAccount))

        coEvery {
            accountManager.disableAccount(firstAccount.userId)
            accountManager.disableAccount(secondAccount.userId)
        } just runs

        coEvery { resetAutoLockDefaults() } just runs

        // When
        clearPinDataAndForceLogout()

        // Then
        coVerify(exactly = 1) {
            accountManager.disableAccount(firstAccount.userId)
            accountManager.disableAccount(secondAccount.userId)
            resetAutoLockDefaults()
        }
    }
}
