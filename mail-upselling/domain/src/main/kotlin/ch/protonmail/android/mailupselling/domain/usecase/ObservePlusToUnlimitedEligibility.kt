/*
 * Copyright (c) 2026 Proton Technologies AG
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

import arrow.core.Either
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsBlackFridayUpsellOptedOut
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsPlusToUnlimitedUpsellEnabled
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsSdkUpgradesPurchaseEnabled
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsSdkUpgradesReadEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailsession.domain.usecase.ObserveUser
import ch.protonmail.android.mailupselling.domain.cache.AvailableUpgradesCache
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeCycle
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeIds
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeSupportedTags
import ch.protonmail.android.mailupselling.domain.model.PlusToUnlimitedOffer
import ch.protonmail.android.mailupselling.domain.repository.UpsellEligibilityRepository
import ch.protonmail.android.mailupselling.domain.repository.UpsellOptOutRepository
import me.proton.android.core.payment.domain.model.ProductOfferList
import me.proton.android.core.payment.domain.model.filterForTags
import me.proton.android.core.payment.domain.usecase.GetCurrentSubscriptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class ObservePlusToUnlimitedEligibility @Inject constructor(
    private val observeUser: ObserveUser,
    private val upsellEligibilityRepository: UpsellEligibilityRepository,
    private val upsellOptOutRepository: UpsellOptOutRepository,
    private val availableUpgradesCache: AvailableUpgradesCache,
    private val getCurrentSubscriptions: GetCurrentSubscriptions,
    private val isEligibleForBlackFridayPromotion: IsEligibleForBlackFridayPromotion,
    @IsPlusToUnlimitedUpsellEnabled private val regularUpsellEnabled: FeatureFlag<Boolean>,
    @IsSdkUpgradesReadEnabled private val sdkUpgradesReadEnabled: FeatureFlag<Boolean>,
    @IsSdkUpgradesPurchaseEnabled private val sdkUpgradesPurchaseEnabled: FeatureFlag<Boolean>,
    @IsBlackFridayUpsellOptedOut private val blackFridayOptedOut: FeatureFlag<Boolean>
) {

    operator fun invoke(userId: UserId): Flow<Either<DataError, PlusToUnlimitedOffer>> = observeUser(userId)
        .map { it.getOrNull()?.subscribed }
        .distinctUntilChanged()
        .combine(upsellOptOutRepository.observeOptOutChanges()) { subscribed, _ -> subscribed }
        .flatMapLatest { observeOffer(userId) }

    private fun observeOffer(userId: UserId): Flow<Either<DataError, PlusToUnlimitedOffer>> = flow {
        if (!sdkUpgradesReadEnabled.get() || !sdkUpgradesPurchaseEnabled.get()) {
            Timber.d("upsell: Payments SDK flags disabled, not eligible for Plus to Unlimited")
            emit(PlusToUnlimitedOffer.None.right())
            return@flow
        }

        val eligibility = upsellEligibilityRepository.getPlusToUnlimitedEligibility(userId)
        val rustOffer = eligibility.getOrNull()
        if (rustOffer == null || rustOffer == PlusToUnlimitedOffer.None) {
            emit(eligibility)
            return@flow
        }

        val blackFridayBlocked = rustOffer == PlusToUnlimitedOffer.BlackFriday &&
            (!isEligibleForBlackFridayPromotion(userId) || blackFridayOptedOut.get())
        if (blackFridayBlocked && !canFallBackToRegular()) {
            emit(PlusToUnlimitedOffer.None.right())
            return@flow
        }
        val offer = if (blackFridayBlocked) PlusToUnlimitedOffer.Regular else rustOffer

        emitAll(
            availableUpgradesCache.observe(userId).map { upgrades ->
                val unlimitedAvailable = upgrades.hasUnlimitedOffer(offer)
                if (unlimitedAvailable && !isSubscribedToUnlimited()) {
                    offer.right()
                } else {
                    PlusToUnlimitedOffer.None.right()
                }
            }
        )
    }

    private fun List<ProductOfferList>.hasUnlimitedOffer(offer: PlusToUnlimitedOffer): Boolean {
        val unlimitedOffers = filter { it.metadata.planName == PlanUpgradeIds.UnlimitedPlanId }
        return if (offer == PlusToUnlimitedOffer.BlackFriday) {
            unlimitedOffers.filterForTags(
                primaryTag = PlanUpgradeSupportedTags.BlackFriday.value,
                fallbackToBaseOffer = false
            ).isNotEmpty()
        } else {
            unlimitedOffers.isNotEmpty()
        }
    }

    private suspend fun canFallBackToRegular(): Boolean = regularUpsellEnabled.get() && hasYearlySubscription()

    private suspend fun hasYearlySubscription(): Boolean = runCatching {
        getCurrentSubscriptions().any { it.price?.cycle == PlanUpgradeCycle.Yearly.months }
    }.getOrElse {
        Timber.d(it, "upsell: could not read the current subscriptions")
        false
    }

    private suspend fun isSubscribedToUnlimited(): Boolean = runCatching {
        getCurrentSubscriptions().any { it.name == PlanUpgradeIds.UnlimitedPlanId }
    }.getOrElse {
        Timber.d(it, "upsell: could not read the current subscriptions")
        false
    }
}
