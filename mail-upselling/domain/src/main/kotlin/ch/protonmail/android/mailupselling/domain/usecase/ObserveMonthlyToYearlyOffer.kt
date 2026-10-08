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
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsSdkUpgradesPurchaseEnabled
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsSdkUpgradesReadEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailupselling.domain.cache.AvailableUpgradesCache
import ch.protonmail.android.mailupselling.domain.model.MonthlyToYearlyOffer
import ch.protonmail.android.mailupselling.domain.repository.UpsellEligibilityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import me.proton.android.core.payment.domain.model.ProductOfferList
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class ObserveMonthlyToYearlyOffer @Inject constructor(
    private val upsellEligibilityRepository: UpsellEligibilityRepository,
    private val availableUpgradesCache: AvailableUpgradesCache,
    private val getMonthlyToYearlyOffer: GetMonthlyToYearlyOffer,
    @IsSdkUpgradesReadEnabled private val sdkUpgradesReadEnabled: FeatureFlag<Boolean>,
    @IsSdkUpgradesPurchaseEnabled private val sdkUpgradesPurchaseEnabled: FeatureFlag<Boolean>
) {

    operator fun invoke(userId: UserId): Flow<Either<DataError, MonthlyToYearlyOffer?>> = flow {
        if (!sdkUpgradesReadEnabled.get() || !sdkUpgradesPurchaseEnabled.get()) {
            Timber.d("upsell: Payments SDK flags disabled, no monthly to yearly offer")
            emit(null.right())
            return@flow
        }

        emitAll(availableUpgradesCache.observe(userId).mapLatest { upgrades -> offerFor(userId, upgrades) })
    }

    private suspend fun offerFor(
        userId: UserId,
        upgrades: List<ProductOfferList>
    ): Either<DataError, MonthlyToYearlyOffer?> {
        if (upgrades.isEmpty()) {
            Timber.d("upsell: no upgrades loaded, no monthly to yearly offer")
            return null.right()
        }
        return upsellEligibilityRepository.getMonthlyToYearlyEligibility(userId)
            .map { eligible -> if (eligible) getMonthlyToYearlyOffer(upgrades) else null }
    }
}
