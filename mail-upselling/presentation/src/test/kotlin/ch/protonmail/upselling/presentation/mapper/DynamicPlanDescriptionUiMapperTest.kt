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
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanDescriptionUiMapper
import ch.protonmail.android.mailupselling.presentation.model.DynamicPlanDescriptionUiModel
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingEntryPoint
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import javax.inject.Provider
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class DynamicPlanDescriptionUiMapperTest {

    private val forceOverride = mockk<Provider<Boolean>>()
    private val mapper: DynamicPlanDescriptionUiMapper
        get() = DynamicPlanDescriptionUiMapper(forceOverride.get())

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return the default description when override is unset`() {
        // Given
        every { forceOverride.get() } returns false
        val expected = DynamicPlanDescriptionUiModel(
            text = TextUiModel.Text(UpsellingTestData.PlusPlan.description!!)
        )

        // When
        val actual = mapper.toUiModel(UpsellingTestData.PlusPlan, UpsellingEntryPoint.Mailbox)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return the local description when override is set, for Mailbox entry point`() {
        // Given
        every { forceOverride.get() } returns true
        val expected = DynamicPlanDescriptionUiModel(
            text = TextUiModel.TextRes(R.string.upselling_mailbox_plus_description_override)
        )

        // When
        val actual = mapper.toUiModel(UpsellingTestData.PlusPlan, UpsellingEntryPoint.Mailbox)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return the local description when override is set, for Mobile Signature entry point`() {
        // Given
        every { forceOverride.get() } returns true
        val expected = DynamicPlanDescriptionUiModel(
            text = TextUiModel.TextRes(R.string.upselling_mobile_signature_plus_description_override)
        )

        // When
        val actual = mapper.toUiModel(UpsellingTestData.PlusPlan, UpsellingEntryPoint.MobileSignature)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return the local description when override is set, for Contact Groups entry point`() {
        // Given
        every { forceOverride.get() } returns true
        val expected = DynamicPlanDescriptionUiModel(
            text = TextUiModel.TextRes(R.string.upselling_contact_groups_plus_description_override)
        )

        // When
        val actual = mapper.toUiModel(UpsellingTestData.PlusPlan, UpsellingEntryPoint.ContactGroups)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return the local description when override is set, for Folders entry point`() {
        // Given
        every { forceOverride.get() } returns true
        val expected = DynamicPlanDescriptionUiModel(
            text = TextUiModel.TextRes(R.string.upselling_folders_plus_description_override)
        )

        // When
        val actual = mapper.toUiModel(UpsellingTestData.PlusPlan, UpsellingEntryPoint.Folders)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return the local description when override is set, for Labels entry point`() {
        // Given
        every { forceOverride.get() } returns true
        val expected = DynamicPlanDescriptionUiModel(
            text = TextUiModel.TextRes(R.string.upselling_labels_plus_description_override)
        )

        // When
        val actual = mapper.toUiModel(UpsellingTestData.PlusPlan, UpsellingEntryPoint.Labels)

        // Then
        assertEquals(expected, actual)
    }
}
