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

package ch.protonmail.android.feature.account

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class RemoveAccountViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(TestDispatcherProvider().Main)

    private val accountManager = mockk<AccountManager>(relaxUnitFun = true) {
        every { this@mockk.getPrimaryUserId() } returns flowOf(UserIdTestData.Primary)
    }
    private val enqueuer = mockk<Enqueuer>()

    private val viewModel = RemoveAccountViewModel(accountManager, enqueuer)

    @Test
    fun `when initialized emits Initial state`() = runTest {
        // When
        val actual = viewModel.state.take(1).first()

        // Then
        assertEquals(RemoveAccountViewModel.State.Initial, actual)
    }

    @Test
    fun `when remove is called emits Removing and then Removed when completed`() = runTest {
        // Given
        every { enqueuer.cancelAllWork(UserIdTestData.Primary) } just Runs

        // When
        viewModel.remove()

        // Then
        viewModel.state.test {
            assertEquals(RemoveAccountViewModel.State.Initial, awaitItem())
            assertEquals(RemoveAccountViewModel.State.Removing, awaitItem())
            assertEquals(RemoveAccountViewModel.State.Removed, awaitItem())
            coVerify { accountManager.removeAccount(any()) }
        }
    }

    @Test
    fun `when remove is called cancel all work related to this user`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        every { enqueuer.cancelAllWork(userId) } just Runs

        // When
        viewModel.remove(userId)

        // Then
        viewModel.state.test {
            assertEquals(RemoveAccountViewModel.State.Initial, awaitItem())
            assertEquals(RemoveAccountViewModel.State.Removing, awaitItem())
            coVerify { enqueuer.cancelAllWork(userId) }
            coVerify { accountManager.removeAccount(any()) }
            assertEquals(RemoveAccountViewModel.State.Removed, awaitItem())
        }
    }
}
