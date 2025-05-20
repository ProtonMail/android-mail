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

package ch.protonmail.android.mailupselling.presentation.mapper

import ch.protonmail.android.mailupselling.domain.annotations.HeaderUpsellSocialProofLayoutEnabled
import ch.protonmail.android.mailupselling.domain.annotations.HeaderUpsellVariantLayoutEnabled
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.extension.promoPrice
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceListUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlansUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlansVariant
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.entity.DynamicPlanInstance
import javax.inject.Inject

internal class DynamicPlanUiMapper @Inject constructor(
    private val iconUiMapper: DynamicPlanIconUiMapper,
    private val titleUiMapper: DynamicPlanTitleUiMapper,
    private val descriptionUiMapper: DynamicPlanDescriptionUiMapper,
    private val planInstanceUiMapper: DynamicPlanInstanceUiMapper,
    private val entitlementsUiMapper: DynamicPlanEntitlementsUiMapper,
    @HeaderUpsellVariantLayoutEnabled private val headerUpsellVariantLayoutEnabled: Boolean,
    @HeaderUpsellSocialProofLayoutEnabled private val headerUpsellSocialProofLayoutEnabled: Boolean
) {

    fun toUiModel(
        userId: UserId,
        plan: DynamicPlan,
        upsellingEntryPoint: UpsellingEntryPoint.Feature
    ): DynamicPlansUiModel {
        val monthlyPlanInstance = plan.instances.minByOrNull { it.key }?.value
        val yearlyPlanInstance = plan.instances.maxByOrNull { it.key }?.value

        val variant = resolveVariant(
            monthlyPlanInstance,
            yearlyPlanInstance,
            upsellingEntryPoint
        )

        val emptyUiModel = DynamicPlansUiModel(
            icon = iconUiMapper.toUiModel(upsellingEntryPoint, variant),
            title = titleUiMapper.toUiModel(plan, upsellingEntryPoint),
            description = descriptionUiMapper.toUiModel(plan, upsellingEntryPoint, variant),
            entitlements = entitlementsUiMapper.toUiModel(plan, upsellingEntryPoint, variant),
            variant = variant,
            list = DynamicPlanInstanceListUiModel.Empty
        )

        if (plan.instances.isEmpty()) return emptyUiModel

        val monthlyPlan = requireNotNull(monthlyPlanInstance)
        val yearlyPlan = requireNotNull(yearlyPlanInstance)

        if (monthlyPlan == yearlyPlan) {
            return emptyUiModel.copy(list = DynamicPlanInstanceListUiModel.Invalid)
        }

        val (shorterCycleUiModel, longerCycleUiModel) = planInstanceUiMapper.toUiModel(
            userId,
            monthlyPlan,
            yearlyPlan,
            plan
        )

        val listUiModel = resolveListUiModel(monthlyPlan, shorterCycleUiModel, longerCycleUiModel, variant)
        return emptyUiModel.copy(list = listUiModel)
    }

    private fun resolveListUiModel(
        shorterPlan: DynamicPlanInstance,
        shorterCycleUiModel: DynamicPlanInstanceUiModel,
        longerCycleUiModel: DynamicPlanInstanceUiModel,
        variant: DynamicPlansVariant
    ): DynamicPlanInstanceListUiModel.Data {
        return when {
            variant == DynamicPlansVariant.SocialProof -> {
                DynamicPlanInstanceListUiModel.Data.SocialProof(shorterCycleUiModel, longerCycleUiModel)
            }

            shorterCycleUiModel is DynamicPlanInstanceUiModel.Promotional ||
                longerCycleUiModel is DynamicPlanInstanceUiModel.Promotional -> {
                if (variant == DynamicPlansVariant.PromoB) {
                    val price = shorterPlan.price.values.first()
                    val formattedPrice = price.promoPrice(shorterPlan.cycle)
                    DynamicPlanInstanceListUiModel.Data.PromotionalVariantB(shorterCycleUiModel, formattedPrice)
                } else {
                    DynamicPlanInstanceListUiModel.Data.Promotional(shorterCycleUiModel, longerCycleUiModel)
                }
            }

            else -> DynamicPlanInstanceListUiModel.Data.Standard(
                shorterCycleUiModel as DynamicPlanInstanceUiModel.Standard,
                longerCycleUiModel as DynamicPlanInstanceUiModel.Standard
            )
        }
    }

    private fun resolveVariant(
        monthlyInstance: DynamicPlanInstance?,
        yearlyInstance: DynamicPlanInstance?,
        entryPoint: UpsellingEntryPoint
    ): DynamicPlansVariant {
        val isPromotional = listOfNotNull(monthlyInstance, yearlyInstance).any { instance ->
            val price = instance.price.values.first()
            val currentPrice = price.current
            val defaultPrice = price.default
            val isPromotional = defaultPrice != null && currentPrice < defaultPrice
            isPromotional
        }
        val supportsHeaderVariants = entryPoint.supportsHeaderVariants()
        return when {
            isPromotional -> if (supportsHeaderVariants && headerUpsellVariantLayoutEnabled) {
                DynamicPlansVariant.PromoB
            } else DynamicPlansVariant.PromoA
            supportsHeaderVariants && headerUpsellSocialProofLayoutEnabled -> DynamicPlansVariant.SocialProof
            else -> DynamicPlansVariant.Normal
        }
    }

    private fun UpsellingEntryPoint.supportsHeaderVariants() = when (this) {
        UpsellingEntryPoint.Feature.Mailbox,
        UpsellingEntryPoint.Feature.MailboxPromo,
        UpsellingEntryPoint.Feature.Navbar -> true
        UpsellingEntryPoint.Feature.AutoDelete,
        UpsellingEntryPoint.Feature.ContactGroups,
        UpsellingEntryPoint.Feature.Folders,
        UpsellingEntryPoint.Feature.Labels,
        UpsellingEntryPoint.Feature.MobileSignature,
        UpsellingEntryPoint.PostOnboarding -> false
    }
}
