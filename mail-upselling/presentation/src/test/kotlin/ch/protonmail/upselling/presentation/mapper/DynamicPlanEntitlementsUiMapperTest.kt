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
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanEntitlementsUiMapper
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlement.Free
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlement.Plus
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlementItemUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlansVariant
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.PlanEntitlementListUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.PlanEntitlementsUiModel
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
        val expected = PlanEntitlementsUiModel.SimpleList(
            listOf(
                PlanEntitlementListUiModel.Default(TextUiModel.Text("10 email addresses"), "iconUrl")
            )
        )

        // When
        val actual = mapper.toListUiModel(UpsellingTestData.PlusPlan, UpsellingEntryPoint.Feature.Mailbox)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return the default entitlements when override is set but plan unknown`() {
        // Given
        every { forceOverride.get() } returns false
        val expected = PlanEntitlementsUiModel.SimpleList(
            listOf(
                PlanEntitlementListUiModel.Default(TextUiModel.Text("10 email addresses"), "iconUrl")
            )
        )

        // When
        val actual =
            mapper.toListUiModel(UpsellingTestData.PlusPlan.copy(name = "Unknown"), UpsellingEntryPoint.Feature.Mailbox)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return custom entitlements when override is set with known plan`() {
        // Given
        every { forceOverride.get() } returns true
        val override = PlanEntitlementsUiModel.SimpleList(
            listOf(
                PlanEntitlementListUiModel.Default(TextUiModel.Text("10 email addresses"), "iconUrl")
            )
        )

        // When
        val actual = mapper.toListUiModel(UpsellingTestData.PlusPlan, UpsellingEntryPoint.Feature.Mailbox)

        // Then
        assertTrue(actual.items.isNotEmpty())
        assertNotEquals(override, actual)
    }

    @Test
    fun `should return comparison table when the entry point is Mailbox`() {
        // Given
        every { forceOverride.get() } returns false
        val expected = PlanEntitlementsUiModel.ComparisonTableList(
            listOf(
                ComparisonTableEntitlementItemUiModel(
                    title = TextUiModel.TextRes(R.string.upselling_comparison_table_storage),
                    freeValue = Free.Value(TextUiModel.TextRes(R.string.upselling_comparison_table_storage_value_free)),
                    paidValue = Plus.Value(TextUiModel.TextRes(R.string.upselling_comparison_table_storage_value_plus))
                ),
                ComparisonTableEntitlementItemUiModel(
                    title = TextUiModel.TextRes(R.string.upselling_comparison_table_email_addresses),
                    freeValue = Free.Value(TextUiModel.TextRes(R.string.upselling_comparison_table_email_addresses_value_free)),
                    paidValue = Plus.Value(TextUiModel.TextRes(R.string.upselling_comparison_table_email_addresses_value_plus))
                ),
                ComparisonTableEntitlementItemUiModel(
                    title = TextUiModel.TextRes(R.string.upselling_comparison_table_custom_email_domain),
                    freeValue = Free.NotPresent,
                    paidValue = Plus.Present
                ),
                ComparisonTableEntitlementItemUiModel(
                    title = TextUiModel.TextRes(R.string.upselling_comparison_table_desktop_app),
                    freeValue = Free.NotPresent,
                    paidValue = Plus.Present
                ),
                ComparisonTableEntitlementItemUiModel(
                    title = TextUiModel.TextRes(R.string.upselling_comparison_table_unlimited_folders_labels),
                    freeValue = Free.NotPresent,
                    paidValue = Plus.Present
                ),
                ComparisonTableEntitlementItemUiModel(
                    title = TextUiModel.TextRes(R.string.upselling_comparison_table_priority_support),
                    freeValue = Free.NotPresent,
                    paidValue = Plus.Present
                )
            )
        )

        // When
        val actual = mapper.toUiModel(
            UpsellingTestData.PlusPlan,
            UpsellingEntryPoint.Feature.Mailbox, DynamicPlansVariant.Normal
        )

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return custom entitlements for different EntryPoints when override is set`() {
        // Given
        every { forceOverride.get() } returns true

        val expectedEntitlements = listOf(
            PlanEntitlementListUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_plus_feature_storage),
                localResource = R.drawable.ic_upselling_storage
            ),
            PlanEntitlementListUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_plus_feature_email_addresses),
                localResource = R.drawable.ic_upselling_inbox
            ),
            PlanEntitlementListUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_plus_feature_custom_domain),
                localResource = R.drawable.ic_upselling_globe
            ),
            PlanEntitlementListUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_plus_feature_plus_7_features),
                localResource = R.drawable.ic_upselling_gift
            )
        )

        val expected = listOf(
            expectedEntitlements,
            expectedEntitlements,
            expectedEntitlements,
            expectedEntitlements,
            expectedEntitlements
        ).map { PlanEntitlementsUiModel.SimpleList(it) }

        // When
        val actual = listOf(
            mapper.toUiModel(UpsellingTestData.PlusPlan, UpsellingEntryPoint.Feature.ContactGroups, DynamicPlansVariant.Normal),
            mapper.toUiModel(UpsellingTestData.PlusPlan, UpsellingEntryPoint.Feature.Labels, DynamicPlansVariant.Normal),
            mapper.toUiModel(UpsellingTestData.PlusPlan, UpsellingEntryPoint.Feature.Folders, DynamicPlansVariant.Normal),
            mapper.toUiModel(UpsellingTestData.PlusPlan, UpsellingEntryPoint.Feature.MobileSignature, DynamicPlansVariant.Normal),
            mapper.toUiModel(UpsellingTestData.PlusPlan, UpsellingEntryPoint.Feature.AutoDelete, DynamicPlansVariant.Normal)
        )

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should correctly map social proof variant`() {
        // Given
        every { forceOverride.get() } returns true

        val expectedEntitlements = listOf(
            TextUiModel.TextRes(R.string.upselling_plus_feature_storage_plus_mails),
            TextUiModel.TextRes(R.string.upselling_plus_feature_folders_labels),
            TextUiModel.TextRes(R.string.upselling_plus_feature_custom_domain),
            TextUiModel.TextRes(R.string.upselling_plus_feature_desktop_app),
            TextUiModel.TextRes(R.string.upselling_plus_feature_calendar),
            TextUiModel.TextRes(R.string.upselling_onboarding_unlimited_feature_sentinel)
        )

        val expected = PlanEntitlementsUiModel.CheckedSimpleList(expectedEntitlements)

        // Then
        assertEquals(
            expected,
            mapper.toUiModel(UpsellingTestData.PlusPlan, UpsellingEntryPoint.Feature.Mailbox, DynamicPlansVariant.SocialProof)
        )
    }
}
