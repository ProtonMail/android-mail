/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailupselling.domain.usecase

import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailupselling.domain.cache.AvailableUpgradesCache
import ch.protonmail.android.mailupselling.domain.model.BlackFridayPhase
import ch.protonmail.android.mailupselling.domain.model.BlackFridaySupported
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeIds
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeSupportedTags
import ch.protonmail.android.mailupselling.domain.model.SpringPromoPhase
import ch.protonmail.android.mailupselling.domain.model.SpringPromoSupported
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.proton.android.core.payment.domain.model.ProductOfferDetail
import me.proton.android.core.payment.domain.model.filterForTags
import javax.inject.Inject

class ObserveMailPlusPlanUpgrades @Inject constructor(
    private val cache: AvailableUpgradesCache,
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val getCurrentBlackFridayPhase: GetCurrentBlackFridayPhase,
    private val getCurrentSpringPromoPhase: GetCurrentSpringPromoPhase,
    private val isEligibleForBlackFridayPromotion: IsEligibleForBlackFridayPromotion
) {

    operator fun invoke(entryPoint: UpsellingEntryPoint.Feature) = observePrimaryUserId().flatMapLatest { userId ->
        userId ?: return@flatMapLatest flowOf(emptyList())

        val flowSupportsBlackFriday = entryPoint is BlackFridaySupported
        val isBlackFridayOngoing = getCurrentBlackFridayPhase() != BlackFridayPhase.None
        val supportsBlackFridayPromo = flowSupportsBlackFriday &&
            isBlackFridayOngoing &&
            isEligibleForBlackFridayPromotion(userId)

        val flowSupportsSpringPromo = entryPoint is SpringPromoSupported
        val isSpringPromoOngoing = getCurrentSpringPromoPhase() != SpringPromoPhase.None
        val supportsSpringPromo = flowSupportsSpringPromo &&
            isSpringPromoOngoing &&
            isEligibleForBlackFridayPromotion(userId)

        val offersTag = when {
            supportsSpringPromo -> PlanUpgradeSupportedTags.SpringOffer
            supportsBlackFridayPromo -> PlanUpgradeSupportedTags.BlackFriday
            else -> PlanUpgradeSupportedTags.IntroductoryPrice
        }

        // Here should be either BF **OR** Intro pricing, never fallback between 2 promo prices
        cache.observe(userId).map { upgrades ->
            val eligibleOffers = upgrades.filterForTags(primaryTag = offersTag.value)
            eligibleOffers.filterMailPlus()
        }
    }

    private fun List<ProductOfferDetail>.filterMailPlus() = filter { it.metadata.planName == PlanUpgradeIds.PlusPlanId }
}
