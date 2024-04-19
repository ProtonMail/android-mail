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

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanInstanceUiMapper
import ch.protonmail.android.mailupselling.presentation.model.DynamicPlanInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.UserIdUiModel
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

internal class DynamicPlanInstanceUIMapperTest {

    private val mapper = DynamicPlanInstanceUiMapper()

    @Test
    fun `should return the expected instance ui model with no discount and no highlights`() {
        // Given
        val plan = UpsellingTestData.PlusPlan
        val planInstance = plan.instances[1]!!
        val expected = DynamicPlanInstanceUiModel(
            userId = UserIdUiModel(UserId),
            name = UpsellingTestData.PlusPlan.title,
            price = TextUiModel.Text("0.1"),
            discount = null,
            currency = "EUR",
            cycle = 1,
            highlighted = false,
            viewId = "$UserId$planInstance".hashCode(),
            dynamicPlan = plan
        )

        // When
        val actual = mapper.toUiModel(
            userId = UserId,
            instance = planInstance,
            discount = null,
            highlighted = false,
            plan = plan
        )

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return the expected instance ui model with discount and highlights`() {
        // Given
        val plan = UpsellingTestData.PlusPlan
        val planInstance = plan.instances[12]!!
        val expected = DynamicPlanInstanceUiModel(
            userId = UserIdUiModel(UserId),
            name = UpsellingTestData.PlusPlan.title,
            price = TextUiModel.Text("0.09"),
            discount = TextUiModel.TextResWithArgs(R.string.upselling_discount_tag, listOf("20")),
            currency = "EUR",
            cycle = 12,
            highlighted = true,
            viewId = "$UserId$planInstance".hashCode(),
            dynamicPlan = plan
        )

        // When
        val actual = mapper.toUiModel(
            userId = UserId,
            instance = planInstance,
            discount = 20,
            highlighted = true,
            plan = plan
        )

        // Then
        assertEquals(expected, actual)
    }

    private companion object {

        val UserId = UserId("id")
    }
}
