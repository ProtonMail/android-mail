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
import ch.protonmail.android.mailupselling.domain.annotations.UpsellingAutodeleteEnabled
import ch.protonmail.android.mailupselling.domain.annotations.UpsellingMobileSignatureEnabled
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.usecase.UserHasAvailablePlans
import ch.protonmail.android.mailupselling.domain.usecase.UserHasPendingPurchases
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsUpsellingContactGroupsEnabled
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsUpsellingFoldersEnabled
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsUpsellingLabelsEnabled
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.ObserveOneClickUpsellingEnabled
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.payment.domain.PurchaseManager
import me.proton.core.plan.domain.usecase.CanUpgradeFromMobile
import javax.inject.Inject

class ObserveUpsellingVisibility @Inject constructor(
    private val observePrimaryUser: ObservePrimaryUser,
    private val purchaseManager: PurchaseManager,
    private val canUpgradeFromMobile: CanUpgradeFromMobile,
    private val userHasAvailablePlans: UserHasAvailablePlans,
    private val userHasPendingPurchases: UserHasPendingPurchases,
    @UpsellingMobileSignatureEnabled private val isUpsellingMobileSignatureEnabled: Boolean,
    private val isUpsellingLabelsEnabled: IsUpsellingLabelsEnabled,
    private val isUpsellingFoldersEnabled: IsUpsellingFoldersEnabled,
    private val isUpsellingContactGroupsEnabled: IsUpsellingContactGroupsEnabled,
    private val observeOneClickUpsellingEnabled: ObserveOneClickUpsellingEnabled,
    @UpsellingAutodeleteEnabled private val isUpsellingAutoDeleteEnabled: Boolean
) {

    operator fun invoke(upsellingEntryPoint: UpsellingEntryPoint.Feature): Flow<Boolean> = combine(
        observePrimaryUser().distinctUntilChanged(),
        purchaseManager.observePurchases()
    ) { user, purchases ->
        if (user == null) return@combine false

        if (isFeatureFlagOff(upsellingEntryPoint)) return@combine false

        if (!canUpgradeFromMobile()) return@combine false
        if (userHasPendingPurchases(purchases, user.userId)) return@combine false

        userHasAvailablePlans(user.userId)
    }

    private suspend fun isFeatureFlagOff(upsellingEntryPoint: UpsellingEntryPoint.Feature): Boolean {
        return !when (upsellingEntryPoint) {
            UpsellingEntryPoint.Feature.ContactGroups -> isUpsellingContactGroupsEnabled()
            UpsellingEntryPoint.Feature.Folders -> isUpsellingFoldersEnabled()
            UpsellingEntryPoint.Feature.Labels -> isUpsellingLabelsEnabled()
            UpsellingEntryPoint.Feature.Mailbox,
            UpsellingEntryPoint.Feature.Navbar -> {
                observeOneClickUpsellingEnabled(null).firstOrNull()?.value == true
            }

            UpsellingEntryPoint.Feature.MobileSignature -> isUpsellingMobileSignatureEnabled
            UpsellingEntryPoint.Feature.AutoDelete -> isUpsellingAutoDeleteEnabled
        }
    }
}
