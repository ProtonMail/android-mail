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
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.toMailLabelSystem
import ch.protonmail.android.testdata.label.LabelTestData.buildLabel
import ch.protonmail.android.testdata.label.LabelTestData.systemLabelsAsMessageFolders
import ch.protonmail.android.testdata.maillabel.MailLabelTestData.buildCustomFolder
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelType
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class ObserveExclusiveDestinationMailLabelsTest {

    private val userId = UserIdTestData.userId

    private val observeLabels = mockk<ObserveLabels> {
        every { this@mockk.invoke(any(), labelType = LabelType.MessageFolder) } returns flowOf(
            Either.Right(
                value = listOf(
                    buildLabel(userId = userId, type = LabelType.MessageFolder, id = "A", order = 0),
                    buildLabel(userId = userId, type = LabelType.MessageFolder, id = "B", order = 1),
                    buildLabel(userId = userId, type = LabelType.MessageFolder, id = "C", order = 2)
                )
            )
        )
    }

    private val TestScope.observeMailLabels
        get() = ObserveExclusiveDestinationMailLabels(
            observeLabels = observeLabels
        )

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
    fun `return correct value on success`() = runTest {
        // When
        observeMailLabels.invoke(userId).test {
            // Then
            val item = awaitItem()
            assertEquals(3, item.folders.size)
            assertEquals(
                expected = MailLabels(
                    systemLabels = SystemLabelId.exclusiveDestinationList.map { it.toMailLabelSystem() },
                    folders = listOf(
                        buildCustomFolder("A", order = 0),
                        buildCustomFolder("B", order = 1),
                        buildCustomFolder("C", order = 2)
                    ),
                    labels = emptyList()
                ),
                actual = item
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `return correct folder hierarchy on success`() = runTest {
        // Given
        val labels = listOf(
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "A", order = 0),
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "A.B", order = 0, parentId = "A"),
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "A.C", order = 1, parentId = "A"),
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "A.C.D", order = 0, parentId = "A.C"),
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "A.C.E", order = 1, parentId = "A.C")
        )
        every { observeLabels(any(), labelType = LabelType.MessageFolder) } returns flowOf(
            Either.Right(
                value = labels
            )
        )

        // When
        observeMailLabels.invoke(userId).test {
            // Then
            val item = awaitItem()

            val f0 = buildCustomFolder("A", level = 0, order = 0, parent = null, children = listOf("A.B", "A.C"))
            val f01 = buildCustomFolder("A.B", level = 1, order = 0, parent = f0)
            val f02 = buildCustomFolder("A.C", level = 1, order = 1, parent = f0, children = listOf("A.C.D", "A.C.E"))
            val f021 = buildCustomFolder("A.C.D", level = 2, order = 0, parent = f02)
            val f022 = buildCustomFolder("A.C.E", level = 2, order = 1, parent = f02)
            assertEquals(
                expected = listOf(f0, f01, f02, f021, f022),
                actual = item.folders
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `return correct folders order on success`() = runTest {
        // Given
        every { observeLabels(any(), labelType = LabelType.MessageFolder) } returns flowOf(
            Either.Right(
                value = listOf(
                    buildLabel(userId = userId, type = LabelType.MessageFolder, id = "A", order = 0),
                    buildLabel(userId = userId, type = LabelType.MessageFolder, id = "A.A", order = 0, parentId = "A"),
                    buildLabel(userId = userId, type = LabelType.MessageFolder, id = "A.B", order = 1, parentId = "A"),
                    buildLabel(userId = userId, type = LabelType.MessageFolder, id = "B", order = 1),
                    buildLabel(userId = userId, type = LabelType.MessageFolder, id = "C", order = 2)
                )
            )
        )

        // When
        observeMailLabels.invoke(userId).test {
            // Then
            val item = awaitItem()

            val f0 = buildCustomFolder("A", level = 0, order = 0, parent = null, children = listOf("A.A", "A.B"))
            val f00 = buildCustomFolder("A.A", level = 1, order = 0, parent = f0)
            val f01 = buildCustomFolder("A.B", level = 1, order = 1, parent = f0)
            val f1 = buildCustomFolder("B", level = 0, order = 1, parent = null)
            val f2 = buildCustomFolder("C", level = 0, order = 2, parent = null)

            assertEquals(
                expected = listOf(f0, f00, f01, f1, f2),
                actual = item.folders
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `does not return system folders as custom folders`() = runTest {
        // Given
        every { observeLabels(any(), labelType = LabelType.MessageFolder) } returns flowOf(
            Either.Right(
                value = listOf(
                    buildLabel(userId = userId, type = LabelType.MessageFolder, id = "A", order = 2),
                    buildLabel(userId = userId, type = LabelType.MessageFolder, id = "A.A", order = 0, parentId = "A"),
                    buildLabel(userId = userId, type = LabelType.MessageFolder, id = "A.B", order = 1, parentId = "A"),
                    buildLabel(userId = userId, type = LabelType.MessageFolder, id = "B", order = 2),
                    *systemLabelsAsMessageFolders(userId)
                )
            )
        )

        // When
        observeMailLabels.invoke(userId).test {
            // Then
            val item = awaitItem()

            val f0 = buildCustomFolder("A", level = 0, order = 2, parent = null, children = listOf("A.A", "A.B"))
            val f00 = buildCustomFolder("A.A", level = 1, order = 0, parent = f0)
            val f01 = buildCustomFolder("A.B", level = 1, order = 1, parent = f0)
            val f1 = buildCustomFolder("B", level = 0, order = 2, parent = null)

            assertEquals(
                expected = listOf(f0, f00, f01, f1),
                actual = item.folders
            )
            cancelAndIgnoreRemainingEvents()
        }
    }
}
