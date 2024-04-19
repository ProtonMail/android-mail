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

package ch.protonmail.upselling.presentation.mapper

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanEntitlementsUiMapper
import ch.protonmail.android.mailupselling.presentation.model.DynamicEntitlementUiModel
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import javax.inject.Provider
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class DynamicPlanEntitlementsUiMapperTest {

    private val forceOverride = mockk<Provider<Boolean>>()
    private val mapper: DynamicPlanEntitlementsUiMapper
        get() = DynamicPlanEntitlementsUiMapper(forceOverride.get())

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return the default entitlements when override is unset`() {
        // Given
        every { forceOverride.get() } returns false
        val expected = listOf(
            DynamicEntitlementUiModel.Default(TextUiModel.Text("10 email addresses"), "iconUrl")
        )

        // When
        val actual = mapper.toUiModel(UpsellingTestData.PlusPlan)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return the default entitlements when override is set but plan unknown`() {
        // Given
        every { forceOverride.get() } returns false
        val expected = listOf(
            DynamicEntitlementUiModel.Default(TextUiModel.Text("10 email addresses"), "iconUrl")
        )

        // When
        val actual = mapper.toUiModel(UpsellingTestData.PlusPlan.copy(name = "Unknown"))

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return custom entitlements when override is set with known plan`() {
        // Given
        every { forceOverride.get() } returns true
        val override = listOf(
            DynamicEntitlementUiModel.Default(TextUiModel.Text("10 email addresses"), "iconUrl")
        )

        // When
        val actual = mapper.toUiModel(UpsellingTestData.PlusPlan)

        // Then
        assertTrue(actual.isNotEmpty())
        assertNotEquals(override, actual)
    }
}
