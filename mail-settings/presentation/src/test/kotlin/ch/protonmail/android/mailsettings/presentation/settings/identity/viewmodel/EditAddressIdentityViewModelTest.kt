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

package ch.protonmail.android.mailsettings.presentation.settings.identity.viewmodel

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailsettings.domain.model.DisplayName
import ch.protonmail.android.mailsettings.domain.model.MobileFooter
import ch.protonmail.android.mailsettings.domain.model.MobileFooterPreference
import ch.protonmail.android.mailsettings.domain.model.Signature
import ch.protonmail.android.mailsettings.domain.model.SignatureValue
import ch.protonmail.android.mailsettings.domain.usecase.identity.GetPrimaryAddressDisplayName
import ch.protonmail.android.mailsettings.domain.usecase.identity.GetPrimaryAddressSignature
import ch.protonmail.android.mailsettings.domain.usecase.identity.UpdatePrimaryAddressIdentity
import ch.protonmail.android.mailsettings.domain.usecase.identity.UpdatePrimaryUserMobileFooter
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.mapper.EditAddressIdentityMapper
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.AddressSignatureUiModel
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.DisplayNameUiModel
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.EditAddressIdentityEvent
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.EditAddressIdentityState
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.EditAddressIdentityViewAction
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.MobileFooterUiModel
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.reducer.EditAddressIdentityReducer
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.usecase.GetMobileFooter
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.viewmodel.EditAddressIdentityViewModel
import ch.protonmail.android.mailupselling.domain.model.UserUpgradeState
import ch.protonmail.android.mailupselling.presentation.model.BottomSheetVisibilityEffect
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingVisibility
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.domain.entity.UserId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

internal class EditAddressIdentityViewModelTest {

    private val userUpgradeCheckStateFlow = MutableStateFlow<UserUpgradeState.UserUpgradeCheckState>(
        UserUpgradeState.UserUpgradeCheckState.Initial
    )

    private val observePrimaryUserId = mockk<ObservePrimaryUserId>()
    private val getPrimaryAddressDisplayName = mockk<GetPrimaryAddressDisplayName>()
    private val getPrimaryAddressSignature = mockk<GetPrimaryAddressSignature>()
    private val getMobileFooter = mockk<GetMobileFooter>()
    private val updatePrimaryAddressIdentity = mockk<UpdatePrimaryAddressIdentity>()
    private val updatePrimaryUserMobileFooter = mockk<UpdatePrimaryUserMobileFooter>()
    private val reducer = spyk(EditAddressIdentityReducer(EditAddressIdentityMapper()))
    private val observeUpsellingVisibility = mockk<ObserveUpsellingVisibility>()
    private val userUpgradeState = mockk<UserUpgradeState> {
        every { userUpgradeCheckState } returns userUpgradeCheckStateFlow
    }

