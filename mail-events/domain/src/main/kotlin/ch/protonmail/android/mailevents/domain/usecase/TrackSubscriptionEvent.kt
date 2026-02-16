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

package ch.protonmail.android.mailevents.domain.usecase

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailevents.domain.model.AppEvent
import ch.protonmail.android.mailevents.domain.repository.EventsRepository
import javax.inject.Inject

class TrackSubscriptionEvent @Inject constructor(
    private val eventsRepository: EventsRepository
) {

    @Suppress("LongParameterList")
    suspend operator fun invoke(
        contentList: List<String>,
        price: Double,
        currency: String,
        cycle: Int,
        couponCode: String? = null,
        transactionId: String? = null,
        isFirstPurchase: Boolean,
        isFreeToPaid: Boolean
    ): Either<DataError, Unit> {
        val event = AppEvent.Subscription(
            contentList = contentList,
            price = price,
            currency = currency,
            cycle = cycle,
            couponCode = couponCode,
            transactionId = transactionId,
            isFirstPurchase = isFirstPurchase,
            isFreeToPaid = isFreeToPaid
        )
        return eventsRepository.sendEvent(event)
    }
}
