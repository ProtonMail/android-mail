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

package ch.protonmail.android.debug.environment

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.data.usecase.SignOutAllAccounts
import ch.protonmail.android.mailsession.domain.model.Account
import ch.protonmail.android.mailsession.domain.model.AccountState
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

internal class EnvironmentDebugViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val store = mockk<DebugEnvironmentStore>(relaxed = true) {
        every { host() } returns null
        every { effectiveHost() } returns "proton.black"
        every { defaultHost() } returns "proton.black"
        every { recentHosts() } returns emptyList()
    }
    private val userSessionRepository = mockk<UserSessionRepository> {
        every { observeAccounts() } returns flowOf(emptyList())
    }
    private val signOutAllAccounts = mockk<SignOutAllAccounts> {
        coEvery { this@mockk.invoke() } returns Unit.right()
    }

    private fun viewModel() = EnvironmentDebugViewModel(store, userSessionRepository, signOutAllAccounts)

    private fun givenAccount(state: AccountState) {
        every { userSessionRepository.observeAccounts() } returns flowOf(
            listOf(Account(UserId("user"), name = "user", state = state, primaryAddress = "user@proton.black"))
        )
    }

    private fun givenOverride(host: String) {
        every { store.host() } returns host
        every { store.effectiveHost() } returns host
    }

    @Test
    fun `given a new environment, when saving it, then it is stored for the next start`() = runTest {
        // Given
        coEvery { store.save("maury.proton.black") } returns listOf("maury.proton.black")
        val viewModel = viewModel()

        // When
        viewModel.save("https://account.maury.proton.black/login")

        // Then
        coVerify { store.save("maury.proton.black") }
        assertEquals(
            EnvironmentSelection("maury.proton.black", isOverridden = true, recentHosts = listOf("maury.proton.black")),
            viewModel.selection.value
        )
        assertEquals("proton.black", viewModel.runningHost)
        assertEquals(EnvironmentSwitchState.Done, viewModel.switchState.value)
    }

    @Test
    fun `given the chosen environment, when saving it again, then nothing changes`() = runTest {
        // Given
        givenOverride("maury.proton.black")

        // When
        viewModel().save("maury.proton.black")

        // Then
        coVerify(exactly = 0) { store.save(any()) }
    }

    @Test
    fun `given invalid input, when saving it, then nothing changes`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.save("staging.protonmail.dev")

        // Then
        coVerify(exactly = 0) { store.save(any()) }
        coVerify(exactly = 0) { store.clear() }
        assertEquals("proton.black", viewModel.selection.value.host)
    }

    @Test
    fun `given an override, when saving the default environment, then the override is cleared`() = runTest {
        // Given
        givenOverride("maury.proton.black")
        val viewModel = viewModel()

        // When
        viewModel.save("proton.black")

        // Then
        coVerify { store.clear() }
        assertEquals("proton.black", viewModel.selection.value.host)
        assertEquals(false, viewModel.selection.value.isOverridden)
    }

    @Test
    fun `given an alpha build on production, when saving a proton black environment, then it is stored`() = runTest {
        // Given
        every { store.defaultHost() } returns "proton.me"
        every { store.effectiveHost() } returns "proton.me"
        coEvery { store.save("proton.black") } returns listOf("proton.black")
        val viewModel = viewModel()

        // When
        viewModel.save("proton.black")

        // Then
        coVerify { store.save("proton.black") }
        assertEquals("proton.black", viewModel.selection.value.host)
        assertEquals(true, viewModel.selection.value.isOverridden)
    }

    @Test
    fun `given an alpha build with an override, when saving production, then the override is cleared`() = runTest {
        // Given
        every { store.defaultHost() } returns "proton.me"
        givenOverride("maury.proton.black")
        val viewModel = viewModel()

        // When
        viewModel.save("proton.me")

        // Then
        coVerify { store.clear() }
        coVerify(exactly = 0) { store.save(any()) }
    }

    @Test
    fun `given no override, when saving the default environment, then nothing changes`() = runTest {
        // When
        viewModel().save("")

        // Then
        coVerify(exactly = 0) { store.clear() }
    }

    @Test
    fun `given an override, when resetting, then the build's environment is chosen and kept as recent`() = runTest {
        // Given
        givenOverride("maury.proton.black")
        every { store.recentHosts() } returns listOf("maury.proton.black")
        val viewModel = viewModel()

        // When
        viewModel.reset()

        // Then
        coVerify { store.clear() }
        assertEquals(
            EnvironmentSelection("proton.black", isOverridden = false, recentHosts = listOf("maury.proton.black")),
            viewModel.selection.value
        )
    }

    @Test
    fun `given a signed in account, when switching environment, then it is signed out before the switch is stored`() =
        runTest {
            // Given
            givenAccount(AccountState.Ready)
            coEvery { store.save("maury.proton.black") } returns listOf("maury.proton.black")

            // When
            viewModel().save("maury.proton.black")

            // Then
            coVerifyOrder {
                signOutAllAccounts()
                store.save("maury.proton.black")
            }
        }

    @Test
    fun `given a signed in account, when resetting, then it is signed out`() = runTest {
        // Given
        givenOverride("maury.proton.black")
        givenAccount(AccountState.Ready)

        // When
        viewModel().reset()

        // Then
        coVerify { signOutAllAccounts() }
    }

    @Test
    fun `given only signed out accounts, when switching environment, then nobody is signed out`() = runTest {
        // Given
        givenAccount(AccountState.Disabled)
        coEvery { store.save("maury.proton.black") } returns listOf("maury.proton.black")

        // When
        viewModel().save("maury.proton.black")

        // Then
        coVerify(exactly = 0) { signOutAllAccounts() }
    }

    @Test
    fun `given a signed in account, when saving the chosen environment again, then nobody is signed out`() = runTest {
        // Given
        givenOverride("maury.proton.black")
        givenAccount(AccountState.Ready)

        // When
        viewModel().save("maury.proton.black")

        // Then
        coVerify(exactly = 0) { signOutAllAccounts() }
    }

    @Test
    fun `given the sign out fails, when switching environment, then the switch is not stored`() = runTest {
        // Given
        givenAccount(AccountState.Ready)
        coEvery { signOutAllAccounts() } returns DataError.Local.Unknown.left()
        val viewModel = viewModel()

        // When
        viewModel.save("maury.proton.black")

        // Then
        coVerify(exactly = 0) { store.save(any()) }
        assertEquals("proton.black", viewModel.selection.value.host)
        assertEquals(EnvironmentSwitchState.Failed, viewModel.switchState.value)
    }

    @Test
    fun `given the sign out fails, when resetting, then the override is kept`() = runTest {
        // Given
        givenOverride("maury.proton.black")
        givenAccount(AccountState.Ready)
        coEvery { signOutAllAccounts() } returns DataError.Local.Unknown.left()
        val viewModel = viewModel()

        // When
        viewModel.reset()

        // Then
        coVerify(exactly = 0) { store.clear() }
        assertEquals(true, viewModel.selection.value.isOverridden)
        assertEquals(EnvironmentSwitchState.Failed, viewModel.switchState.value)
    }

    @Test
    fun `given a failed switch, when the dialog closes, then the next switch starts afresh`() = runTest {
        // Given
        givenAccount(AccountState.Ready)
        coEvery { signOutAllAccounts() } returns DataError.Local.Unknown.left()
        val viewModel = viewModel()
        viewModel.save("maury.proton.black")

        // When
        viewModel.onDialogClosed()

        // Then
        assertEquals(EnvironmentSwitchState.Idle, viewModel.switchState.value)
    }
}
