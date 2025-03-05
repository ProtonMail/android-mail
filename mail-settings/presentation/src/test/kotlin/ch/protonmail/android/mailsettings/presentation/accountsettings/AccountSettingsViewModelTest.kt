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

package ch.protonmail.android.mailsettings.presentation.accountsettings

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.usecase.IsPaidMailUser
import ch.protonmail.android.mailcommon.domain.usecase.ObserveUser
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveUserSettings
import ch.protonmail.android.mailsettings.presentation.R.string
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingsState.Data
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingsState.Loading
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingsState.NotLoggedIn
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.AccountSettingsViewAction
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.model.UserUpgradeState
import ch.protonmail.android.mailupselling.presentation.model.BottomSheetVisibilityEffect
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingVisibility
import ch.protonmail.android.testdata.mailsettings.MailSettingsTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import ch.protonmail.android.testdata.user.UserTestData
import ch.protonmail.android.testdata.usersettings.UserSettingsTestData
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.auth.domain.feature.IsFido2Enabled
import me.proton.core.auth.fido.domain.entity.Fido2RegisteredKey
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.user.domain.entity.User
import me.proton.core.usersettings.domain.entity.UserSettings
import me.proton.core.usersettings.domain.usecase.ObserveRegisteredSecurityKeys
import javax.inject.Provider
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AccountSettingsViewModelTest {

    private val userFlow = MutableSharedFlow<User?>()
    private val accountManager: AccountManager = mockk {
        every { getPrimaryUserId() } returns flowOf(userId)
    }
    private val observeUser = mockk<ObserveUser> {
        every { this@mockk(userId) } returns userFlow
    }

    private val userSettingsFlow = MutableSharedFlow<UserSettings?>()
    private val observeUserSettings = mockk<ObserveUserSettings> {
        every { this@mockk(userId) } returns userSettingsFlow
    }

    private val mailSettingsFlow = MutableSharedFlow<MailSettings?>()
    private val observeMailSettings = mockk<ObserveMailSettings> {
        every { this@mockk(userId) } returns mailSettingsFlow
    }

    private val registeredSecurityKeys = listOf<Fido2RegisteredKey>(mockk())
    private val observeRegisteredSecurityKeys = mockk<ObserveRegisteredSecurityKeys> {
        every { this@mockk(userId) } returns MutableStateFlow(registeredSecurityKeys)
    }

    private val isFido2Enabled = mockk<IsFido2Enabled> {
        every { this@mockk.invoke(userId) } returns true
    }

    private val observeUpsellingVisibility = mockk<ObserveUpsellingVisibility> {
        every { this@mockk(UpsellingEntryPoint.Feature.AutoDelete) } returns flowOf(false)
    }

    private val userUpgradeCheckStateFlow = MutableStateFlow<UserUpgradeState.UserUpgradeCheckState>(
        UserUpgradeState.UserUpgradeCheckState.Initial
    )

    private val userUpgradeState = mockk<UserUpgradeState> {
        every { userUpgradeCheckState } returns userUpgradeCheckStateFlow
        every { this@mockk.isUserPendingUpgrade } returns false
    }

    private val provideIsAutodeleteFeatureEnabled = mockk<Provider<Boolean>>()

    private val isPaidMailUser = mockk<IsPaidMailUser> {
        coEvery { this@mockk(userId) } returns false.right()
    }

    private val viewModel by lazy {
        AccountSettingsViewModel(
            accountManager = accountManager,
            isFido2Enabled = isFido2Enabled,
            observeUser = observeUser,
            observeUserSettings = observeUserSettings,
            observeMailSettings = observeMailSettings,
            observeRegisteredSecurityKeys = observeRegisteredSecurityKeys,
            observeUpsellingVisibility = observeUpsellingVisibility,
            userUpgradeState = userUpgradeState,
            isAutodeleteFeatureEnabled = provideIsAutodeleteFeatureEnabled.get(),
            isPaidMailUser = isPaidMailUser
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        MockKAnnotations.init(this)

        autoDeleteFeatureEnabled(false)
    }

    @Test
    fun `emits loading state when initialised`() = runTest {
        viewModel.state.test {
            assertEquals(Loading, awaitItem())
        }
    }

    @Test
    fun `state has recovery email when use case returns valid user settings`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()
            primaryUserExists()
            mailSettingsExist()

            // When
            userSettingsFlow.emit(UserSettingsTestData.userSettings)

            // Then
            val actual = awaitItem() as Data
            val expected = UserSettingsTestData.RECOVERY_EMAIL_RAW
            assertEquals(expected, actual.recoveryEmail)
        }
    }

    @Test
    fun `state has null recovery email when use case returns invalid user settings`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()
            primaryUserExists()
            mailSettingsExist()

            // When
            userSettingsFlow.emit(UserSettingsTestData.emptyUserSettings)

            // Then
            val actual = awaitItem() as Data
            assertNull(actual.recoveryEmail)
        }
    }

    @Test
    fun `state has default email when use case returns a valid user`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()
            userSettingsExist()
            mailSettingsExist()

            // When
            userFlow.emit(UserTestData.Primary)

            // Then
            val actual = awaitItem() as Data
            val expected = UserTestData.USER_EMAIL_RAW
            assertEquals(expected, actual.defaultEmail)
        }
    }

    @Test
    fun `state has null default email when use case returns an invalid user`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()
            userSettingsExist()
            mailSettingsExist()

            // When
            userFlow.emit(null)

            // Then
            val actual = awaitItem() as Data
            assertNull(actual.defaultEmail)
        }
    }

    @Test
    fun `state has base mailbox sizes when use case returns a valid user with split storage`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()
            userSettingsExist()
            mailSettingsExist()

            // When
            userFlow.emit(UserTestData.Primary)

            // Then
            val actual = awaitItem() as Data
            assertEquals(UserTestData.MAX_BASE_SPACE_RAW, actual.mailboxSize)
            assertEquals(UserTestData.USED_BASE_SPACE_RAW, actual.mailboxUsedSpace)
        }
    }

    @Test
    fun `state has total mailbox sizes when use case returns a valid user with unified storage`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()
            userSettingsExist()
            mailSettingsExist()

            // When
            userFlow.emit(UserTestData.Primary.copy(usedBaseSpace = null, maxBaseSpace = null))

            // Then
            val actual = awaitItem() as Data
            assertEquals(UserTestData.MAX_SPACE_RAW, actual.mailboxSize)
            assertEquals(UserTestData.USED_SPACE_RAW, actual.mailboxUsedSpace)
        }
    }

    @Test
    fun `state has null mailbox sizes when use case returns an invalid user`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()
            userSettingsExist()
            mailSettingsExist()

            // When
            userFlow.emit(null)

            // Then
            val actual = awaitItem() as Data
            assertNull(actual.mailboxSize)
            assertNull(actual.mailboxUsedSpace)
        }
    }

    @Test
    fun `state has conversation mode flag when use case returns a valid mail settings`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()
            primaryUserExists()
            userSettingsExist()

            // When
            mailSettingsFlow.emit(MailSettingsTestData.mailSettings)
            awaitItem()

            // Then
            val actual = awaitItem() as Data
            assertEquals(false, actual.isConversationMode)
        }
    }

    @Test
    fun `state has auto-delete feature visible when FF is on`() = runTest {
        // given
        autoDeleteFeatureEnabled(true)

        viewModel.state.test {
            // when
            initialStateEmitted()
            primaryUserExists()
            userSettingsExist()
            mailSettingsExist()
            awaitItem()

            // then
            val data = awaitItem() as Data
            assertTrue(data.autoDeleteSettingsState.isSettingVisible)
        }
    }

    @Test
    fun `state has auto-delete upselling visible visible when FF is on`() = runTest {
        // given
        autoDeleteUpsellingIsOn()

        viewModel.state.test {
            // when
            initialStateEmitted()
            primaryUserExists()
            userSettingsExist()
            mailSettingsExist()
            awaitItem()

            // then
            val data = awaitItem() as Data
            assertTrue(data.autoDeleteSettingsState.isUpsellingVisible)
        }
    }

    @Test
    fun `state has doesSettingNeedSubscription = true when User has no subscription for Mail`() = runTest {
        // given
        expectIsPaidMailUser(false)

        viewModel.state.test {
            // when
            initialStateEmitted()
            primaryUserExists()
            userSettingsExist()
            mailSettingsExist()
            awaitItem()

            // then
            val data = awaitItem() as Data
            assertTrue(data.autoDeleteSettingsState.doesSettingNeedSubscription)
        }
    }

    @Test
    fun `state has doesSettingNeedSubscription = false when User has subscription for Mail`() = runTest {
        // given
        expectIsPaidMailUser(true)

        viewModel.state.test {
            // when
            initialStateEmitted()
            primaryUserExists()
            userSettingsExist()
            mailSettingsExist()
            awaitItem()

            // then
            val data = awaitItem() as Data
            assertFalse(data.autoDeleteSettingsState.doesSettingNeedSubscription)
        }
    }

    @Test
    fun `state has auto-delete upselling in days correctly passed from MailSettings`() = runTest {
        // given
        viewModel.state.test {
            // when
            initialStateEmitted()
            primaryUserExists()
            userSettingsExist()
            mailSettingsFlow.emit(
                MailSettingsTestData.mailSettings.copy(
                    autoDeleteSpamAndTrashDays = 666
                )
            )
            awaitItem()

            // then
            val data = awaitItem() as Data
            assertEquals(666, data.autoDeleteSettingsState.autoDeleteInDays)
        }
    }

    @Test
    fun `state has auto-delete upselling BottomSheet visible when no upselling in progress and setting clicked`() =
        runTest {
            // given
            autoDeleteUpsellingIsOn()
            every { userUpgradeState.isUserPendingUpgrade } returns false

            viewModel.state.test {
                // when
                initialStateEmitted()
                primaryUserExists()
                userSettingsExist()
                mailSettingsExist()
                awaitItem()
                awaitItem()

                viewModel.submit(AccountSettingsViewAction.SettingsItemClicked)

                // then
                val data = awaitItem() as Data
                assertEquals(
                    Effect.of(BottomSheetVisibilityEffect.Show),
                    data.autoDeleteSettingsState.upsellingVisibility as Effect<*>
                )
            }
        }

    @Test
    fun `state has auto-delete upgrading in progress visible when upselling in progress and setting clicked`() =
        runTest {
            // given
            every { userUpgradeState.isUserPendingUpgrade } returns true

            viewModel.state.test {
                // when
                initialStateEmitted()
                primaryUserExists()
                userSettingsExist()
                mailSettingsExist()
                awaitItem()
                awaitItem()

                viewModel.submit(AccountSettingsViewAction.SettingsItemClicked)

                // then
                val data = awaitItem() as Data
                assertEquals(
                    Effect.of(TextUiModel(string.upselling_snackbar_upgrade_in_progress)),
                    data.autoDeleteSettingsState.upsellingInProgress
                )
                assertEquals(
                    Effect.empty(),
                    data.autoDeleteSettingsState.subscriptionNeededError
                )
            }
        }

    @Test
    fun `state has subscription needed error visible when non-paid-mail user clicks the setting and upselling OFF`() =
        runTest {
            // given
            expectIsPaidMailUser(false)

            viewModel.state.test {
                // when
                initialStateEmitted()
                primaryUserExists()
                userSettingsExist()
                mailSettingsExist()
                awaitItem()
                awaitItem()

                viewModel.submit(AccountSettingsViewAction.SettingsItemClicked)

                // then
                val data = awaitItem() as Data
                assertEquals(
                    Effect.of(TextUiModel(string.mail_settings_auto_delete_subscription_needed)),
                    data.autoDeleteSettingsState.subscriptionNeededError
                )
                assertEquals(
                    Effect.empty(),
                    data.autoDeleteSettingsState.upsellingInProgress
                )
            }
        }

    @Test
    fun `state has null conversation mode when use case returns invalid mail settings`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()
            primaryUserExists()
            userSettingsExist()

            // When
            mailSettingsFlow.emit(null)
            awaitItem()

            // Then
            val actual = awaitItem() as Data
            assertNull(actual.isConversationMode)
        }
    }

    @Test
    fun `emits right state when no user is logged in`() = runTest {
        // given
        givenNoLoggedInUser()

        // when
        viewModel.state.test {

            // then
            assertEquals(NotLoggedIn, awaitItem())
        }
    }

    @Test
    fun `security keys item is visible if fido2 flag is enabled`() = runTest {
        // given
        every { isFido2Enabled(userId) } returns true

        viewModel.state.test {
            // given
            initialStateEmitted()
            primaryUserExists()
            userSettingsExist()
            mailSettingsExist()
            awaitItem()

            // then
            val data = awaitItem() as Data
            assertTrue(data.securityKeysVisible)
            assertSame(registeredSecurityKeys, data.registeredSecurityKeys)
        }
    }

    private fun givenNoLoggedInUser() {
        every { accountManager.getPrimaryUserId() } returns flowOf(null)
    }

    private suspend fun ReceiveTurbine<AccountSettingsState>.initialStateEmitted() {
        awaitItem() as Loading
    }

    private suspend fun primaryUserExists() {
        userFlow.emit(UserTestData.Primary)
    }

    private suspend fun userSettingsExist() {
        userSettingsFlow.emit(UserSettingsTestData.userSettings)
    }

    private suspend fun mailSettingsExist() {
        mailSettingsFlow.emit(MailSettingsTestData.mailSettings)
    }

    private fun autoDeleteFeatureEnabled(value: Boolean) {
        every {
            provideIsAutodeleteFeatureEnabled.get()
        } returns value
    }

    private fun autoDeleteUpsellingIsOn() {
        every { observeUpsellingVisibility(UpsellingEntryPoint.Feature.AutoDelete) } returns flowOf(true)
    }

    private fun expectIsPaidMailUser(value: Boolean) {
        coEvery { isPaidMailUser(userId) } returns value.right()
    }
}
