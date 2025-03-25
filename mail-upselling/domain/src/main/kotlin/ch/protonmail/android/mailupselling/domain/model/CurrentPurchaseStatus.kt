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

package ch.protonmail.android.mailupselling.domain.model

import me.proton.core.payment.presentation.viewmodel.ProtonPaymentEvent

data class CurrentPurchaseStatus(
    val flowStatus: FlowStatus
) {
    fun reduce(update: PurchaseStatusUpdate) = copy(
        flowStatus = when (update.event) {
            is ProtonPaymentEvent.GiapSuccess -> FlowStatus.GiapSuccess
            is ProtonPaymentEvent.Loading -> FlowStatus.Initial
            is ProtonPaymentEvent.Error -> FlowStatus.Error
            else -> flowStatus
        }
    )

    fun serializeToString() = flowStatus.name

    enum class FlowStatus { Initial, GiapSuccess, Error }

    companion object {

        val Empty = CurrentPurchaseStatus(
            flowStatus = FlowStatus.Initial
        )


        fun fromString(name: String) = FlowStatus.entries.firstOrNull { it.name == name }?.let {
            CurrentPurchaseStatus(it)
        } ?: Empty

        fun initial(from: PurchaseStatusUpdate) = Empty.reduce(from)
    }
}

data class PurchaseStatusUpdate(val event: ProtonPaymentEvent)
