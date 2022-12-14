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

import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.flowTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class RemoveAccountViewModelTest : CoroutinesTest by CoroutinesTest() {

    private val accountManager = mockk<AccountManager>(relaxUnitFun = true) {
        every { this@mockk.getPrimaryUserId() } returns flowOf(UserIdTestData.userId)
    }

    lateinit var viewModel: RemoveAccountViewModel

    @Before
    fun setUp() {
        viewModel = RemoveAccountViewModel(
            accountManager
        )
    }

    @Test
    fun `when initialized emits Initial state`() = runTest {
        // WHEN
        val actual = viewModel.state.take(1).first()
        // THEN
        assertEquals(RemoveAccountViewModel.State.Initial, actual)
    }

    @Test
    fun `when signout emits SigningOut and then SignedOut when completed`() = runTest {
        // WHEN
        viewModel.remove()
        // THEN
        flowTest(viewModel.state) {
            assertEquals(RemoveAccountViewModel.State.Initial, awaitItem())
            assertEquals(RemoveAccountViewModel.State.Removing, awaitItem())
            assertEquals(RemoveAccountViewModel.State.Removed, awaitItem())
            coVerify { accountManager.removeAccount(any()) }
        }
    }
}
