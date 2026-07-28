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
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.usecase.SetPrimaryAccount
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.proton.android.core.auth.presentation.AuthOrchestrator
import me.proton.android.core.auth.presentation.login.LoginInput
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
class LegacyAccountApiProviderTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val orchestrator = mockk<AuthOrchestrator>(relaxUnitFun = true)
    private val setPrimaryAccount = mockk<SetPrimaryAccount>(relaxUnitFun = true)
    private val userSessionRepository = mockk<UserSessionRepository>(relaxed = true)

    private val provider = LegacyAccountApiProvider(
        orchestrator = orchestrator,
        setPrimaryAccount = setPrimaryAccount,
        userSessionRepository = userSessionRepository
    )

    @AfterTest
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `unregister delegates to the orchestrator`() {
        provider.unregister()

        verify(exactly = 1) { orchestrator.unregister() }
    }

    @Test
    fun `startAddAccount starts the add account workflow`() {
        provider.startAddAccount()

        verify(exactly = 1) { orchestrator.startAddAccountWorkflow() }
    }

    @Test
    fun `startSignIn starts the login workflow with the given username`() {
        provider.startSignIn("user@proton.me")

        verify(exactly = 1) { orchestrator.startLoginWorkflow(LoginInput(username = "user@proton.me")) }
    }

    @Test
    fun `startSignIn without a username starts the login workflow with a null username`() {
        provider.startSignIn(null)

        verify(exactly = 1) { orchestrator.startLoginWorkflow(LoginInput(username = null)) }
    }

    @Test
    fun `startSignUp starts the sign up workflow`() {
        provider.startSignUp()

        verify(exactly = 1) { orchestrator.startSignUpWorkflow() }
    }

    @Test
    fun `switchAccount sets the given account as primary`() = runTest(mainDispatcherRule.testDispatcher) {
        val userId = UserIdSample.Primary

        provider.switchAccount(userId)

        coVerify(exactly = 1) { setPrimaryAccount(userId) }
    }
}
