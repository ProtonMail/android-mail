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

import java.time.Instant
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanTitleUiMapper
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanTitleUiModel
import io.mockk.mockk
import me.proton.core.domain.type.IntEnum
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.entity.DynamicPlanInstance
import me.proton.core.plan.domain.entity.DynamicPlanPrice
import me.proton.core.plan.domain.entity.DynamicPlanState
import me.proton.core.plan.domain.entity.DynamicPlanType
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("MaxLineLength")
internal class DynamicPlanTitleUiMapperTest {

    private val mapper = DynamicPlanTitleUiMapper(false)
    private val mapperVariant = DynamicPlanTitleUiMapper(true)
    private val plan = mockk<DynamicPlan>()

    @Test
    fun `should map to the corresponding ui models`() {
        // Given
        val expected = mapOf(
            UpsellingEntryPoint.Feature.ContactGroups to DynamicPlanTitleUiModel(TextUiModel(R.string.upselling_contact_groups_plus_title)),
            UpsellingEntryPoint.Feature.Folders to DynamicPlanTitleUiModel(TextUiModel(R.string.upselling_folders_plus_title)),
            UpsellingEntryPoint.Feature.Labels to DynamicPlanTitleUiModel(TextUiModel(R.string.upselling_labels_plus_title)),
            UpsellingEntryPoint.Feature.MobileSignature to DynamicPlanTitleUiModel(TextUiModel(R.string.upselling_mobile_signature_plus_title)),
            UpsellingEntryPoint.Feature.Mailbox to DynamicPlanTitleUiModel(TextUiModel(R.string.upselling_mailbox_plus_title)),
            UpsellingEntryPoint.Feature.AutoDelete to DynamicPlanTitleUiModel(TextUiModel(R.string.upselling_auto_delete_plus_title)),
            UpsellingEntryPoint.Feature.MailboxPromo to DynamicPlanTitleUiModel(TextUiModel(R.string.upselling_mailbox_plus_title))
        )

        // When
        val actual = mapOf(
            UpsellingEntryPoint.Feature.ContactGroups to mapper.toUiModel(plan, UpsellingEntryPoint.Feature.ContactGroups),
            UpsellingEntryPoint.Feature.Folders to mapper.toUiModel(plan, UpsellingEntryPoint.Feature.Folders),
            UpsellingEntryPoint.Feature.Labels to mapper.toUiModel(plan, UpsellingEntryPoint.Feature.Labels),
            UpsellingEntryPoint.Feature.MobileSignature to mapper.toUiModel(plan, UpsellingEntryPoint.Feature.MobileSignature),
            UpsellingEntryPoint.Feature.Mailbox to mapper.toUiModel(plan, UpsellingEntryPoint.Feature.Mailbox),
            UpsellingEntryPoint.Feature.AutoDelete to mapper.toUiModel(plan, UpsellingEntryPoint.Feature.AutoDelete),
            UpsellingEntryPoint.Feature.MailboxPromo to mapper.toUiModel(plan, UpsellingEntryPoint.Feature.MailboxPromo)
        )

        // Then
        for (pair in expected) {
            assertEquals(pair.value, actual[pair.key])
        }
    }

    @Test
    fun `should map to the corresponding ui models using the promo variant FF`() {
        // Given
        val expected = mapOf(
            UpsellingEntryPoint.Feature.MailboxPromo to DynamicPlanTitleUiModel(TextUiModel(R.string.upselling_mailbox_plus_promo_title, "EUR 1.23"))
        )

        // When
        val actual = mapOf(
            UpsellingEntryPoint.Feature.MailboxPromo to mapperVariant.toUiModel(DynamicPlanPromo, UpsellingEntryPoint.Feature.MailboxPromo)
        )

        // Then
        for (pair in expected) {
            assertEquals(pair.value, actual[pair.key])
        }
    }

    private companion object {

        val PlanPricePromo = DynamicPlanPrice(
            id = "id1",
            currency = "EUR",
            current = 123,
            default = 200
        )
        val PlanInstancePromo = DynamicPlanInstance(
            cycle = 0,
            description = "desc",
            periodEnd = Instant.EPOCH,
            price = mapOf("EUR" to PlanPricePromo)
        )
        val DynamicPlanPromo = DynamicPlan(
            name = "Plan",
            order = 1,
            state = DynamicPlanState.Available,
            title = "Title",
            type = IntEnum(0, DynamicPlanType.Primary),
            instances = mapOf(
                0 to PlanInstancePromo
            )
        )
    }
}
