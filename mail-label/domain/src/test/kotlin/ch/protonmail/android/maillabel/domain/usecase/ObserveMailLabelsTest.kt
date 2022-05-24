/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.maillabel.domain.usecase

import android.graphics.Color
import app.cash.turbine.test
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.maillabel.domain.getLabel
import ch.protonmail.android.maillabel.domain.getMailFolder
import ch.protonmail.android.maillabel.domain.getMailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.repository.LabelRepository
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class ObserveMailLabelsTest {

    private val userId = UserId("1")

    private val labelRepository = mockk<LabelRepository> {
        every { observeLabels(any(), type = LabelType.MessageFolder) } returns flowOf(
            DataResult.Success(
                source = ResponseSource.Local,
                value = listOf(
                    getLabel(userId, LabelType.MessageFolder, "0", order = 0),
                    getLabel(userId, LabelType.MessageFolder, "1", order = 1),
                    getLabel(userId, LabelType.MessageFolder, "2", order = 2),
                )
            )
        )
        every { observeLabels(any(), type = LabelType.MessageLabel) } returns flowOf(
            DataResult.Success(
                source = ResponseSource.Local,
                value = listOf(
                    getLabel(userId, LabelType.MessageLabel, "3", order = 0),
                    getLabel(userId, LabelType.MessageLabel, "4", order = 1),
                    getLabel(userId, LabelType.MessageLabel, "5", order = 2),
                )
            )
        )
    }

    private val selectedMailLabelId = mockk<SelectedMailLabelId> {
        every { flow } returns MutableStateFlow(MailLabelId.System.Inbox)
    }

    private val TestScope.observeMailLabels
        get() = ObserveMailLabels(
            dispatcher = UnconfinedTestDispatcher(testScheduler),
            labelRepository = labelRepository,
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
        // Given
        every { selectedMailLabelId.flow } returns MutableStateFlow(MailLabelId.System.Inbox)

        // When
        observeMailLabels.invoke(userId).test {
            // Then
            val item = awaitItem()
            assertEquals(3, item.folders.size)
            assertEquals(3, item.labels.size)
            assertEquals(
                expected = listOf(
                    getMailFolder("0", order = 0),
                    getMailFolder("1", order = 1),
                    getMailFolder("2", order = 2),
                ),
                actual = item.folders
            )
            assertEquals(
                expected = listOf(
                    getMailLabel("3", order = 0),
                    getMailLabel("4", order = 1),
                    getMailLabel("5", order = 2),
                ),
                actual = item.labels
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `return correct folders parent on success`() = runTest {
        // Given
        every { selectedMailLabelId.flow } returns MutableStateFlow(MailLabelId.System.Inbox)

        every { labelRepository.observeLabels(any(), type = LabelType.MessageFolder) } returns flowOf(
            DataResult.Success(
                source = ResponseSource.Local,
                value = listOf(
                    getLabel(userId, LabelType.MessageFolder, "0", order = 0),
                    getLabel(userId, LabelType.MessageFolder, "0.1", order = 0, parentId = "0"),
                    getLabel(userId, LabelType.MessageFolder, "0.2", order = 1, parentId = "0"),
                    getLabel(userId, LabelType.MessageFolder, "0.2.1", order = 0, parentId = "0.2"),
                    getLabel(userId, LabelType.MessageFolder, "0.2.2", order = 1, parentId = "0.2"),
                )
            )
        )

        // When
        observeMailLabels.invoke(userId).test {
            // Then
            val item = awaitItem()

            val folder0 = getMailFolder(id = "0", level = 0, order = 0, parent = null, children = listOf("0.1", "0.2"))
            val folder01 = getMailFolder(id = "0.1", level = 1, order = 0, parent = folder0)
            val folder02 = getMailFolder(id = "0.2", level = 1, order = 1, parent = folder0, children = listOf("0.2.1", "0.2.2"))
            val folder021 = getMailFolder(id = "0.2.1", level = 2, order = 0, parent = folder02)
            val folder022 = getMailFolder(id = "0.2.2", level = 2, order = 1, parent = folder02)
            assertEquals(
                expected = listOf(folder0, folder01, folder02, folder021, folder022),
                actual = item.folders
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `return correct folders order on success`() = runTest {
        // Given
        every { selectedMailLabelId.flow } returns MutableStateFlow(MailLabelId.System.AllMail)

        every { labelRepository.observeLabels(any(), type = LabelType.MessageFolder) } returns flowOf(
            DataResult.Success(
                source = ResponseSource.Local,
                value = listOf(
                    getLabel(userId, LabelType.MessageFolder, "0.1", order = 1, parentId = "0"),
                    getLabel(userId, LabelType.MessageFolder, "0.0", order = 0, parentId = "0"),
                    getLabel(userId, LabelType.MessageFolder, "2", order = 2),
                    getLabel(userId, LabelType.MessageFolder, "0", order = 0),
                    getLabel(userId, LabelType.MessageFolder, "1", order = 1),
                )
            )
        )

        // When
        observeMailLabels.invoke(userId).test {
            // Then
            val item = awaitItem()

            val folder0 = getMailFolder(id = "0", level = 0, order = 0, parent = null, children = listOf("0.0", "0.1"))
            val folder00 = getMailFolder(id = "0.0", level = 1, order = 0, parent = folder0)
            val folder01 = getMailFolder(id = "0.1", level = 1, order = 1, parent = folder0)
            val folder1 = getMailFolder(id = "1", level = 0, order = 1, parent = null)
            val folder2 = getMailFolder(id = "2", level = 0, order = 2, parent = null)

            assertEquals(
                expected = listOf(folder0, folder00, folder01, folder1, folder2),
                actual = item.folders
            )
            cancelAndIgnoreRemainingEvents()
        }
    }
}