    private val viewModel by lazy {
        EditAddressIdentityViewModel(
            observePrimaryUserId,
            getPrimaryAddressDisplayName,
            getPrimaryAddressSignature,
            getMobileFooter,
            updatePrimaryAddressIdentity,
            updatePrimaryUserMobileFooter,
            reducer,
            observeUpsellingVisibility,
            userUpgradeState
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return loading state when first launched`() = runTest {
        // Given
        expectObserverUpsellingVisibility(true)
        every { observePrimaryUserId() } returns flowOf()

        // Then
        viewModel.state.test {
            val loadingState = awaitItem()
            assertEquals(EditAddressIdentityState.Loading, loadingState)
        }
    }

    @Test
    fun `should return an error when no primary user id is found`() = runTest {
        // Given
        expectObserverUpsellingVisibility(true)
        every { observePrimaryUserId() } returns flowOf(null)

        // Then
        viewModel.state.test {
            val errorState = awaitItem()
            assertEquals(EditAddressIdentityState.LoadingError, errorState)
        }
        verify(exactly = 1) {
            reducer.newStateFrom(
                EditAddressIdentityState.Loading,
                EditAddressIdentityEvent.Error.LoadingError
            )
        }
        confirmVerified(reducer)
    }

    @Test
    fun `should return an error when no primary address display name is found`() = runTest {
        // Given
        expectValidUserId()
        expectObserverUpsellingVisibility(true)
        coEvery { getPrimaryAddressDisplayName(BaseUserId) } returns DataError.AddressNotFound.left()

        // Then
        viewModel.state.test {
            val errorState = awaitItem()
            assertEquals(EditAddressIdentityState.LoadingError, errorState)
        }
        verify(exactly = 1) {
            reducer.newStateFrom(
                EditAddressIdentityState.Loading,
                EditAddressIdentityEvent.Error.LoadingError
            )
        }
        confirmVerified(reducer)
    }

    @Test
    fun `should return an error when no primary address signature is found`() = runTest {
        // Given
        expectValidUserId()
        expectValidDisplayName()
        expectObserverUpsellingVisibility(true)
        coEvery { getPrimaryAddressSignature(BaseUserId) } returns DataError.Local.NoDataCached.left()

        // Then
        viewModel.state.test {
            val errorState = awaitItem()
            assertEquals(EditAddressIdentityState.LoadingError, errorState)
        }
        verify(exactly = 1) {
            reducer.newStateFrom(
                EditAddressIdentityState.Loading,
                EditAddressIdentityEvent.Error.LoadingError
            )
        }
        confirmVerified(reducer)
    }

    @Test
    fun `should return an error when no mobile footer is found`() = runTest {
        // Given
        expectValidUserId()
        expectValidDisplayName()
        expectValidSignature()
        expectObserverUpsellingVisibility(true)
        coEvery { getMobileFooter(BaseUserId) } returns DataError.Local.NoDataCached.left()

        // Then
        viewModel.state.test {
            val errorState = awaitItem()
            assertEquals(EditAddressIdentityState.LoadingError, errorState)
        }
        verify(exactly = 1) {
            reducer.newStateFrom(
                EditAddressIdentityState.Loading,
                EditAddressIdentityEvent.Error.LoadingError
            )
        }
        confirmVerified(reducer)
    }

    @Test
    fun `should load data when all details can be fetched an error when no mobile footer is found`() = runTest {
        // Given
        expectValidUserId()
        expectValidDisplayName()
        expectValidSignature()
        expectValidMobileFooter()
        expectObserverUpsellingVisibility(false)

        // Then
        viewModel.state.test {
            val actualState = awaitItem()
            assertEquals(BaseLoadedState, actualState)
        }
        verify(exactly = 1) {
            reducer.newStateFrom(
                EditAddressIdentityState.Loading,
                EditAddressIdentityEvent.Data.ContentLoaded(
                    BaseDisplayName,
                    BaseSignature,
                    BaseMobileFooter
                )
            )
            reducer.newStateFrom(
                BaseLoadedState,
                EditAddressIdentityEvent.UpgradeStateChanged(
                    BaseMobileFooter,
                    UserUpgradeState.UserUpgradeCheckState.Initial,
                    shouldShowUpselling = false
                )
            )
        }
        confirmVerified(reducer)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `should load data when all details can be fetched and make mobile toggle toggleable for upselling if ON`() =
        runTest {
            // Given
            expectValidUserId()
            expectValidDisplayName()
            expectValidSignature()
            coEvery { getMobileFooter(BaseUserId) } returns BaseMobileFreeUpsellingFooter.right()
            expectObserverUpsellingVisibility(true)

            // Then
            viewModel.state.test {
                val actualState = awaitItem()
                assertEquals(BaseFreeUserUpsellingLoadedState, actualState)
            }
        }

    @Test
    fun `should update the state when the display name is changed`() = runTest {
        // Given
        expectValidUserId()
        expectValidDisplayName()
        expectValidSignature()
        expectValidMobileFooter()
        expectObserverUpsellingVisibility(false)
        val newValue = "display-name-2"

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(EditAddressIdentityViewAction.DisplayName.UpdateValue(newValue))
            assertEquals(
                BaseLoadedState.copy(
                    displayNameState = EditAddressIdentityState.DisplayNameState(DisplayNameUiModel(newValue))
                ),
                awaitItem()
            )
        }
        coVerifySequence {
            reducer.newStateFrom(
                EditAddressIdentityState.Loading,
                EditAddressIdentityEvent.Data.ContentLoaded(
                    BaseDisplayName,
                    BaseSignature,
                    BaseMobileFooter
                )
            )
            reducer.newStateFrom(
                BaseLoadedState,
                EditAddressIdentityEvent.UpgradeStateChanged(
                    BaseMobileFooter,
                    UserUpgradeState.UserUpgradeCheckState.Initial,
                    shouldShowUpselling = false
                )
            )
            reducer.newStateFrom(
                BaseLoadedState,
                EditAddressIdentityViewAction.DisplayName.UpdateValue(newValue)
            )
        }
    }

    @Test
    fun `should update the state when the signature value is changed`() = runTest {
        // Given
        expectValidUserId()
        expectValidDisplayName()
        expectValidSignature()
        expectValidMobileFooter()
        expectObserverUpsellingVisibility(false)
        val newValue = "display-name-2"

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(EditAddressIdentityViewAction.Signature.UpdateValue(newValue))
            assertEquals(
                BaseLoadedState.copy(
                    signatureState = BaseSignatureState.copy(AddressSignatureUiModel(newValue, true))
                ),
                awaitItem()
            )
        }
        coVerifySequence {
            reducer.newStateFrom(
                EditAddressIdentityState.Loading,
                EditAddressIdentityEvent.Data.ContentLoaded(
                    BaseDisplayName,
                    BaseSignature,
                    BaseMobileFooter
                )
            )
            reducer.newStateFrom(
                BaseLoadedState,
                EditAddressIdentityEvent.UpgradeStateChanged(
                    BaseMobileFooter,
                    UserUpgradeState.UserUpgradeCheckState.Initial,
                    shouldShowUpselling = false
                )
            )
            reducer.newStateFrom(
                BaseLoadedState,
                EditAddressIdentityViewAction.Signature.UpdateValue(newValue)
            )
        }
    }

    @Test
    fun `should update the state when the signature toggled value is changed`() = runTest {
        // Given
        expectValidUserId()
        expectValidDisplayName()
        expectValidSignature()
        expectValidMobileFooter()
        expectObserverUpsellingVisibility(false)
        val newValue = false

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(EditAddressIdentityViewAction.Signature.ToggleState(newValue))
            assertEquals(
                BaseLoadedState.copy(
                    signatureState = BaseSignatureState.copy(AddressSignatureUiModel("signature", newValue))
                ),
                awaitItem()
            )
        }
        coVerifySequence {
            reducer.newStateFrom(
                EditAddressIdentityState.Loading,
                EditAddressIdentityEvent.Data.ContentLoaded(
                    BaseDisplayName,
                    BaseSignature,
                    BaseMobileFooter
                )
            )
            reducer.newStateFrom(
                BaseLoadedState,
                EditAddressIdentityEvent.UpgradeStateChanged(
                    BaseMobileFooter,
                    UserUpgradeState.UserUpgradeCheckState.Initial,
                    shouldShowUpselling = false
                )
            )
            reducer.newStateFrom(
                BaseLoadedState,
                EditAddressIdentityViewAction.Signature.ToggleState(newValue)
            )
        }
    }

    @Test
    fun `should update the state when the mobile footer value is changed`() = runTest {
        // Given
        expectValidUserId()
        expectValidDisplayName()
        expectValidSignature()
        expectValidMobileFooter()
        expectObserverUpsellingVisibility(false)
        val newValue = "mobile-footer-2"

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(EditAddressIdentityViewAction.MobileFooter.UpdateValue(newValue))
            assertEquals(
                BaseLoadedState.copy(
                    mobileFooterState = BaseMobileFooterState.copy(
                        MobileFooterUiModel(
                            newValue,
                            enabled = true,
                            isFieldEnabled = true,
                            isToggleEnabled = true,
                            isUpsellingVisible = false
                        )
                    )
                ),
                awaitItem()
            )
        }
        coVerifySequence {
            reducer.newStateFrom(
                EditAddressIdentityState.Loading,
                EditAddressIdentityEvent.Data.ContentLoaded(
                    BaseDisplayName,
                    BaseSignature,
                    BaseMobileFooter
                )
            )
            reducer.newStateFrom(
                BaseLoadedState,
                EditAddressIdentityEvent.UpgradeStateChanged(
                    BaseMobileFooter,
                    UserUpgradeState.UserUpgradeCheckState.Initial,
                    shouldShowUpselling = false
                )
            )
            reducer.newStateFrom(
                BaseLoadedState,
                EditAddressIdentityViewAction.MobileFooter.UpdateValue(newValue)
            )
        }
    }

    @Test
    fun `should update the state when the mobile footer toggled value is changed`() = runTest {
        // Given
        expectValidUserId()
        expectValidDisplayName()
        expectValidSignature()
        expectValidMobileFooter()
        expectObserverUpsellingVisibility(false)
        expectUpsellingInProgress(false)

        val newValue = false

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(EditAddressIdentityViewAction.MobileFooter.ToggleState(newValue))
            assertEquals(
                BaseLoadedState.copy(
                    mobileFooterState = BaseMobileFooterState.copy(
                        MobileFooterUiModel("footer", newValue, true, true, false)
                    )
                ),
                awaitItem()
            )
        }
        coVerifySequence {
            reducer.newStateFrom(
                EditAddressIdentityState.Loading,
                EditAddressIdentityEvent.Data.ContentLoaded(
                    BaseDisplayName,
                    BaseSignature,
                    BaseMobileFooter
                )
            )
            reducer.newStateFrom(
                BaseLoadedState,
                EditAddressIdentityEvent.UpgradeStateChanged(
                    BaseMobileFooter,
                    UserUpgradeState.UserUpgradeCheckState.Initial,
                    shouldShowUpselling = false
                )
            )
            reducer.newStateFrom(
                BaseLoadedState,
                EditAddressIdentityViewAction.MobileFooter.ToggleState(newValue)
            )
        }
    }

    @Test
    fun `should show upselling when mobile footer is toggled OFF and upselling is ON`() = runTest {
        // Given
        expectValidUserId()
        expectValidDisplayName()
        expectValidSignature()
        coEvery { getMobileFooter(BaseUserId) } returns BaseMobileFreeUpsellingFooter.right()
        expectObserverUpsellingVisibility(true)
        expectUpsellingInProgress(false)

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(EditAddressIdentityViewAction.MobileFooter.ToggleState(false))
            assertEquals(
                BaseFreeUserUpsellingLoadedState.copy(
                    upsellingVisibility = Effect.of(BottomSheetVisibilityEffect.Show)
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun `should show upselling in progress when mobile footer is toggled and user is being upgraded`() = runTest {
        // Given
        expectValidUserId()
        expectValidDisplayName()
        expectValidSignature()
        coEvery { getMobileFooter(BaseUserId) } returns BaseMobileFreeUpsellingFooter.right()
        expectObserverUpsellingVisibility(true)
        expectUpsellingInProgress(true)

        val expectedState = BaseFreeUserUpsellingLoadedState.copy(
            upsellingInProgress = Effect.of(TextUiModel(R.string.upselling_snackbar_upgrade_in_progress))
        )

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(EditAddressIdentityViewAction.MobileFooter.ToggleState(false))
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `should save the settings and close the screen if no error occurs`() = runTest {
        // Given
        expectValidUserId()
        expectValidDisplayName()
        expectValidSignature()
        expectValidMobileFooter()
        expectObserverUpsellingVisibility(false)
        expectUpsellingInProgress(false)
        coEvery { updatePrimaryAddressIdentity(BaseDisplayName, BaseSignature) } returns Unit.right()
        coEvery { updatePrimaryUserMobileFooter(BaseMobileFooterPreference) } returns Unit.right()

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(EditAddressIdentityViewAction.Save)
            assertEquals(BaseLoadedState.copy(close = Effect.of(Unit)), awaitItem())
        }
        coVerifySequence {
            reducer.newStateFrom(
                EditAddressIdentityState.Loading,
                EditAddressIdentityEvent.Data.ContentLoaded(
                    BaseDisplayName,
                    BaseSignature,
                    BaseMobileFooter
                )
            )
            reducer.newStateFrom(
                BaseLoadedState,
                EditAddressIdentityEvent.UpgradeStateChanged(
                    BaseMobileFooter,
                    UserUpgradeState.UserUpgradeCheckState.Initial,
                    shouldShowUpselling = false
                )
            )
            reducer.newStateFrom(
                BaseLoadedState,
                EditAddressIdentityEvent.Navigation.Close
            )
        }
        coVerify(exactly = 1) {
            updatePrimaryAddressIdentity(BaseDisplayName, BaseSignature)
            updatePrimaryUserMobileFooter(BaseMobileFooterPreference)
        }
    }

    @Test
    fun `should hide upselling when action is triggered`() = runTest {
        // Given
        expectValidUserId()
        expectValidDisplayName()
        expectValidSignature()
        expectValidMobileFooter()
        expectObserverUpsellingVisibility(false)

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(EditAddressIdentityViewAction.HideUpselling)
            assertEquals(
                BaseLoadedState.copy(upsellingVisibility = Effect.of(BottomSheetVisibilityEffect.Hide)), awaitItem()
            )
        }
        coVerifySequence {
            reducer.newStateFrom(
                EditAddressIdentityState.Loading,
                EditAddressIdentityEvent.Data.ContentLoaded(
                    BaseDisplayName,
                    BaseSignature,
                    BaseMobileFooter
                )
            )
            reducer.newStateFrom(
                BaseLoadedState,
                EditAddressIdentityEvent.UpgradeStateChanged(
                    BaseMobileFooter,
                    UserUpgradeState.UserUpgradeCheckState.Initial,
                    shouldShowUpselling = false
                )
            )
            reducer.newStateFrom(
                BaseLoadedState,
                EditAddressIdentityEvent.HideUpselling
            )
        }
    }

    @Test
    fun `should not close the screen if the display name and signature cannot be updated`() = runTest {
        // Given
        expectValidUserId()
        expectValidDisplayName()
        expectValidSignature()
        expectValidMobileFooter()
        expectObserverUpsellingVisibility(false)
        expectUpsellingInProgress(false)

        coEvery {
            updatePrimaryAddressIdentity(
                BaseDisplayName,
                BaseSignature
            )
        } returns UpdatePrimaryAddressIdentity.Error.UpdateFailure.left()

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(EditAddressIdentityViewAction.Save)
            assertEquals(BaseLoadedState.copy(updateError = Effect.of(Unit)), awaitItem())
        }
        coVerifySequence {
            reducer.newStateFrom(
                EditAddressIdentityState.Loading,
                EditAddressIdentityEvent.Data.ContentLoaded(
                    BaseDisplayName,
                    BaseSignature,
                    BaseMobileFooter
                )
            )
            reducer.newStateFrom(
                BaseLoadedState,
                EditAddressIdentityEvent.UpgradeStateChanged(
                    BaseMobileFooter,
                    UserUpgradeState.UserUpgradeCheckState.Initial,
                    shouldShowUpselling = false
                )
            )
            reducer.newStateFrom(
                BaseLoadedState,
                EditAddressIdentityEvent.Error.UpdateError
            )
        }
        coVerify { updatePrimaryUserMobileFooter wasNot called }
        coVerify(exactly = 1) {
            updatePrimaryAddressIdentity(BaseDisplayName, BaseSignature)
        }
    }

    @Test
    fun `should not close the screen if the mobile footer cannot be updated`() = runTest {
        // Given
        expectValidUserId()
        expectValidDisplayName()
        expectValidSignature()
        expectValidMobileFooter()
        expectObserverUpsellingVisibility(false)
        expectUpsellingInProgress(false)

        coEvery {
            updatePrimaryAddressIdentity(BaseDisplayName, BaseSignature)
        } returns Unit.right()
        coEvery {
            updatePrimaryUserMobileFooter(BaseMobileFooterPreference)
        } returns UpdatePrimaryUserMobileFooter.Error.UpdateFailure.left()

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(EditAddressIdentityViewAction.Save)
            assertEquals(BaseLoadedState.copy(updateError = Effect.of(Unit)), awaitItem())
        }
        coVerifySequence {
            reducer.newStateFrom(
                EditAddressIdentityState.Loading,
                EditAddressIdentityEvent.Data.ContentLoaded(
                    BaseDisplayName,
                    BaseSignature,
                    BaseMobileFooter
                )
            )
            reducer.newStateFrom(
                BaseLoadedState,
                EditAddressIdentityEvent.UpgradeStateChanged(
                    BaseMobileFooter,
                    UserUpgradeState.UserUpgradeCheckState.Initial,
                    shouldShowUpselling = false
                )
            )
            reducer.newStateFrom(
                BaseLoadedState,
                EditAddressIdentityEvent.Error.UpdateError
            )
        }
        coVerify(exactly = 1) {
            updatePrimaryAddressIdentity(BaseDisplayName, BaseSignature)
            updatePrimaryUserMobileFooter(BaseMobileFooterPreference)
        }
    }

    @Test
    fun `should return upselling in progress when performing a save action and user is being upgraded`() = runTest {
        // Given
        expectValidUserId()
        expectValidDisplayName()
        expectValidSignature()
        expectValidMobileFooter()
        expectObserverUpsellingVisibility(false)
        expectUpsellingInProgress(true)
        coEvery { updatePrimaryAddressIdentity(BaseDisplayName, BaseSignature) } returns Unit.right()
        coEvery { updatePrimaryUserMobileFooter(BaseMobileFooterPreference) } returns Unit.right()
        val expectedState = BaseLoadedState.copy(
            upsellingInProgress = Effect.of(TextUiModel(R.string.upselling_snackbar_upgrade_in_progress))
        )

        // When + Then
        viewModel.state.test {
            skipItems(1)
            viewModel.submit(EditAddressIdentityViewAction.Save)
            assertEquals(expectedState, awaitItem())
        }
        coVerifySequence {
            reducer.newStateFrom(
                EditAddressIdentityState.Loading,
                EditAddressIdentityEvent.Data.ContentLoaded(
                    BaseDisplayName,
                    BaseSignature,
                    BaseMobileFooter
                )
            )
            reducer.newStateFrom(
                BaseLoadedState,
                EditAddressIdentityEvent.UpgradeStateChanged(
                    BaseMobileFooter,
                    UserUpgradeState.UserUpgradeCheckState.Initial,
                    shouldShowUpselling = false
                )
            )
            reducer.newStateFrom(
                BaseLoadedState,
                EditAddressIdentityEvent.UpsellingInProgress
            )
        }
        coVerify {
            updatePrimaryAddressIdentity wasNot called
            updatePrimaryUserMobileFooter wasNot called
        }

        confirmVerified(reducer)
    }

    @Test
    fun `should emit an event when the upgrade state has changed`() = runTest {
        // Given
        expectValidUserId()
        expectValidDisplayName()
        expectValidSignature()
        expectValidMobileFooter()
        expectObserverUpsellingVisibility(false)
        val expectedState = BaseLoadedState.copy(
            mobileFooterState = BaseMobileFooterState.copy(
                mobileFooterUiModel = BaseMobileFooterState.mobileFooterUiModel.copy(
                    isFieldEnabled = false
                )
            )
        )

        viewModel.state.test {
            skipItems(1)

            // When
            userUpgradeCheckStateFlow.emit(UserUpgradeState.UserUpgradeCheckState.Pending)

            // Then
            assertEquals(expectedState, awaitItem())
            verify {
                reducer.newStateFrom(
                    any(),
                    EditAddressIdentityEvent.UpgradeStateChanged(
                        BaseMobileFooter,
                        UserUpgradeState.UserUpgradeCheckState.Pending,
                        shouldShowUpselling = false
                    )
                )
            }
        }
    }

    private fun expectUpsellingInProgress(value: Boolean) {
        every { userUpgradeState.isUserPendingUpgrade } returns value
    }

    private fun expectValidUserId() {
        every { observePrimaryUserId() } returns flowOf(BaseUserId)
    }

    private fun expectValidDisplayName() {
        coEvery { getPrimaryAddressDisplayName(BaseUserId) } returns BaseDisplayName.right()
    }

    private fun expectValidSignature() {
        coEvery { getPrimaryAddressSignature(BaseUserId) } returns BaseSignature.right()
    }

    private fun expectValidMobileFooter() {
        coEvery { getMobileFooter(BaseUserId) } returns BaseMobileFooter.right()
    }

    private fun expectObserverUpsellingVisibility(value: Boolean) {
        coEvery { observeUpsellingVisibility(any()) } returns flowOf(value)
    }

    private companion object {

        private val BaseUserId = UserId("user-id")
        private val BaseDisplayName = DisplayName("display-name")
        private val BaseSignature = Signature(enabled = true, SignatureValue("signature"))
        private val BaseMobileFooter = MobileFooter.PaidUserMobileFooter("footer", enabled = true)
        private val BaseMobileFreeUpsellingFooter = MobileFooter.FreeUserUpsellingMobileFooter("footer")
        private val BaseDisplayNameState = EditAddressIdentityState.DisplayNameState(
            DisplayNameUiModel("display-name")
        )
        private val BaseSignatureState = EditAddressIdentityState.SignatureState(
            AddressSignatureUiModel("signature", enabled = true)
        )
        private val BaseMobileFooterState = EditAddressIdentityState.MobileFooterState(
            MobileFooterUiModel(
                "footer",
                enabled = true,
                isFieldEnabled = true,
                isToggleEnabled = true,
                isUpsellingVisible = false
            )
        )
        private val BaseMobileUpsellingFooterState = EditAddressIdentityState.MobileFooterState(
            MobileFooterUiModel(
                "footer",
                enabled = true,
                isFieldEnabled = false,
                isToggleEnabled = true,
                isUpsellingVisible = true
            )
        )
        private val BaseError = Effect.empty<Unit>()
        private val BaseClose = Effect.empty<Unit>()
        private val BaseUpsellingVisibility = Effect.empty<BottomSheetVisibilityEffect>()
        private val BaseUpsellingInProgress = Effect.empty<TextUiModel>()
        private val BaseMobileFooterPreference = MobileFooterPreference(
            BaseMobileFooter.value, BaseMobileFooter.enabled
        )

        private val BaseLoadedState = EditAddressIdentityState.DataLoaded(
            BaseDisplayNameState,
            BaseSignatureState,
            BaseMobileFooterState,
            BaseError,
            BaseClose,
            BaseUpsellingVisibility,
            BaseUpsellingInProgress
        )

        private val BaseFreeUserUpsellingLoadedState = BaseLoadedState.copy(
            mobileFooterState = BaseMobileUpsellingFooterState
        )
    }
}
