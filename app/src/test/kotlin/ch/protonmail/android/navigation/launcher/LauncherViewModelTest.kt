/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.navigation.launcher

import ch.protonmail.android.navigation.launcher.PrimaryAccountState.SignedIn
import ch.protonmail.android.navigation.launcher.PrimaryAccountState.SigningIn
import ch.protonmail.android.testdata.AccountTestData
import ch.protonmail.android.testdata.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.getPrimaryAccount
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class LauncherViewModelTest {

    private val accountManager = mockk<AccountManager> {
        every { getPrimaryAccount() } returns flowOf(null)
    }

    private lateinit var launcherViewModel: LauncherViewModel

    @Before
    fun setUp() {
        mockkStatic(AccountManager::getPrimaryAccount)
        launcherViewModel = LauncherViewModel(
            accountManager
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(AccountManager::getPrimaryAccount)
    }

    @Test
    fun `view state is SignedIn when a user is signed in`() = runBlockingTest {
        every { accountManager.getPrimaryAccount() } returns flowOf(AccountTestData.readyAccount)

        val actual = launcherViewModel.viewState().take(1).toList().first()

        val expected = LauncherViewState(SignedIn(UserIdTestData.userId))
        assertEquals(expected, actual)
    }

    @Test
    fun `view state is SigningIn when no user is signed in`() = runBlockingTest {
        val actual = launcherViewModel.viewState().take(1).toList().first()

        val expected = LauncherViewState(SigningIn)
        assertEquals(expected, actual)
    }
}
