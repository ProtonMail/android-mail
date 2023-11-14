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

package ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.mapper

import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model.DefaultAddressUiModel
import kotlinx.collections.immutable.toImmutableList
import me.proton.core.user.domain.entity.AddressId
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertEquals

internal class EditDefaultAddressUiMapperTest {

    private val editDefaultAddressUiMapper = EditDefaultAddressUiMapper()

    @Test
    fun `valid active addresses list is correctly mapped to ui model`() {
        // Given
        val addresses = activeAddresses
        val expectedUiModels = listOf(
            DefaultAddressUiModel.Active(isDefault = true, addressId = "id1", address = "email1@proton.me"),
            DefaultAddressUiModel.Active(isDefault = false, addressId = "id2", address = "email2@proton.me"),
            DefaultAddressUiModel.Active(isDefault = false, addressId = "id3", address = "email3@proton.me")
        ).toImmutableList()

        // When
        val result = editDefaultAddressUiMapper.toActiveAddressUiModel(addresses)

        // Then
        assertEquals(expectedUiModels, result)
        assertTrue(result.first().isDefault)
    }

    @Test
    fun `valid inactive addresses list is correctly mapped to ui model`() {
        // Given
        val addresses = inactiveAddresses
        val expectedUiModels = listOf(
            DefaultAddressUiModel.Inactive(address = "email4@proton.me"),
            DefaultAddressUiModel.Inactive(address = "email5@proton.me"),
            DefaultAddressUiModel.Inactive(address = "email6@proton.me")
        ).toImmutableList()

        // When
        val result = editDefaultAddressUiMapper.toInactiveAddressUiModel(addresses)

        // Then
        assertEquals(expectedUiModels, result)
    }

    private companion object {

        val activeAddresses = listOf(
            UserAddressSample.build(
                addressId = AddressId("id1"),
                email = "email1@proton.me",
                order = 1,
                enabled = true
            ),
            UserAddressSample.build(
                addressId = AddressId("id2"),
                email = "email2@proton.me",
                order = 2,
                enabled = true
            ),
            UserAddressSample.build(
                addressId = AddressId("id3"),
                email = "email3@proton.me",
                order = 3,
                enabled = true
            )
        )

        val inactiveAddresses = listOf(
            UserAddressSample.build(
                addressId = AddressId("id4"),
                email = "email4@proton.me",
                order = 4,
                enabled = false
            ),
            UserAddressSample.build(
                addressId = AddressId("id5"),
                email = "email5@proton.me",
                order = 5,
                enabled = false
            ),
            UserAddressSample.build(
                addressId = AddressId("id6"),
                email = "email6@proton.me",
                order = 6,
                enabled = false
            )
        )
    }
}
