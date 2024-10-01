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

package ch.protonmail.android.mailsettings.presentation.settings.identity.mapper

import ch.protonmail.android.mailsettings.domain.model.DisplayName
import ch.protonmail.android.mailsettings.domain.model.MobileFooter
import ch.protonmail.android.mailsettings.domain.model.Signature
import ch.protonmail.android.mailsettings.domain.model.SignatureValue
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.mapper.EditAddressIdentityMapper
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.AddressSignatureUiModel
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.DisplayNameUiModel
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.MobileFooterUiModel
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

internal class EditAddressIdentityMapperTest {

    private val editAddressIdentityMapper = EditAddressIdentityMapper()

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should correctly map a display name to the corresponding ui model`() {
        // Given
        val expectedUiModel = DisplayNameUiModel("123")
        val displayName = DisplayName("123")

        // When
        val result = editAddressIdentityMapper.toDisplayNameUiModel(displayName)

        // Then
        assertEquals(expectedUiModel, result)
    }

    @Test
    fun `should correctly map the signature to the corresponding ui model`() {
        // Given
        val expectedUiModel = AddressSignatureUiModel(textValue = "123", enabled = false)
        val signature = Signature(enabled = false, SignatureValue("123"))

        // When
        val result = editAddressIdentityMapper.toSignatureUiModel(signature)

        // Then
        assertEquals(expectedUiModel, result)
    }

    @Test
    fun `should correctly map the free mobile footer to the corresponding ui model`() {
        // Given
        val expectedUiModel = MobileFooterUiModel(
            textValue = "123",
            enabled = true,
            isFieldEnabled = false,
            isToggleEnabled = false,
            isUpsellingVisible = false
        )
        val mobileFooter = MobileFooter.FreeUserMobileFooter("123")

        // When
        val result = editAddressIdentityMapper.toMobileFooterUiModel(mobileFooter)

        // Then
        assertEquals(expectedUiModel, result)
    }

    @Test
    fun `should correctly map the free upselling mobile footer to the corresponding ui model`() {
        // Given
        val expectedUiModel = MobileFooterUiModel(
            textValue = "123",
            enabled = true,
            isFieldEnabled = false,
            isToggleEnabled = true,
            isUpsellingVisible = true
        )
        val mobileFooter = MobileFooter.FreeUserUpsellingMobileFooter("123")

        // When
        val result = editAddressIdentityMapper.toMobileFooterUiModel(mobileFooter)

        // Then
        assertEquals(expectedUiModel, result)
    }

    @Test
    fun `should correctly map the paid mobile footer to the corresponding ui model`() {
        // Given
        val expectedUiModel = MobileFooterUiModel(
            textValue = "123",
            enabled = false,
            isFieldEnabled = true,
            isToggleEnabled = true,
            isUpsellingVisible = false
        )
        val mobileFooter = MobileFooter.PaidUserMobileFooter("123", enabled = false)

        // When
        val result = editAddressIdentityMapper.toMobileFooterUiModel(mobileFooter)

        // Then
        assertEquals(expectedUiModel, result)
    }
}
