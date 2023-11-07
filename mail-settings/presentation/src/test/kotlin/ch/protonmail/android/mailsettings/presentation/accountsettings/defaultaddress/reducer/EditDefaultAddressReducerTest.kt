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

package ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.reducer

import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.mapper.EditDefaultAddressUiMapper
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model.DefaultAddressUiModel
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model.EditDefaultAddressEvent
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model.EditDefaultAddressState
import io.mockk.spyk
import kotlinx.collections.immutable.toImmutableList
import me.proton.core.user.domain.entity.AddressId
import me.proton.core.user.domain.entity.UserAddress
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class EditDefaultAddressReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val mapper = spyk(EditDefaultAddressUiMapper())
    private val reducer = EditDefaultAddressReducer(mapper)

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        val actualState = reducer.newStateFrom(currentState, event)
        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val activeAddresses = listOf(
            UserAddressSample.build(addressId = AddressId("123"), email = "address1@proton.me", order = 1),
            UserAddressSample.build(addressId = AddressId("456"), email = "address2@proton.me", order = 2),
            UserAddressSample.build(addressId = AddressId("789"), email = "address3@proton.me", order = 3)
        )
        private val updatedAddresses = listOf(
            UserAddressSample.build(addressId = AddressId("456"), email = "address2@proton.me", order = 1),
            UserAddressSample.build(addressId = AddressId("123"), email = "address1@proton.me", order = 2),
            UserAddressSample.build(addressId = AddressId("789"), email = "address3@proton.me", order = 3)
        )

        private val inactiveAddresses = emptyList<UserAddress>()
        private val baseAddresses = activeAddresses + inactiveAddresses
        private val baseUpdatedAddresses = updatedAddresses + inactiveAddresses

        private val baseState = EditDefaultAddressState.WithData(
            activeAddressesState = EditDefaultAddressState.WithData.ActiveAddressesState(
                listOf(
                    DefaultAddressUiModel.Active(order = 1, addressId = "123", address = "address1@proton.me"),
                    DefaultAddressUiModel.Active(order = 2, addressId = "456", address = "address2@proton.me"),
                    DefaultAddressUiModel.Active(order = 3, addressId = "789", address = "address3@proton.me")
                ).toImmutableList()
            ),
            inactiveAddressesState = EditDefaultAddressState.WithData.InactiveAddressesState(
                emptyList<DefaultAddressUiModel.Inactive>().toImmutableList()
            ),
            updateErrorState = EditDefaultAddressState.WithData.UpdateErrorState(Effect.empty(), Effect.empty())
        )

        private val updatedState = EditDefaultAddressState.WithData(
            activeAddressesState = EditDefaultAddressState.WithData.ActiveAddressesState(
                listOf(
                    DefaultAddressUiModel.Active(order = 1, addressId = "456", address = "address2@proton.me"),
                    DefaultAddressUiModel.Active(order = 2, addressId = "123", address = "address1@proton.me"),
                    DefaultAddressUiModel.Active(order = 3, addressId = "789", address = "address3@proton.me")
                ).toImmutableList()
            ),
            inactiveAddressesState = EditDefaultAddressState.WithData.InactiveAddressesState(
                emptyList<DefaultAddressUiModel.Inactive>().toImmutableList()
            ),
            updateErrorState = EditDefaultAddressState.WithData.UpdateErrorState(Effect.empty(), Effect.empty())
        )

        private val errorState = EditDefaultAddressState.LoadingError
        private val updateErrorState = baseState.copy(
            updateErrorState = EditDefaultAddressState.WithData.UpdateErrorState(
                updateError = Effect.of(Unit), incompatibleSubscriptionError = Effect.empty()
            )
        )
        private val upgradeErrorState = baseState.copy(
            updateErrorState = EditDefaultAddressState.WithData.UpdateErrorState(
                updateError = Effect.empty(), incompatibleSubscriptionError = Effect.of(Unit)
            )
        )

        private val transitionFromLoadingState = listOf(
            TestInput(
                currentState = EditDefaultAddressState.Loading,
                event = EditDefaultAddressEvent.Data.ContentLoaded(baseAddresses),
                expectedState = baseState
            ),
            TestInput(
                currentState = EditDefaultAddressState.Loading,
                event = EditDefaultAddressEvent.Error.LoadingError,
                expectedState = errorState
            )
        )

        private val transitionsFromLoadedState = listOf(
            TestInput(
                currentState = baseState,
                event = EditDefaultAddressEvent.Data.ContentUpdated(baseUpdatedAddresses),
                expectedState = updatedState
            ),
            TestInput(
                currentState = baseState,
                event = EditDefaultAddressEvent.Error.UpdateError,
                expectedState = updateErrorState
            ),
            TestInput(
                currentState = baseState,
                event = EditDefaultAddressEvent.Error.UpgradeRequired,
                expectedState = upgradeErrorState
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = (transitionFromLoadingState + transitionsFromLoadedState)
            .map { testInput ->
                val testName = """
                    Current state: ${testInput.currentState}
                    Event: ${testInput.event}
                    Next state: ${testInput.expectedState}        
                """.trimIndent()
                arrayOf(testName, testInput)
            }
    }

    data class TestInput(
        val currentState: EditDefaultAddressState,
        val event: EditDefaultAddressEvent,
        val expectedState: EditDefaultAddressState
    )
}
