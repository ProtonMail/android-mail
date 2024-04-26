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

import ch.protonmail.android.mailupselling.domain.extensions.currentPrice
import me.proton.core.plan.domain.entity.DynamicPlanInstance
import javax.inject.Inject
import kotlin.math.roundToInt

class GetDiscountRate @Inject constructor() {

    operator fun invoke(shorterInstance: DynamicPlanInstance, longerInstance: DynamicPlanInstance): Int? {
        val longerInstancePrice = longerInstance.currentPrice
        val shorterInstancePrice = shorterInstance.currentPrice

        if (longerInstancePrice == null || shorterInstancePrice == null) return null

        val safeRatio = kotlin.runCatching {
            @Suppress("UnnecessaryParentheses")
            (longerInstancePrice / longerInstance.cycle) /
                (shorterInstancePrice / shorterInstance.cycle)
        }
            .getOrNull()
            ?.takeIf { it > 0f && it <= 1f }
            ?: return null

        @Suppress("MagicNumber")
        return ((1 - safeRatio) * 100).takeIf { it > 0 }?.roundToInt()
    }
}
