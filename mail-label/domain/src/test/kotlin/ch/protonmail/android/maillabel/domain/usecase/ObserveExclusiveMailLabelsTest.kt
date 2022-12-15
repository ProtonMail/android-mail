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

import app.cash.turbine.test
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.toMailLabelSystem
import ch.protonmail.android.testdata.maillabel.MailLabelTestData.buildCustomFolder
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

internal class ObserveExclusiveMailLabelsTest {

    private val userId = UserIdTestData.userId

    private val observeExclusiveDestinationMailLabels = mockk<ObserveExclusiveDestinationMailLabels> {
        every { this@mockk.invoke(userId) } returns flowOf(
            MailLabels(
                systemLabels = SystemLabelId.exclusiveDestinationList.map { it.toMailLabelSystem() },
                folders = listOf(
                    buildCustomFolder("custom1"),
                    buildCustomFolder("custom2")
                ),
                labels = emptyList()
            )
        )
    }

    @Test
    fun `return exclusive list with Draft and Sent labels`() = runTest {
        // When
        ObserveExclusiveMailLabels(observeExclusiveDestinationMailLabels).invoke(userId).test {
            // Then
            val item = awaitItem()
            assertEquals(SystemLabelId.exclusiveList.map { it.toMailLabelSystem() }, item.systemLabels)
            cancelAndIgnoreRemainingEvents()
        }
    }

}
