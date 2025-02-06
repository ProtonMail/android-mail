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

package ch.protonmail.android.maillabel.domain.usecase

import android.graphics.Color
import app.cash.turbine.test
import arrow.core.Either
import ch.protonmail.android.maillabel.domain.model.toMailLabelCustom
import ch.protonmail.android.testdata.label.LabelTestData.buildLabel
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelType
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class ObserveCustomMailLabelsTest {

    private val userId = UserIdTestData.userId
    private val observeLabels = mockk<ObserveLabels> {
        every { this@mockk.invoke(any(), labelType = LabelType.MessageLabel) } returns flowOf(
            Either.Right(
                value = listOf(
                    buildLabel(userId = userId, type = LabelType.MessageLabel, id = "id0", order = 0),
                    buildLabel(userId = userId, type = LabelType.MessageLabel, id = "id2", order = 2),
                    buildLabel(userId = userId, type = LabelType.MessageLabel, id = "id1", order = 1)
                )
            )
        )
    }

    private val observeCustomLabels = ObserveCustomMailLabels(observeLabels)

    @Before
    fun setUp() {
        mockkStatic(Color::parseColor)
        every { Color.parseColor(any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Color::parseColor)
    }

    @Test
    fun `return correct value and order on success`() = runTest {
        observeCustomLabels(userId).test {
            val item = awaitItem()
            val result = checkNotNull(item.orNull())

            assertEquals(3, result.size)
            assertEquals(
                expected = listOf(
                    buildLabel(userId = userId, type = LabelType.MessageLabel, id = "id0", order = 0),
                    buildLabel(userId = userId, type = LabelType.MessageLabel, id = "id1", order = 1),
                    buildLabel(userId = userId, type = LabelType.MessageLabel, id = "id2", order = 2)
                ).toMailLabelCustom(),
                actual = result
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

}
