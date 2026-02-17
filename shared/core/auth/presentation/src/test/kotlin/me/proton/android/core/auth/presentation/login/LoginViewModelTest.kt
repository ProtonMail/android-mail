/*
 * Copyright (C) 2024 Proton AG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.core.auth.presentation.login

import android.content.Context
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.proton.android.core.auth.presentation.flow.FlowCache
import me.proton.android.core.auth.presentation.rules.MainDispatcherRule
import org.junit.Rule
import org.junit.Test
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailSessionNewLoginFlowResult

internal class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = mockk<Context>(relaxed = true)
    private val sessionInterface = mockk<MailSession>()
    private val flowCache = mockk<FlowCache>(relaxUnitFun = true)

    @Test
    fun `should clear flow cache when login flow is created`() = runTest {
        // Given
        val loginFlowResult = mockk<MailSessionNewLoginFlowResult.Ok>()
        coEvery { sessionInterface.newLoginFlow() } returns loginFlowResult

        // When
        LoginViewModel(
            context = context,
            sessionInterface = sessionInterface,
            flowCache = flowCache,
            ioDispatcher = mainDispatcherRule.testDispatcher
        )

        // Then
        verify { flowCache.clear() }
    }
}
