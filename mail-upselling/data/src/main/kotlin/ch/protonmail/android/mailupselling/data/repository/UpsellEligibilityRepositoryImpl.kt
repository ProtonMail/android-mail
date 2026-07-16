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

package ch.protonmail.android.mailupselling.data.repository

import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailupselling.domain.repository.UpsellEligibilityRepository
import ch.protonmail.android.mailupselling.domain.usecase.UpsellVariantPlan
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.MailUserSessionUpsellEligibilityResult
import uniffi.mail_uniffi.UpsellEligibility
import uniffi.mail_uniffi.UpsellType
import uniffi.mail_uniffi.upsellExperimentFlagForAndroid
import javax.inject.Inject

class UpsellEligibilityRepositoryImpl @Inject constructor(
    private val executeWithUserSession: ExecuteWithUserSession
) : UpsellEligibilityRepository {

    override suspend fun getEligibleUpsellPlan(userId: UserId): UpsellVariantPlan? =
        executeWithUserSession(userId) { session ->
            when (val result = session.getRustUserSession().upsellEligibility(upsellExperimentFlagForAndroid())) {
                is MailUserSessionUpsellEligibilityResult.Ok -> result.v1.toUpsellPlan()
                is MailUserSessionUpsellEligibilityResult.Error -> null
            }
        }.getOrNull()
}

private fun UpsellEligibility.toUpsellPlan(): UpsellVariantPlan? = when (this) {
    is UpsellEligibility.Eligible -> when (v1) {
        UpsellType.MAIL_PLUS -> UpsellVariantPlan.MAIL_PLUS
        UpsellType.UNLIMITED -> UpsellVariantPlan.UNLIMITED
    }

    UpsellEligibility.NotEligible -> null
}
