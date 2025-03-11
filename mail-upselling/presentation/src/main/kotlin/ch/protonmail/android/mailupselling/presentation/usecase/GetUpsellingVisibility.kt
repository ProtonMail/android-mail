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
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import kotlinx.coroutines.flow.first
import me.proton.core.payment.domain.PurchaseManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetUpsellingVisibility @Inject constructor(
    private val resolveUpsellingVisibility: ResolveUpsellingVisibility,
    private val observePrimaryUser: ObservePrimaryUser,
    private val purchaseManager: PurchaseManager,
    private val cache: UpsellingVisibilityCache
) {

    suspend operator fun invoke(upsellingEntryPoint: UpsellingEntryPoint.Feature): Boolean {
        val user = observePrimaryUser().first() ?: return false
        val cachedValue = cache.retrieve(upsellingEntryPoint)
        if (cachedValue != null) {
            return cachedValue
        }
        val resolved = resolveUpsellingVisibility(
            user = user,
            purchases = purchaseManager.getPurchases(),
            upsellingEntryPoint = upsellingEntryPoint
        )
        cache.store(upsellingEntryPoint, resolved)
        return resolved
    }
}
