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

import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailupselling.domain.cache.AvailableUpgradesCache
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeIds
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.proton.android.core.payment.domain.model.ProductOfferDetail
import me.proton.android.core.payment.domain.model.filterForTags
import javax.inject.Inject

class ObserveUnlimitedPlanUpgrades @Inject constructor(
    private val cache: AvailableUpgradesCache,
    private val observePrimaryUserId: ObservePrimaryUserId
) {

    operator fun invoke() = observePrimaryUserId().flatMapLatest { userId ->
        userId ?: return@flatMapLatest flowOf(emptyList())

        cache.observe(userId).map { upgrades ->
            val eligibleOffers = upgrades.filterForTags(primaryTag = null, fallbackToBaseOffer = true)
            eligibleOffers.filterUnlimited()
        }
    }

    private fun List<ProductOfferDetail>.filterUnlimited() =
        filter { it.metadata.planName == PlanUpgradeIds.UnlimitedPlanId }
}
