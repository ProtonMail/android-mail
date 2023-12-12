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
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId.System
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.toMailLabelCustom
import ch.protonmail.android.maillabel.domain.model.toMailLabelSystem
import ch.protonmail.android.testdata.label.LabelTestData.buildLabel
import ch.protonmail.android.testdata.maillabel.MailLabelTestData.buildCustomFolder
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelType
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class MailLabelBuilderTest {

    private val userId = UserIdTestData.userId

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
    fun `return correct system labels`() = runTest {
        // Given
        val items = listOf(
            SystemLabelId.Inbox,
            SystemLabelId.Drafts,
            SystemLabelId.Sent,
            SystemLabelId.Starred,
            SystemLabelId.Archive,
            SystemLabelId.Spam,
            SystemLabelId.Trash,
            SystemLabelId.AllMail
        )

        // When
        val actual = items.toMailLabelSystem()

        // Then
        val expected = listOf(
            MailLabel.System(System.Inbox),
            MailLabel.System(System.Drafts),
            MailLabel.System(System.Sent),
            MailLabel.System(System.Starred),
            MailLabel.System(System.Archive),
            MailLabel.System(System.Spam),
            MailLabel.System(System.Trash),
            MailLabel.System(System.AllMail)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `return correct simple folders`() = runTest {
        // Given
        val items = listOf(
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "0", order = 0),
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "1", order = 1),
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "2", order = 2)
        )

        // When
        val actual = items.toMailLabelCustom()

        // Then
        val expected = listOf(
            buildCustomFolder("0", order = 0),
            buildCustomFolder("1", order = 1),
            buildCustomFolder("2", order = 2)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `return correct folders with parent`() = runTest {
        // Given
        val items = listOf(
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "0", order = 0),
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "0.1", order = 0, parentId = "0"),
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "0.2", order = 1, parentId = "0"),
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "0.2.1", order = 0, parentId = "0.2"),
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "0.2.2", order = 1, parentId = "0.2")
        )

        // When
        val actual = items.toMailLabelCustom()

        // Then
        val f0 = buildCustomFolder("0", level = 0, order = 0, parent = null, children = listOf("0.1", "0.2"))
        val f01 = buildCustomFolder("0.1", level = 1, order = 0, parent = f0)
        val f02 = buildCustomFolder("0.2", level = 1, order = 1, parent = f0, children = listOf("0.2.1", "0.2.2"))
        val f021 = buildCustomFolder("0.2.1", level = 2, order = 0, parent = f02)
        val f022 = buildCustomFolder("0.2.2", level = 2, order = 1, parent = f02)
        val expected = listOf(f0, f01, f02, f021, f022)

        assertEquals(expected, actual)
    }

    @Test
    fun `when a parent does not exist, ignore its children`() = runTest {
        // Given
        val items = listOf(
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "0", order = 0),
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "0.1", order = 0, parentId = "0"),
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "0.2.1", order = 0, parentId = "0.2"),
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "0.2.2", order = 1, parentId = "0.2")
        )

        // When
        val actual = items.toMailLabelCustom()

        // Then
        val f0 = buildCustomFolder("0", level = 0, order = 0, parent = null, children = listOf("0.1"))
        val f01 = buildCustomFolder("0.1", level = 1, order = 0, parent = f0)
        val expected = listOf(f0, f01)

        assertEquals(expected, actual)
    }

    @Test
    fun `return correct ordered folders`() = runTest {
        // Given
        val items = listOf(
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "0.2.2", order = 1, parentId = "0.2"),
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "0.2", order = 1, parentId = "0"),
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "0.1", order = 0, parentId = "0"),
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "0", order = 0),
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = "0.2.1", order = 0, parentId = "0.2")
        )

        // When
        val actual = items.toMailLabelCustom()

        // Then
        val f0 = buildCustomFolder("0", level = 0, order = 0, parent = null, children = listOf("0.1", "0.2"))
        val f01 = buildCustomFolder("0.1", level = 1, order = 0, parent = f0)
        val f02 = buildCustomFolder("0.2", level = 1, order = 1, parent = f0, children = listOf("0.2.1", "0.2.2"))
        val f021 = buildCustomFolder("0.2.1", level = 2, order = 0, parent = f02)
        val f022 = buildCustomFolder("0.2.2", level = 2, order = 1, parent = f02)
        val expected = listOf(f0, f01, f02, f021, f022)

        assertEquals(expected, actual)
    }
}
