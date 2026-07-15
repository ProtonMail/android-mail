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
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlagVariant
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlagVariantPayload
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlagVariantPayloadType
import ch.protonmail.android.mailfeatureflags.domain.model.UpsellPlanExperiment
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ResolveUpsellVariantTest {

    private val variantProvider = mockk<FeatureFlagVariantProvider>()
    private val resolveUpsellVariant = ResolveUpsellVariant(variantProvider)

    private fun jsonVariant(value: String, enabled: Boolean = true) = FeatureFlagVariant(
        name = "variant",
        enabled = enabled,
        payload = FeatureFlagVariantPayload(FeatureFlagVariantPayloadType.JSON, value)
    )

    private fun stubVariant(variant: FeatureFlagVariant?) {
        coEvery { variantProvider.getVariant(UpsellPlanExperiment.key) } returns variant
    }

    @Test
    fun `resolves Unlimited from the payload`() = runTest {
        stubVariant(jsonVariant("""{"upsell":"Unlimited"}"""))
        assertEquals(UpsellVariantPlan.UNLIMITED, resolveUpsellVariant())
    }

    @Test
    fun `resolves MailPlus from the payload`() = runTest {
        stubVariant(jsonVariant("""{"upsell":"MailPlus"}"""))
        assertEquals(UpsellVariantPlan.MAIL_PLUS, resolveUpsellVariant())
    }

    @Test
    fun `ignores unknown web-offer fields alongside the upsell type`() = runTest {
        stubVariant(jsonVariant("""{"upsell":"Unlimited","type":"blackFriday","coupon":"BF"}"""))
        assertEquals(UpsellVariantPlan.UNLIMITED, resolveUpsellVariant())
    }

    @Test
    fun `falls back to MailPlus when there is no variant`() = runTest {
        stubVariant(null)
        assertEquals(UpsellVariantPlan.MAIL_PLUS, resolveUpsellVariant())
    }

    @Test
    fun `falls back to MailPlus when the variant is disabled`() = runTest {
        stubVariant(jsonVariant("""{"upsell":"Unlimited"}""", enabled = false))
        assertEquals(UpsellVariantPlan.MAIL_PLUS, resolveUpsellVariant())
    }

    @Test
    fun `falls back to MailPlus when the payload is not JSON`() = runTest {
        stubVariant(
            FeatureFlagVariant(
                name = "variant",
                enabled = true,
                payload = FeatureFlagVariantPayload(FeatureFlagVariantPayloadType.STRING, "Unlimited")
            )
        )
        assertEquals(UpsellVariantPlan.MAIL_PLUS, resolveUpsellVariant())
    }

    @Test
    fun `falls back to MailPlus when the payload is malformed`() = runTest {
        stubVariant(jsonVariant("""{"upsell":"Enterprise"}"""))
        assertEquals(UpsellVariantPlan.MAIL_PLUS, resolveUpsellVariant())
    }

    @Test
    fun `falls back to MailPlus when there is no payload`() = runTest {
        stubVariant(FeatureFlagVariant(name = "variant", enabled = true, payload = null))
        assertEquals(UpsellVariantPlan.MAIL_PLUS, resolveUpsellVariant())
    }
}
