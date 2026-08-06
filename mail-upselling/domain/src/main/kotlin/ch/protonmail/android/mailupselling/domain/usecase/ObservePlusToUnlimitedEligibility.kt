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

import ch.protonmail.android.mailfeatureflags.domain.annotation.IsSdkUpgradesPurchaseEnabled
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsSdkUpgradesReadEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailupselling.domain.cache.AvailableUpgradesCache
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeIds
import ch.protonmail.android.mailupselling.domain.repository.UpsellEligibilityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class ObservePlusToUnlimitedEligibility @Inject constructor(
    private val upsellEligibilityRepository: UpsellEligibilityRepository,
    private val availableUpgradesCache: AvailableUpgradesCache,
    @IsSdkUpgradesReadEnabled private val sdkUpgradesReadEnabled: FeatureFlag<Boolean>,
    @IsSdkUpgradesPurchaseEnabled private val sdkUpgradesPurchaseEnabled: FeatureFlag<Boolean>
) {

    operator fun invoke(userId: UserId): Flow<Boolean> = flow {
        if (!sdkUpgradesReadEnabled.get() || !sdkUpgradesPurchaseEnabled.get()) {
            Timber.d("upsell: Payments SDK flags disabled, not eligible for Plus to Unlimited")
            emit(false)
            return@flow
        }

        if (!upsellEligibilityRepository.getPlusToUnlimitedEligibility(userId)) {
            emit(false)
            return@flow
        }

        emitAll(
            availableUpgradesCache.observe(userId).map { upgrades ->
                upgrades.any { it.metadata.planName == PlanUpgradeIds.UnlimitedPlanId }
            }
        )
    }
}
