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

package ch.protonmail.android.mailupselling.domain.usecase

import ch.protonmail.android.mailfeatureflags.domain.annotation.IsSummerCampaign2026Enabled
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsSummerCampaign2026Wave2Enabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailupselling.domain.model.SummerCampaignPhase
import javax.inject.Inject

class GetCurrentSummerCampaignPhase @Inject constructor(
    @IsSummerCampaign2026Enabled private val wave1Flag: FeatureFlag<Boolean>,
    @IsSummerCampaign2026Wave2Enabled private val wave2Flag: FeatureFlag<Boolean>
) {

    suspend operator fun invoke() = when {
        // Wave2 **ALWAYS** takes precedence over Wave1 when both are enabled
        wave2Flag.get() -> SummerCampaignPhase.Active.Wave2
        wave1Flag.get() -> SummerCampaignPhase.Active.Wave1
        else -> SummerCampaignPhase.None
    }
}
