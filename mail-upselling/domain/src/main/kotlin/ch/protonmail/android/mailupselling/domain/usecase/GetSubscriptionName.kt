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

package ch.protonmail.android.mailupselling.domain.usecase

import java.util.concurrent.ConcurrentHashMap
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailupselling.domain.model.telemetry.data.SubscriptionName
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.domain.usecase.GetCurrentSubscription
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetSubscriptionName @Inject constructor(
    private val getCurrentSubscription: GetCurrentSubscription
) {

    // We cache the values since otherwise it fires an API call all the time via core library.
    private val cache = ConcurrentHashMap<UserId, SubscriptionName>()

    /**
     * Returns the current [SubscriptionName] for the given [UserId].
     * After the first invocation, the value will stay cached unless [refresh] is passed as `true`.
     */
    suspend operator fun invoke(
        userId: UserId,
        refresh: Boolean = false
    ): Either<GetSubscriptionNameError, SubscriptionName> {
        if (refresh) cache.remove(userId)

        return cache.getOrPut(userId) {
            val subscription = runCatching { getCurrentSubscription(userId = userId) }
                .getOrElse { return GetSubscriptionNameError.left() }

            val rawValue = subscription?.plans?.firstOrNull()?.name ?: FreePlanName
            SubscriptionName(rawValue)
        }.right()
    }

    private companion object {

        const val FreePlanName = "free"
    }

    data object GetSubscriptionNameError
}
