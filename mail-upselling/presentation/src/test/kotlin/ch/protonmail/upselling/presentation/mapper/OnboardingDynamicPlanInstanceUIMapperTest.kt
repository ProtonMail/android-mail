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

package ch.protonmail.upselling.presentation.mapper

import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingDynamicPlanInstanceUiMapper
import ch.protonmail.android.mailupselling.presentation.model.OnboardingDynamicPlanInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.UserIdUiModel
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

internal class OnboardingDynamicPlanInstanceUIMapperTest {

    private val mapper = OnboardingDynamicPlanInstanceUiMapper()

    @Test
    fun `should return the expected instance ui model`() {
        // Given
        val plan = UpsellingTestData.PlusPlan
        val planInstance = plan.instances[1]!!
        val expected = OnboardingDynamicPlanInstanceUiModel(
            userId = UserIdUiModel(UserId),
            name = UpsellingTestData.PlusPlan.title,
            currency = "EUR",
            cycle = 1,
            viewId = "$UserId$planInstance".hashCode(),
            dynamicPlan = plan
        )

        // When
        val actual = mapper.toUiModel(
            userId = UserId,
            instance = planInstance,
            plan = plan
        )

        // Then
        assertEquals(expected, actual)
    }

    private companion object {

        val UserId = UserId("id")
    }
}
