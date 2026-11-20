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

import androidx.compose.ui.graphics.Color
import app.cash.turbine.test
import arrow.core.Either
import ch.protonmail.android.design.compose.model.VisibilityUiModel
import ch.protonmail.android.mailbyoe.domain.model.ByoeTelemetryEvent
import ch.protonmail.android.mailbyoe.domain.model.GmailConnectionSettingsAccess
import ch.protonmail.android.mailbyoe.domain.usecase.GetGmailConnectionSettingsAccess
import ch.protonmail.android.mailbyoe.domain.usecase.ReportByoeTelemetryEvent
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchFeatureEnabled
import ch.protonmail.android.mailsession.domain.model.Account
import ch.protonmail.android.mailsession.domain.model.AccountAvatarInfo
import ch.protonmail.android.mailsession.domain.model.AccountState
import ch.protonmail.android.mailsession.domain.model.Percent
import ch.protonmail.android.mailsession.domain.model.Storage
import ch.protonmail.android.mailsession.domain.model.StorageUnit
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryAccount
import ch.protonmail.android.mailsession.presentation.mapper.AccountInformationMapper
import ch.protonmail.android.mailsession.presentation.model.AccountInformationUiModel
import ch.protonmail.android.mailsession.presentation.model.StorageQuotaUiModel
import ch.protonmail.android.mailsettings.domain.model.StorageQuota
import ch.protonmail.android.mailsettings.domain.usecase.ObserveStorageQuotaUseCase
import ch.protonmail.android.mailsettings.presentation.settings.SettingsState
import ch.protonmail.android.mailsettings.presentation.settings.SettingsState.Loading
import ch.protonmail.android.mailsettings.presentation.settings.SettingsViewModel
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.mailspotlight.domain.model.ByoeEntryPoint
import ch.protonmail.android.mailspotlight.domain.model.FeatureEntryPoint
import ch.protonmail.android.mailspotlight.domain.usecase.ObserveFeatureEntryPointEnabled
import kotlinx.coroutines.flow.MutableStateFlow
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accountFlow = MutableSharedFlow<Account?>()

    private val observePrimaryAccount = mockk<ObservePrimaryAccount> {
        coEvery { this@mockk.invoke() } returns accountFlow
    }

    private val observeStorageQuotaUseCase = mockk<ObserveStorageQuotaUseCase> {
        every { this@mockk.invoke() } returns flowOf(
            Either.Right(
                StorageQuota(
                    usagePercent = Percent(30.0),
                    maxStorage = Storage(1, StorageUnit.GiB),
                    isAboveAlertThreshold = false
                )
            )
        )
    }

    private val appInformation = AppInformation(appVersionName = "6.0.0-alpha")
    private val accountInformationMapper = AccountInformationMapper(ColorMapper())

    private val isContentSearchFeatureEnabled = mockk<IsContentSearchFeatureEnabled> {
        coEvery { this@mockk.invoke(any()) } returns true
    }

    private val getGmailConnectionSettingsAccess = mockk<GetGmailConnectionSettingsAccess> {
        coEvery { this@mockk.invoke(any()) } returns GmailConnectionSettingsAccess.Hidden
    }
    private val isByoeSettingsEnabled = MutableStateFlow(true)
    private val observeFeatureEntryPointEnabled = mockk<ObserveFeatureEntryPointEnabled> {
        every { this@mockk(any(), FeatureEntryPoint.Byoe(ByoeEntryPoint.Settings)) } returns isByoeSettingsEnabled
    }

    private val reportByoeTelemetryEvent = mockk<ReportByoeTelemetryEvent>(relaxUnitFun = true)

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {

        viewModel = SettingsViewModel(
            appInformation = appInformation,
            observePrimaryAccount = observePrimaryAccount,
            observeStorageQuotaUseCase = observeStorageQuotaUseCase,
            accountInformationMapper = accountInformationMapper,
            isContentSearchFeatureEnabled = isContentSearchFeatureEnabled,
            getGmailConnectionSettingsAccess = getGmailConnectionSettingsAccess,
            observeFeatureEntryPointEnabled = observeFeatureEntryPointEnabled,
            reportByoeTelemetryEvent = reportByoeTelemetryEvent
        )
    }

    @Test
    fun `emits loading state when initialised`() = runTest {
        // Given
        viewModel.state.test {
            assertEquals(Loading, awaitItem())
        }
    }

    @Test
    fun `state has account info when there is a valid primary user`() = runTest {
        // Given
        val accountUiModel = AccountInformationUiModel(
            userId = UserId("123"),
            name = "Display Name",
            email = "primary@example.com",
            avatarUiModel = AvatarUiModel.ParticipantAvatar(
                initial = "D",
                address = "primary@example.com",
                bimiSelector = null,
                color = Color(0xFFFF5733)
            )
        )
        val storageQuotaUiModel: VisibilityUiModel<StorageQuotaUiModel> = VisibilityUiModel.Visible(
            StorageQuotaUiModel(
                usagePercent = Percent(30.0),
                maxStorage = "1 GB",
                isAboveAlertThreshold = false
            )
        )

        // When
        viewModel.state.test {
            assertEquals(Loading, awaitItem()) // Initial state
            accountFlow.emit(account)

            // Then
            val actual = awaitItem() as SettingsState.Data
            assertEquals(accountUiModel, actual.accountInfoUiModel)
            assertEquals(storageQuotaUiModel, actual.storageQuotaUiModel)
            assertEquals(appInformation, actual.appInformation)
        }
    }

    @Test
    fun `state has null account info when there is no valid primary account`() = runTest {
        viewModel.state.test {
            assertEquals(Loading, awaitItem())
            accountFlow.emit(null)

            // Wait for the state to update with null account info
            val actual = awaitItem() as SettingsState.Data
            assertNull(actual.accountInfoUiModel)
            assertEquals(appInformation, actual.appInformation)
        }
    }

    @Test
    fun `given the user can connect a Gmail account, when observed, then Connect your Gmail is shown`() = runTest {
        // Given
        coEvery { getGmailConnectionSettingsAccess(account.userId) } returns GmailConnectionSettingsAccess.Full

        // When
        viewModel.state.test {
            assertEquals(Loading, awaitItem())
            accountFlow.emit(account)

            // Then
            assertTrue((awaitItem() as SettingsState.Data).isConnectGmailVisible)
        }
    }

    @Test
    fun `given the feature gate hides the BYOE settings, when observed, then Connect your Gmail is hidden`() = runTest {
        // Given
        isByoeSettingsEnabled.value = false
        coEvery { getGmailConnectionSettingsAccess(account.userId) } returns GmailConnectionSettingsAccess.Full

        // When
        viewModel.state.test {
            assertEquals(Loading, awaitItem())
            accountFlow.emit(account)

            // Then
            assertFalse((awaitItem() as SettingsState.Data).isConnectGmailVisible)
        }
    }

    @Test
    fun `given only the address list is allowed, when observed, then Connect your Gmail is hidden`() = runTest {
        // Given
        coEvery { getGmailConnectionSettingsAccess(account.userId) } returns GmailConnectionSettingsAccess.ListOnly

        // When
        viewModel.state.test {
            assertEquals(Loading, awaitItem())
            accountFlow.emit(account)

            // Then
            assertFalse((awaitItem() as SettingsState.Data).isConnectGmailVisible)
        }
    }

    @Test
    fun `given the SDK hides the Gmail connection, when observed, then Connect your Gmail is hidden`() = runTest {
        // Given
        coEvery { getGmailConnectionSettingsAccess(account.userId) } returns GmailConnectionSettingsAccess.Hidden

        // When
        viewModel.state.test {
            assertEquals(Loading, awaitItem())
            accountFlow.emit(account)

            // Then
            assertFalse((awaitItem() as SettingsState.Data).isConnectGmailVisible)
        }
    }

    @Test
    fun `given no primary account, when observed, then Connect your Gmail is hidden`() = runTest {
        // When
        viewModel.state.test {
            assertEquals(Loading, awaitItem())
            accountFlow.emit(null)

            // Then
            assertFalse((awaitItem() as SettingsState.Data).isConnectGmailVisible)
        }
    }

    @Test
    fun `given Connect your Gmail is shown, when the screen shows, then the settings shown and view are reported`() =
        runTest {
            // Given
            coEvery { getGmailConnectionSettingsAccess(account.userId) } returns GmailConnectionSettingsAccess.Full

            viewModel.state.test {
                skipItems(1)
                accountFlow.emit(account)
                awaitItem()

                // When
                viewModel.onConnectGmailShown()

                // Then
                verify(exactly = 1) { reportByoeTelemetryEvent(ByoeTelemetryEvent.SettingsShown, account.userId) }
                verify(exactly = 1) { reportByoeTelemetryEvent(ByoeTelemetryEvent.SettingsView, account.userId) }
            }
        }

    @Test
    fun `given Connect your Gmail is hidden, when the screen shows, then nothing is reported`() = runTest {
        // Given
        coEvery { getGmailConnectionSettingsAccess(account.userId) } returns GmailConnectionSettingsAccess.ListOnly

        viewModel.state.test {
            skipItems(1)
            accountFlow.emit(account)
            awaitItem()

            // When
            viewModel.onConnectGmailShown()

            // Then
            verify(exactly = 0) { reportByoeTelemetryEvent(any(), any()) }
        }
    }

    @Test
    fun `given Connect your Gmail is shown, when the user taps it, then nothing is reported`() = runTest {
        // Given
        coEvery { getGmailConnectionSettingsAccess(account.userId) } returns GmailConnectionSettingsAccess.Full

        viewModel.state.test {
            skipItems(1)
            accountFlow.emit(account)
            awaitItem()

            // When
            viewModel.onConnectGmailClicked()

            // Then
            verify(exactly = 0) { reportByoeTelemetryEvent(any(), any()) }
        }
    }

    @Test
    fun `given the user left for the Gmail connection, when the screen shows again, then only later views report`() =
        runTest {
            // Given
            coEvery { getGmailConnectionSettingsAccess(account.userId) } returns GmailConnectionSettingsAccess.Full

            viewModel.state.test {
                skipItems(1)
                accountFlow.emit(account)
                awaitItem()
                viewModel.onConnectGmailClicked()

                // When
                viewModel.onConnectGmailShown()

                // Then
                verify(exactly = 0) { reportByoeTelemetryEvent(any(), any()) }

                // When
                viewModel.onConnectGmailShown()

                // Then
                verify(exactly = 1) { reportByoeTelemetryEvent(ByoeTelemetryEvent.SettingsShown, account.userId) }
                verify(exactly = 1) { reportByoeTelemetryEvent(ByoeTelemetryEvent.SettingsView, account.userId) }
            }
        }

    private companion object {

        val account = Account(
            userId = UserId("123"),
            name = "Display Name",
            state = AccountState.Ready,
            primaryAddress = "primary@example.com",
            avatarInfo = AccountAvatarInfo("D", "#FF5733")
        )
    }
}
