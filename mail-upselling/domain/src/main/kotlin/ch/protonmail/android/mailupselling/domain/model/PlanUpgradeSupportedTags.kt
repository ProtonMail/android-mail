/*
 * Copyright (c) 2025 Proton Technologies AG
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

import me.proton.android.core.payment.domain.model.ProductOfferDetail

/**
 * Represents the offers (either promotional or base plan) supported by the current app version.
 *
 * Any offer that does not contain the following tags on Play Console won't be picked up by the app,
 * except the base offer (often untagged).
 */
sealed class PlanUpgradeSupportedTags(val value: String) {

    data object BlackFriday : PlanUpgradeSupportedTags("bf-promo")
    data object IntroductoryPrice : PlanUpgradeSupportedTags("introductory-price")
    data object SpringOffer : PlanUpgradeSupportedTags("spring26")
}

fun ProductOfferDetail.isTaggedWith(tag: PlanUpgradeSupportedTags) = offer.tags.value.contains(tag.value)
