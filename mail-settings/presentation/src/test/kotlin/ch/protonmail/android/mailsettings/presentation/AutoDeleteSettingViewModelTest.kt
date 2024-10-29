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

package ch.protonmail.android.mailsettings.presentation

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import ch.protonmail.android.mailcommon.presentation.model.DialogState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailsettings.data.usecase.UpdateAutoDeleteSpamAndTrashDays
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailSettings
import ch.protonmail.android.mailsettings.presentation.accountsettings.autodelete.AutoDeleteSettingState
import ch.protonmail.android.mailsettings.presentation.accountsettings.autodelete.AutoDeleteSettingViewModel
import ch.protonmail.android.mailsettings.presentation.accountsettings.autodelete.AutoDeleteViewAction
import ch.protonmail.android.testdata.mailsettings.MailSettingsTestData.mailSettings
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
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AutoDeleteSettingViewModelTest {

    private val accountManager = mockk<AccountManager> {
        every { getPrimaryUserId() } returns flowOf(userId)
    }

    private val mailSettingsFlow = MutableSharedFlow<MailSettings?>()
    private val observeMailSettings = mockk<ObserveMailSettings> {
        every { this@mockk(userId) } returns mailSettingsFlow
    }

    private val mailSettingsRepository = mockk<MailSettingsRepository> {
        coEvery { updateViewMode(any(), any()) } returns mailSettings
    }

    private val updateAutoDeleteSpamAndTrashDays = UpdateAutoDeleteSpamAndTrashDays(mailSettingsRepository)

    private val viewModel by lazy {
        AutoDeleteSettingViewModel(
            accountManager,
            updateAutoDeleteSpamAndTrashDays,
            observeMailSettings
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `state has correct auto-delete setting when mail setting has value equal null`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()

            // When
            mailSettingsFlow.emit(
                mailSettings.copy(
                    autoDeleteSpamAndTrashDays = null
                )
            )

            // Then
            val actual = awaitItem() as AutoDeleteSettingState.Data
            assertFalse(actual.isEnabled)
        }
    }

    @Test
    fun `state has correct auto-delete setting when mail setting has value equal 0`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()

            // When
            mailSettingsFlow.emit(
                mailSettings.copy(
                    autoDeleteSpamAndTrashDays = 0
                )
            )

            // Then
            val actual = awaitItem() as AutoDeleteSettingState.Data
            assertFalse(actual.isEnabled)
        }
    }

    @Test
    fun `state has correct auto-delete setting when mail setting has value greater than 0`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()

            // When
            mailSettingsFlow.emit(
                mailSettings.copy(
                    autoDeleteSpamAndTrashDays = 666
                )
            )

            // Then
            val actual = awaitItem() as AutoDeleteSettingState.Data
            kotlin.test.assertTrue(actual.isEnabled)
        }
    }

    @Test
    fun `auto-delete preference is updated on mailSettings when activation is confirmed`() = runTest {
        // Given
        expectRepositorySettingsUpdate(30)

        viewModel.state.test {
            initialStateEmitted()

            // When
            mailSettingsFlow.emit(
                mailSettings.copy(
                    autoDeleteSpamAndTrashDays = null
                )
            )

            // Then
            viewModel.submit(AutoDeleteViewAction.ActivationConfirmed)
            awaitItem()

            coVerify {
                mailSettingsRepository.updateAutoDeleteSpamAndTrashDays(
                    userId,
                    30
                )
            }
        }
    }

    @Test
    fun `auto-delete preference is updated on mailSettings when deactivation is confirmed`() = runTest {
        // Given
        expectRepositorySettingsUpdate(0)

        viewModel.state.test {
            initialStateEmitted()

            // When
            mailSettingsFlow.emit(
                mailSettings.copy(
                    autoDeleteSpamAndTrashDays = null
                )
            )

            // Then
            viewModel.submit(AutoDeleteViewAction.DeactivationConfirmed)
            awaitItem()

            coVerify {
                mailSettingsRepository.updateAutoDeleteSpamAndTrashDays(
                    userId,
                    0
                )
            }
        }
    }

    @Test
    fun `auto-delete activation dialog is shown when activation is requested`() = runTest {
        // Given
        val expectedEnablingDialogState = DialogState.Shown(
            title = TextUiModel(R.string.mail_settings_auto_delete_dialog_enabling_title),
            message = TextUiModel(R.string.mail_settings_auto_delete_dialog_enabling_text),
            dismissButtonText = TextUiModel(R.string.mail_settings_auto_delete_dialog_button_cancel),
            confirmButtonText = TextUiModel(
                R.string.mail_settings_auto_delete_dialog_enabling_button_confirm
            )
        )

        viewModel.state.test {
            initialStateEmitted()

            // When
            mailSettingsFlow.emit(
                mailSettings.copy(
                    autoDeleteSpamAndTrashDays = null
                )
            )

            awaitItem()

            // Then
            viewModel.submit(AutoDeleteViewAction.ActivationRequested)
            val actual = awaitItem() as AutoDeleteSettingState.Data

            assertEquals(expectedEnablingDialogState, actual.enablingDialogState)
            assertEquals(DialogState.Hidden, actual.disablingDialogState)
        }
    }

    @Test
    fun `auto-delete deactivation dialog is shown when deactivation is requested`() = runTest {
        // Given
        val expectedDisablingDialogState = DialogState.Shown(
            title = TextUiModel(R.string.mail_settings_auto_delete_dialog_disabling_title),
            message = TextUiModel(R.string.mail_settings_auto_delete_dialog_disabling_text),
            dismissButtonText = TextUiModel(R.string.mail_settings_auto_delete_dialog_button_cancel),
            confirmButtonText = TextUiModel(
                R.string.mail_settings_auto_delete_dialog_disabling_button_confirm
            )
        )

        viewModel.state.test {
            initialStateEmitted()

            // When
            mailSettingsFlow.emit(
                mailSettings.copy(
                    autoDeleteSpamAndTrashDays = null
                )
            )

            awaitItem()

            // Then
            viewModel.submit(AutoDeleteViewAction.DeactivationRequested)
            val actual = awaitItem() as AutoDeleteSettingState.Data

            assertEquals(expectedDisablingDialogState, actual.disablingDialogState)
            assertEquals(DialogState.Hidden, actual.enablingDialogState)
        }
    }

    @Test
    fun `auto-delete de-activation dialogs are hidden when dismissed`() = runTest {
        // Given
        viewModel.state.test {
            initialStateEmitted()

            // When
            mailSettingsFlow.emit(
                mailSettings.copy(
                    autoDeleteSpamAndTrashDays = null
                )
            )

            awaitItem()

            viewModel.submit(AutoDeleteViewAction.ActivationRequested)
            awaitItem()

            viewModel.submit(AutoDeleteViewAction.DeactivationRequested)
            awaitItem()

            // Then
            viewModel.submit(AutoDeleteViewAction.DialogDismissed)
            val actual = awaitItem() as AutoDeleteSettingState.Data

            assertEquals(DialogState.Hidden, actual.enablingDialogState)
            assertEquals(DialogState.Hidden, actual.disablingDialogState)
        }
    }

    private fun expectRepositorySettingsUpdate(value: Int) {
        coEvery { mailSettingsRepository.updateAutoDeleteSpamAndTrashDays(userId, value) } returns mailSettings.copy(
            autoDeleteSpamAndTrashDays = value
        )
    }

    private suspend fun ReceiveTurbine<AutoDeleteSettingState>.initialStateEmitted() {
        awaitItem() as AutoDeleteSettingState.Loading
    }
}
