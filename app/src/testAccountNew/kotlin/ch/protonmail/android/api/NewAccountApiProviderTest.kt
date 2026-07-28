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

package ch.protonmail.android.api

import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.proton.android.account.api.ProtonAccountApi
import me.proton.android.account.types.ProtonUserId
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
class NewAccountApiProviderTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accountApi = mockk<ProtonAccountApi>(relaxUnitFun = true)

    private val provider = NewAccountApiProvider(accountApi = accountApi)

    @AfterTest
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `startSignIn launches the SDK sign in`() {
        provider.startSignIn("user@proton.me")

        verify(exactly = 1) { accountApi.launchSignIn() }
    }

    @Test
    fun `startSignUp launches the SDK sign up`() {
        provider.startSignUp()

        verify(exactly = 1) { accountApi.launchSignUp() }
    }

    @Test
    fun `deleteAccount removes the account from the SDK`() = runTest(mainDispatcherRule.testDispatcher) {
        val userId = UserIdSample.Primary

        provider.deleteAccount(userId)

        verify(exactly = 1) { accountApi.removeAccount(ProtonUserId(userId.id)) }
    }

    @Test
    fun `switchAccount sets the current account on the SDK`() = runTest(mainDispatcherRule.testDispatcher) {
        val userId = UserIdSample.Primary

        provider.switchAccount(userId)

        verify(exactly = 1) { accountApi.setCurrentAccount(ProtonUserId(userId.id)) }
    }

    @Test
    fun `register does not interact with the SDK`() = runTest(mainDispatcherRule.testDispatcher) {
        provider.register(mockk(relaxed = true))
        provider.unregister()
        provider.startAddAccount()

        confirmVerified(accountApi)
    }
}
