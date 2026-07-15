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

import ch.protonmail.android.mailfeatureflags.domain.FeatureFlagVariantProvider
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlagVariantPayloadType
import ch.protonmail.android.mailfeatureflags.domain.model.UpsellPlanExperiment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

enum class UpsellVariantPlan {
    MAIL_PLUS,
    UNLIMITED;
    companion object {
        val Default = MAIL_PLUS
    }
}

class ResolveUpsellVariant @Inject constructor(
    private val variantProvider: FeatureFlagVariantProvider
) {

    suspend operator fun invoke(): UpsellVariantPlan {
        val variant = variantProvider.getVariant(UpsellPlanExperiment.key) ?: return UpsellVariantPlan.Default
        val payload = variant.payload
            ?.takeIf { variant.enabled && it.type == FeatureFlagVariantPayloadType.JSON }
            ?: return UpsellVariantPlan.Default

        return runCatching {
            when (json.decodeFromString<UpsellPayload>(payload.value).upsell) {
                UpsellPayload.Plan.UNLIMITED -> UpsellVariantPlan.UNLIMITED
                UpsellPayload.Plan.MAIL_PLUS -> UpsellVariantPlan.MAIL_PLUS
            }
        }.getOrDefault(UpsellVariantPlan.Default)
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}

@Serializable
private data class UpsellPayload(val upsell: Plan) {

    @Serializable
    enum class Plan {
        @SerialName("MailPlus") MAIL_PLUS,

        @SerialName("Unlimited") UNLIMITED
    }
}
