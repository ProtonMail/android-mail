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

package ch.protonmail.android.mailupselling.presentation.ui.screen

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentState
import ch.protonmail.android.mailupselling.presentation.model.UserIdUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanCycle
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanDescriptionUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanIconUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceListUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanTitleUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlansUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.PlanEntitlementListUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.PlanEntitlementsUiModel
import me.proton.core.domain.entity.UserId
import me.proton.core.domain.type.IntEnum
import me.proton.core.plan.domain.entity.DynamicEntitlement
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.entity.DynamicPlanState
import me.proton.core.plan.domain.entity.DynamicPlanType

internal object UpsellingContentPreviewData {

    private val dynPlan = DynamicPlan(
        name = "Proton Unlimited",
        order = 1,
        state = DynamicPlanState.Available,
        title = "Proton Unlimited",
        description = "For more storage and all premium features across all Proton services.",
        entitlements = listOf(
            DynamicEntitlement.Description(text = "Unlimited hide-my-email aliases", iconUrl = "nourl"),
            DynamicEntitlement.Description(text = "Unlimited hide-my-email aliases", iconUrl = "nourl")
        ),
        type = IntEnum(1, DynamicPlanType.Primary)
    )

    val Base = UpsellingScreenContentState.Data(
        DynamicPlansUiModel(
            icon = DynamicPlanIconUiModel(R.drawable.illustration_upselling_mailbox),
            title = DynamicPlanTitleUiModel(TextUiModel.Text("Mail Plus")),
            description = DynamicPlanDescriptionUiModel(TextUiModel.Text("Description")),
            entitlements = PlanEntitlementsUiModel.SimpleList(
                listOf(
                    PlanEntitlementListUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 1"),
                        localResource = R.drawable.ic_upselling_pass
                    ),
                    PlanEntitlementListUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 2"),
                        localResource = R.drawable.ic_upselling_mail
                    ),
                    PlanEntitlementListUiModel.Overridden(
                        text = TextUiModel.Text("Entitlement 3"),
                        localResource = R.drawable.ic_upselling_gift
                    )
                )
            ),
            list = DynamicPlanInstanceListUiModel.Data.Standard(
                DynamicPlanInstanceUiModel.Standard(
                    name = "Mail Plus",
                    userId = UserIdUiModel(UserId("12")),
                    currency = "EUR",
                    pricePerCycle = TextUiModel("0.99"),
                    totalPrice = TextUiModel("4.99"),
                    discountRate = null,
                    cycle = DynamicPlanCycle.Monthly,
                    viewId = 123,
                    dynamicPlan = dynPlan
                ),
                DynamicPlanInstanceUiModel.Standard(
                    name = "Mail Plus",
                    userId = UserIdUiModel(UserId("12")),
                    currency = "EUR",
                    pricePerCycle = TextUiModel("4.99"),
                    totalPrice = TextUiModel("49.99"),
                    discountRate = null,
                    cycle = DynamicPlanCycle.Yearly,
                    viewId = 123,
                    dynamicPlan = dynPlan
                )
            )
        )
    )
}
