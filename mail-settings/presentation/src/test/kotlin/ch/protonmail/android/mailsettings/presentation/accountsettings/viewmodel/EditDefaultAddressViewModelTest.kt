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

package ch.protonmail.android.mailsettings.presentation.accountsettings.viewmodel

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailsettings.domain.usecase.SetDefaultAddress
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.mapper.EditDefaultAddressUiMapper
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model.DefaultAddressUiModel
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model.EditDefaultAddressEvent
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model.EditDefaultAddressState
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.reducer.EditDefaultAddressReducer
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.viewmodel.EditDefaultAddressViewModel
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.user.domain.UserAddressManager
import me.proton.core.user.domain.entity.AddressId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

internal class EditDefaultAddressViewModelTest {

    private val observePrimaryUserId = mockk<ObservePrimaryUserId>()
    private val userAddressManager = mockk<UserAddressManager>()
    private val setDefaultEmailAddress = mockk<SetDefaultAddress>()
    private val mapper = EditDefaultAddressUiMapper()
    private val reducer = spyk(EditDefaultAddressReducer(mapper))

    private val viewModel by lazy {
        EditDefaultAddressViewModel(
            observePrimaryUserId,
            userAddressManager,
            setDefaultEmailAddress,
            reducer
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
        every { observePrimaryUserId() } returns flowOf()

        // Then
        viewModel.state.test {
            val loadingState = awaitItem()
            assertEquals(EditDefaultAddressState.Loading, loadingState)
        }
    }

    @Test
    fun `should return an error when no primary user id is found`() = runTest {
        // Given
        every { observePrimaryUserId() } returns flowOf(null)

        // Then
        viewModel.state.test {
            val errorState = awaitItem()
            assertEquals(EditDefaultAddressState.LoadingError, errorState)
        }
        verify(exactly = 1) {
            reducer.newStateFrom(
                EditDefaultAddressState.Loading,
                EditDefaultAddressEvent.Error.LoadingError
            )
        }
        confirmVerified(reducer)
    }

    @Test
    fun `should return an error when no addresses are found`() = runTest {
        // Given
        expectValidUserIdFetched()
        every { userAddressManager.observeAddresses(userId) } returns flowOf(emptyList())

        // Then
        viewModel.state.test {
            val errorState = awaitItem()
            assertEquals(EditDefaultAddressState.LoadingError, errorState)
        }
        verify(exactly = 1) {
            reducer.newStateFrom(
                EditDefaultAddressState.Loading,
                EditDefaultAddressEvent.Error.LoadingError
            )
        }
        confirmVerified(reducer)
    }

    @Test
    fun `should render the addresses list when addresses are fetched correctly`() = runTest {
        // Given
        expectValidUserIdFetched()
        expectValidAddressesLoaded()

        // Then
        viewModel.state.test {
            val actualState = awaitItem()
            assertEquals(baseExpectedDataState, actualState)
        }
        verify(exactly = 1) {
            reducer.newStateFrom(
                EditDefaultAddressState.Loading,
                EditDefaultAddressEvent.Data.ContentLoaded(addresses)
            )
        }
        confirmVerified(reducer)
    }

    @Test
    fun `should return an error when user id cannot be fetched upon updating the addresses`() = runTest {
        // Given
        expectValidUserIdFetched()
        expectValidAddressesLoaded()
        val expectedState = baseExpectedDataState.copy(
            updateErrorState = EditDefaultAddressState.WithData.UpdateErrorState(
                updateError = Effect.of(Unit),
                incompatibleSubscriptionError = Effect.empty()
            )
        )

        // When
        viewModel.state.test {
            awaitItem()
            expectInvalidUserIsFetched()
            viewModel.setPrimaryAddress("123")

            // Then
            assertEquals(awaitItem(), expectedState)
        }
    }

    @Test
    fun `should return a generic error when the update fails`() = runTest {
        // Given
        expectValidUserIdFetched()
        expectValidAddressesLoaded()
        coEvery { setDefaultEmailAddress(any(), any()) } returns SetDefaultAddress.Error.UpdateFailed.left()

        // When + Then
        verifyUpdateError(updateError = Effect.of(Unit), subscriptionError = Effect.empty())
    }

    @Test
    fun `should return an error when the selected address does not exist`() = runTest {
        // Given
        expectValidUserIdFetched()
        expectValidAddressesLoaded()
        coEvery { setDefaultEmailAddress(any(), any()) } returns SetDefaultAddress.Error.AddressNotFound.left()

        // When + Then
        verifyUpdateError(updateError = Effect.of(Unit), subscriptionError = Effect.empty())
    }

    @Test
    fun `should return a subscription related error when the address needs a paid account to be set`() = runTest {
        // Given
        expectValidUserIdFetched()
        expectValidAddressesLoaded()
        coEvery { setDefaultEmailAddress(any(), any()) } returns SetDefaultAddress.Error.UpgradeRequired.left()

        // When + Then
        verifyUpdateError(
            updateError = Effect.empty(),
            subscriptionError = Effect.of(Unit),
            updateErrorEvent = EditDefaultAddressEvent.Error.UpgradeRequired
        )
    }

    @Test
    fun `should not force update the state when a new address is set as default`() = runTest {
        // Given
        expectValidUserIdFetched()
        expectValidAddressesLoaded()
        coEvery { setDefaultEmailAddress(any(), any()) } returns addresses.right()

        // When + Then
        viewModel.state.test {
            skipItems(1)
            viewModel.setPrimaryAddress(baseAddressId.id)
        }
        verify {
            reducer.newStateFrom(
                EditDefaultAddressState.Loading,
                EditDefaultAddressEvent.Data.ContentLoaded(addresses)
            )
            reducer.newStateFrom(
                baseExpectedDataState,
                EditDefaultAddressEvent.Data.ContentUpdated(addresses)
            )
        }
        confirmVerified(reducer)
    }

    private suspend fun verifyUpdateError(
        updateError: Effect<Unit>,
        subscriptionError: Effect<Unit>,
        updateErrorEvent: EditDefaultAddressEvent.Error = EditDefaultAddressEvent.Error.UpdateError
    ) {
        val expectedFinalState = baseExpectedDataState.copy(
            updateErrorState = EditDefaultAddressState.WithData.UpdateErrorState(
                updateError = updateError,
                incompatibleSubscriptionError = subscriptionError
            )
        )

        viewModel.state.test {
            skipItems(1) // Skip loading state
            viewModel.setPrimaryAddress(baseAddressId.id)
            assertEquals(expectedFinalState, awaitItem())
        }

        // Then
        verifySequence {
            reducer.newStateFrom(
                EditDefaultAddressState.Loading,
                EditDefaultAddressEvent.Data.ContentLoaded(addresses)
            )
            reducer.newStateFrom(
                baseExpectedDataState,
                updateErrorEvent
            )
        }
        confirmVerified(reducer)
    }

    private fun expectInvalidUserIsFetched() {
        every { observePrimaryUserId() } returns flowOf(null)
    }

    private fun expectValidUserIdFetched() {
        every { observePrimaryUserId() } returns flowOf(userId)
    }

    private fun expectValidAddressesLoaded() {
        every {
            userAddressManager.observeAddresses(userId)
        } returns flowOf(addresses)
    }


    private companion object {

        val userId = UserIdTestData.userId
        val baseAddressId = AddressId("123")
        val baseInactiveAddressId = AddressId("456")
        val addresses = listOf(
            UserAddressSample.build(
                addressId = baseAddressId,
                email = "email@proton.me",
                order = 1,
                enabled = true
            ),
            UserAddressSample.build(
                addressId = baseInactiveAddressId,
                email = "email2@proton.me",
                order = 2,
                enabled = false
            )
        )

        val baseExpectedDataState = EditDefaultAddressState.WithData(
            activeAddressesState = EditDefaultAddressState.WithData.ActiveAddressesState(
                listOf(DefaultAddressUiModel.Active(order = 1, addressId = "123", "email@proton.me")).toImmutableList()
            ),
            inactiveAddressesState = EditDefaultAddressState.WithData.InactiveAddressesState(
                listOf(DefaultAddressUiModel.Inactive("email2@proton.me")).toImmutableList()
            ),
            updateErrorState = EditDefaultAddressState.WithData.UpdateErrorState(Effect.empty(), Effect.empty())
        )
    }
}
