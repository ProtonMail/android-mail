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

package ch.protonmail.android.mailupselling.presentation.extension

import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant

internal fun PlanUpgradeVariant.toOfferId(): String? = when (this) {
    is PlanUpgradeVariant.IntroductoryPrice -> INTRO_PRICE
    is PlanUpgradeVariant.BlackFriday.Wave1 -> BLACK_FRIDAY_WAVE1
    is PlanUpgradeVariant.BlackFriday.Wave2 -> BLACK_FRIDAY_WAVE2
    is PlanUpgradeVariant.SpringPromo.Wave1 -> SPRING_WAVE1
    is PlanUpgradeVariant.SpringPromo.Wave2 -> SPRING_WAVE2
    is PlanUpgradeVariant.SummerCampaign.Wave1 -> SUMMER_WAVE1
    is PlanUpgradeVariant.SummerCampaign.Wave2 -> SUMMER_WAVE2
    is PlanUpgradeVariant.FallPromo.Wave1 -> FALL_WAVE1
    is PlanUpgradeVariant.FallPromo.Wave2 -> FALL_WAVE2
    is PlanUpgradeVariant.Normal,
    is PlanUpgradeVariant.SocialProof -> null
}

internal fun UpsellingVisibility.toOfferId(): String? = when (this) {
    is UpsellingVisibility.Promotional.IntroductoryPrice -> INTRO_PRICE
    is UpsellingVisibility.Promotional.BlackFriday.Wave1 -> BLACK_FRIDAY_WAVE1
    is UpsellingVisibility.Promotional.BlackFriday.Wave2 -> BLACK_FRIDAY_WAVE2
    is UpsellingVisibility.Promotional.SpringPromo.Wave1 -> SPRING_WAVE1
    is UpsellingVisibility.Promotional.SpringPromo.Wave2 -> SPRING_WAVE2
    is UpsellingVisibility.Promotional.SummerCampaign.Wave1 -> SUMMER_WAVE1
    is UpsellingVisibility.Promotional.SummerCampaign.Wave2 -> SUMMER_WAVE2
    is UpsellingVisibility.Promotional.FallPromo.Wave1 -> FALL_WAVE1
    is UpsellingVisibility.Promotional.FallPromo.Wave2 -> FALL_WAVE2
    is UpsellingVisibility.Normal,
    is UpsellingVisibility.Hidden -> null
}

private const val INTRO_PRICE = "intro_price"
private const val BLACK_FRIDAY_WAVE1 = "black_friday_wave1"
private const val BLACK_FRIDAY_WAVE2 = "black_friday_wave2"
private const val SPRING_WAVE1 = "spring26_wave1"
private const val SPRING_WAVE2 = "spring26_wave2"
private const val SUMMER_WAVE1 = "summer26_wave1"
private const val SUMMER_WAVE2 = "summer26_wave2"
private const val FALL_WAVE1 = "fall26_wave1"
private const val FALL_WAVE2 = "fall26_wave2"
