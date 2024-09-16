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
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanEntitlementsUiMapper
import ch.protonmail.android.mailupselling.presentation.model.DynamicEntitlementUiModel
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
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

@Suppress("MaxLineLength")
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
        val actual = mapper.toUiModel(UpsellingTestData.PlusPlan, UpsellingEntryPoint.BottomSheet.Mailbox)

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
        val actual = mapper.toUiModel(UpsellingTestData.PlusPlan.copy(name = "Unknown"), UpsellingEntryPoint.BottomSheet.Mailbox)

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
        val actual = mapper.toUiModel(UpsellingTestData.PlusPlan, UpsellingEntryPoint.BottomSheet.Mailbox)

        // Then
        assertTrue(actual.isNotEmpty())
        assertNotEquals(override, actual)
    }

    @Test
    fun `should return custom entitlements for different EntryPoints when override is set`() {
        // Given
        every { forceOverride.get() } returns true

        val expectedMailboxEntitlements = listOf(
            DynamicEntitlementUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_plus_feature_storage),
                localResource = R.drawable.ic_upselling_storage
            ),
            DynamicEntitlementUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_plus_feature_email_addresses),
                localResource = R.drawable.ic_upselling_inbox
            ),
            DynamicEntitlementUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_plus_feature_custom_domain),
                localResource = R.drawable.ic_upselling_globe
            ),
            DynamicEntitlementUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_plus_feature_desktop_app),
                localResource = R.drawable.ic_upselling_rocket
            ),
            DynamicEntitlementUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_plus_feature_folders_labels),
                localResource = R.drawable.ic_upselling_tag
            )
        )

        val expectedAllTheOtherEntitlements = listOf(
            DynamicEntitlementUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_plus_feature_storage),
                localResource = R.drawable.ic_upselling_storage
            ),
            DynamicEntitlementUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_plus_feature_email_addresses),
                localResource = R.drawable.ic_upselling_inbox
            ),
            DynamicEntitlementUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_plus_feature_custom_domain),
                localResource = R.drawable.ic_upselling_globe
            ),
            DynamicEntitlementUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_plus_feature_plus_7_features),
                localResource = R.drawable.ic_upselling_gift
            )
        )

        val expected = listOf(
            expectedMailboxEntitlements,
            expectedAllTheOtherEntitlements,
            expectedAllTheOtherEntitlements,
            expectedAllTheOtherEntitlements,
            expectedAllTheOtherEntitlements
        )

        // When
        val actual = listOf(
            mapper.toUiModel(UpsellingTestData.PlusPlan, UpsellingEntryPoint.BottomSheet.Mailbox),
            mapper.toUiModel(UpsellingTestData.PlusPlan, UpsellingEntryPoint.BottomSheet.ContactGroups),
            mapper.toUiModel(UpsellingTestData.PlusPlan, UpsellingEntryPoint.BottomSheet.Labels),
            mapper.toUiModel(UpsellingTestData.PlusPlan, UpsellingEntryPoint.BottomSheet.Folders),
            mapper.toUiModel(UpsellingTestData.PlusPlan, UpsellingEntryPoint.BottomSheet.MobileSignature)
        )

        // Then
        assertEquals(expected, actual)
    }
}
