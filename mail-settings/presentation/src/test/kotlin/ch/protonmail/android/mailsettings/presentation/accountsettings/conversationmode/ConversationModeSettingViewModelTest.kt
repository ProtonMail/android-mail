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

package ch.protonmail.android.mailsettings.presentation.accountsettings.conversationmode

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailSettings
import ch.protonmail.android.mailsettings.presentation.accountsettings.conversationmode.ConversationModeSettingState.Data
import ch.protonmail.android.mailsettings.presentation.accountsettings.conversationmode.ConversationModeSettingState.Loading
import ch.protonmail.android.testdata.mailsettings.MailSettingsTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.entity.ViewMode.NoConversationGrouping
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConversationModeSettingViewModelTest {

    private val accountManager = mockk<AccountManager> {
        every { getPrimaryUserId() } returns flowOf(userId)
    }

    private val mailSettingsFlow = MutableSharedFlow<MailSettings?>()
    private val observeMailSettings = mockk<ObserveMailSettings> {
        every { this@mockk(userId) } returns mailSettingsFlow
    }

    private val mailSettingsRepository = mockk<MailSettingsRepository> {
        coEvery { updateViewMode(any(), any()) } returns MailSettingsTestData.mailSettings
    }

    private val viewModel by lazy {
        ConversationModeSettingViewModel(
            accountManager,
            mailSettingsRepository,
            observeMailSettings
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `state has conversation mode flag when use case returns a valid mail settings`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()

            // When
            mailSettingsFlow.emit(MailSettingsTestData.mailSettings)

            // Then
            val actual = awaitItem() as Data
            assertEquals(false, actual.isEnabled)
        }
    }

    @Test
    fun `state has null conversation mode when use case returns invalid mail settings`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()

            // When
            mailSettingsFlow.emit(null)

            // Then
            val actual = awaitItem() as Data
            assertNull(actual.isEnabled)
        }
    }

    @Test
    fun `emits right state when no user is logged in`() = runTest {
        // given
        givenNoLoggedInUser()

        // when
        viewModel.state.test {

            // then
            assertEquals(ConversationModeSettingState.NotLoggedIn, awaitItem())
        }
    }

    @Test
    fun `conversation mode preference is updated on mailSettings when onConversationToggled`() = runTest {
        viewModel.onConversationToggled(false)

        coVerify {
            mailSettingsRepository.updateViewMode(
                userId,
                NoConversationGrouping
            )
        }
    }

    private fun givenNoLoggedInUser() {
        every { accountManager.getPrimaryUserId() } returns flowOf(null)
    }

    private suspend fun ReceiveTurbine<ConversationModeSettingState>.initialStateEmitted() {
        awaitItem() as Loading
    }

}
