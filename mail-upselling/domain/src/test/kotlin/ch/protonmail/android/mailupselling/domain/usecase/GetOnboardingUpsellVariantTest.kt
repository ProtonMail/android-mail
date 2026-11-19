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

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailupselling.domain.model.OnboardingUpsellVariant
import ch.protonmail.android.mailupselling.domain.repository.UpsellEligibilityRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

internal class GetOnboardingUpsellVariantTest {

    private val userId = UserId("user-id")
    private val repository = mockk<UpsellEligibilityRepository>()
    private val getOnboardingUpsellVariant = GetOnboardingUpsellVariant(repository)

    @Test
    fun `returns the variant from the repository`() = runTest {
        coEvery {
            repository.getOnboardingUpsellVariant(userId)
        } returns OnboardingUpsellVariant.SingleOffer.right()

        assertEquals(OnboardingUpsellVariant.SingleOffer, getOnboardingUpsellVariant(userId))
    }

    @Test
    fun `falls back to control when the variant can't be read`() = runTest {
        coEvery { repository.getOnboardingUpsellVariant(userId) } returns DataError.Local.NoUserSession.left()

        assertEquals(OnboardingUpsellVariant.Control, getOnboardingUpsellVariant(userId))
    }
}
