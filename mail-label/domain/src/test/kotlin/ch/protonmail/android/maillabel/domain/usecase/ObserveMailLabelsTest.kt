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
import ch.protonmail.android.maillabel.domain.model.LabelType
import ch.protonmail.android.maillabel.domain.repository.LabelRepository
import ch.protonmail.android.testdata.label.LabelTestData.buildLabel
import ch.protonmail.android.testdata.maillabel.MailLabelTestData.buildCustomFolder
import ch.protonmail.android.testdata.maillabel.MailLabelTestData.buildCustomLabel
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ObserveMailLabelsTest {

    private val userId = UserIdTestData.userId

    private val labelRepository = mockk<LabelRepository> {
        every { observeCustomFolders(any()) } returns flowOf(
            listOf(
                buildLabel(id = "id0", type = LabelType.MessageFolder, order = 0),
                buildLabel(id = "id1", type = LabelType.MessageFolder, order = 1),
                buildLabel(id = "id2", type = LabelType.MessageFolder, order = 2)
            )
        )
        every { observeCustomLabels(any()) } returns flowOf(
            listOf(
                buildLabel(id = "id3", type = LabelType.MessageLabel, order = 0),
                buildLabel(id = "id4", type = LabelType.MessageLabel, order = 1),
                buildLabel(id = "id5", type = LabelType.MessageLabel, order = 2)
            )
        )
        every { observeSystemLabels(userId) } returns flowOf(emptyList())
    }

    private val TestScope.observeMailLabels
        get() = ObserveMailLabels(
            dispatcher = UnconfinedTestDispatcher(testScheduler),
            labelRepository = labelRepository
        )

    @BeforeTest
    fun setUp() {
        mockkStatic(Color::parseColor)
        every { Color.parseColor(any()) } returns 0
    }

    @AfterTest
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
            assertEquals(3, item.labels.size)
            assertEquals(
                expected = listOf(
                    buildCustomFolder("id0", order = 0),
                    buildCustomFolder("id1", order = 1),
                    buildCustomFolder("id2", order = 2)
                ),
                actual = item.folders
            )
            assertEquals(
                expected = listOf(
                    buildCustomLabel("id3", order = 0),
                    buildCustomLabel("id4", order = 1),
                    buildCustomLabel("id5", order = 2)
                ),
                actual = item.labels
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `return correct folders parent on success`() = runTest {

        val labels = listOf(
            buildLabel(id = "id0", type = LabelType.MessageFolder, order = 0),
            buildLabel(id = "id0.1", type = LabelType.MessageFolder, order = 0, parentId = "id0"),
            buildLabel(id = "id0.2", type = LabelType.MessageFolder, order = 1, parentId = "id0"),
            buildLabel(id = "id0.2.1", type = LabelType.MessageFolder, order = 0, parentId = "id0.2"),
            buildLabel(id = "id0.2.2", type = LabelType.MessageFolder, order = 1, parentId = "id0.2")
        )
        every { labelRepository.observeCustomFolders(any()) } returns flowOf(labels)

        // When
        observeMailLabels.invoke(userId).test {
            // Then
            val item = awaitItem()

            val f0 = buildCustomFolder("id0", level = 0, order = 0, parent = null, children = listOf("id0.1", "id0.2"))
            val f01 = buildCustomFolder("id0.1", level = 1, order = 0, parent = f0)
            val f02 =
                buildCustomFolder("id0.2", level = 1, order = 1, parent = f0, children = listOf("id0.2.1", "id0.2.2"))
            val f021 = buildCustomFolder("id0.2.1", level = 2, order = 0, parent = f02)
            val f022 = buildCustomFolder("id0.2.2", level = 2, order = 1, parent = f02)
            assertEquals(
                expected = listOf(f0, f01, f02, f021, f022),
                actual = item.folders
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `return correct folders order on success`() = runTest {

        every { labelRepository.observeCustomFolders(any()) } returns flowOf(
            listOf(
                buildLabel(
                    id = "id0.1",
                    type = LabelType.MessageFolder,
                    order = 1,
                    parentId = "id0"
                ),
                buildLabel(
                    id = "id0.0",
                    type = LabelType.MessageFolder,
                    order = 0,
                    parentId = "id0"
                ),
                buildLabel(id = "id2", type = LabelType.MessageFolder, order = 2),
                buildLabel(id = "id0", type = LabelType.MessageFolder, order = 0),
                buildLabel(id = "id1", type = LabelType.MessageFolder, order = 1)
            )
        )

        // When
        observeMailLabels.invoke(userId).test {
            // Then
            val item = awaitItem()

            val f0 = buildCustomFolder("id0", level = 0, order = 0, parent = null, children = listOf("id0.0", "id0.1"))
            val f00 = buildCustomFolder("id0.0", level = 1, order = 0, parent = f0)
            val f01 = buildCustomFolder("id0.1", level = 1, order = 1, parent = f0)
            val f1 = buildCustomFolder("id1", level = 0, order = 1, parent = null)
            val f2 = buildCustomFolder("id2", level = 0, order = 2, parent = null)

            assertEquals(
                expected = listOf(f0, f00, f01, f1, f2),
                actual = item.folders
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `propagates the persisted isExpanded value from the repository`() = runTest {
        every { labelRepository.observeCustomFolders(any()) } returns flowOf(
            listOf(
                buildLabel(id = "id0", type = LabelType.MessageFolder, order = 0, isExpanded = false),
                buildLabel(id = "id1", type = LabelType.MessageFolder, order = 1, isExpanded = true)
            )
        )

        observeMailLabels.invoke(userId).test {
            val item = awaitItem()

            assertFalse(item.folders.single { it.id.labelId.id == "id0" }.isExpanded)
            assertTrue(item.folders.single { it.id.labelId.id == "id1" }.isExpanded)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
