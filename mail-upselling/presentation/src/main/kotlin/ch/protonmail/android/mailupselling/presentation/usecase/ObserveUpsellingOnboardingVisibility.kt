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
import ch.protonmail.android.mailupselling.domain.annotations.UpsellingOnboardingEnabled
import ch.protonmail.android.mailupselling.domain.usecase.UserHasPendingPurchases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import me.proton.core.payment.domain.PurchaseManager
import me.proton.core.plan.domain.usecase.CanUpgradeFromMobile
import javax.inject.Inject

class ObserveUpsellingOnboardingVisibility @Inject constructor(
    private val observePrimaryUser: ObservePrimaryUser,
    private val purchaseManager: PurchaseManager,
    private val canUpgradeFromMobile: CanUpgradeFromMobile,
    private val userHasPendingPurchases: UserHasPendingPurchases,
    @UpsellingOnboardingEnabled private val isFeatureFlagEnabled: Boolean,
    private val getOnboardingUpsellingPlans: GetOnboardingUpsellingPlans
) {

    operator fun invoke(): Flow<Boolean> = combine(
        observePrimaryUser().distinctUntilChanged(),
        purchaseManager.observePurchases()
    ) { user, purchases ->
        if (user == null) return@combine false

        if (!isFeatureFlagEnabled) return@combine false
        if (!canUpgradeFromMobile(user.userId)) return@combine false
        if (userHasPendingPurchases(purchases, user.userId)) return@combine false

        getOnboardingUpsellingPlans(user.userId).isRight()
    }
}
