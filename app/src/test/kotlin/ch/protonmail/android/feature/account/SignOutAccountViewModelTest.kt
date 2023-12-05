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
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class SignOutAccountViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(TestDispatcherProvider().Main)

    private val accountManager = mockk<AccountManager>(relaxUnitFun = true)
    private val enqueuer = mockk<Enqueuer>()
    private val viewModel = SignOutAccountViewModel(accountManager, enqueuer)

    @Before
    fun setup() {
        every { accountManager.getPrimaryUserId() } returns flowOf(BaseUserId)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `when initialized emits initial state`() = runTest {
        // When
        val actual = viewModel.state.take(1).first()

        // Then
        assertEquals(SignOutAccountViewModel.State.Initial, actual)
    }

    @Test
    fun `when sign out is called emits signing out and then signed out when completed`() = runTest {
        // Given
        every { enqueuer.cancelAllWork(BaseUserId) } just Runs

        // When
        viewModel.signOut()

        // Then
        viewModel.state.test {
            assertEquals(SignOutAccountViewModel.State.Initial, awaitItem())
            assertEquals(SignOutAccountViewModel.State.SigningOut, awaitItem())
            assertEquals(SignOutAccountViewModel.State.SignedOut, awaitItem())

            coVerify { accountManager.disableAccount(BaseUserId) }
            coVerify(exactly = 0) { accountManager.removeAccount(any()) }
        }
    }

    @Test
    fun `when sign out is called cancel all work related to this user`() = runTest {
        // Given
        every { enqueuer.cancelAllWork(BaseUserId) } just Runs

        // When
        viewModel.signOut(BaseUserId)

        // Then
        viewModel.state.test {
            assertEquals(SignOutAccountViewModel.State.Initial, awaitItem())
            assertEquals(SignOutAccountViewModel.State.SigningOut, awaitItem())
            assertEquals(SignOutAccountViewModel.State.SignedOut, awaitItem())

            coVerify {
                enqueuer.cancelAllWork(BaseUserId)
                accountManager.disableAccount(BaseUserId)
            }
            coVerify(exactly = 0) { accountManager.removeAccount(any()) }
        }
    }

    private companion object {

        val BaseUserId = UserIdTestData.Primary
    }
}
