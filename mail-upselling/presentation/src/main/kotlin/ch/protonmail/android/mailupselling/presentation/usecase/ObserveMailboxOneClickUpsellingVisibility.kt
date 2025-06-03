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

package ch.protonmail.android.mailupselling.presentation.usecase

import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailupselling.domain.annotations.OneClickUpsellingAlwaysShown
import ch.protonmail.android.mailupselling.domain.usecase.GetPromotionStatus
import ch.protonmail.android.mailupselling.domain.usecase.PromoStatus
import ch.protonmail.android.mailupselling.domain.usecase.UserHasPendingPurchases
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.ObserveOneClickUpsellingEnabled
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import me.proton.core.domain.entity.UserId
import me.proton.core.payment.domain.PurchaseManager
import me.proton.core.plan.domain.usecase.CanUpgradeFromMobile
import javax.inject.Inject

class ObserveMailboxOneClickUpsellingVisibility @Inject constructor(
    private val observePrimaryUser: ObservePrimaryUser,
    private val purchaseManager: PurchaseManager,
    private val observeOneClickUpsellingEnabled: ObserveOneClickUpsellingEnabled,
    private val observeUpsellingOneClickOnCooldown: ObserveUpsellingOneClickOnCooldown,
    private val canUpgradeFromMobile: CanUpgradeFromMobile,
    private val isPromotionEnabled: GetPromotionStatus,
    private val userHasPendingPurchases: UserHasPendingPurchases,
    @OneClickUpsellingAlwaysShown private val alwaysShowOneClickUpselling: Boolean,
    private val observeDriveSpotlightVisibility: ObserveDriveSpotlightVisibility
) {

    operator fun invoke(): Flow<UpsellingVisibility> = observePrimaryUser()
        .distinctUntilChanged()
        .flatMapLatest { user ->
            if (user == null) return@flatMapLatest flowOf(UpsellingVisibility.HIDDEN)
            combine(
                purchaseManager.observePurchases(),
                observeUpsellingOneClickOnCooldown(),
                observeOneClickUpsellingEnabled(null),
                observeDriveSpotlightVisibility(user)
            ) { purchases, isOneClickOnCooldown, isOneClickUpsellingEnabled, driveSpotlightVisible ->
                if (driveSpotlightVisible) return@combine UpsellingVisibility.DRIVE_SPOTLIGHT

                if (!canUpgradeFromMobile(user.userId)) return@combine UpsellingVisibility.HIDDEN
                if (isOneClickUpsellingEnabled == null || !isOneClickUpsellingEnabled.value)
                    return@combine UpsellingVisibility.HIDDEN
                if (isOneClickOnCooldown && !alwaysShowOneClickUpselling) return@combine UpsellingVisibility.HIDDEN
                if (userHasPendingPurchases(purchases, user.userId)) return@combine UpsellingVisibility.HIDDEN

                user.userId.resolvePromoVisibility()
            }
        }

    private suspend fun UserId.resolvePromoVisibility() = when (isPromotionEnabled(this)) {
        PromoStatus.NO_PLANS -> UpsellingVisibility.HIDDEN
        PromoStatus.NORMAL -> UpsellingVisibility.NORMAL
        PromoStatus.PROMO -> UpsellingVisibility.PROMO
    }
}

enum class UpsellingVisibility {
    HIDDEN, PROMO, NORMAL, DRIVE_SPOTLIGHT
}

