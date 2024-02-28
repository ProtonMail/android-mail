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

package ch.protonmail.android.mailcontact.domain.usecase

import arrow.core.left
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.repository.LabelRepository
import org.junit.Test
import kotlin.test.assertEquals

class EditContactGroupTest {

    private val userId = UserId("userId")
    private val contactGroupName = "Contact Group Name"
    private val contactGroupColor = "#AABBCC"
    private val contactGroupLabelId = LabelId("Contact Group Label Id")

    private val labelRepositoryMock = mockk<LabelRepository>()
    val editContactGroup = EditContactGroup(labelRepositoryMock)

    @Test
    fun `should call LabelRepository with correct argument`() = runTest {
        // Given
        val existingLabel = Label(
            userId = userId,
            labelId = contactGroupLabelId,
            name = contactGroupName,
            color = contactGroupColor,
            isNotified = null,
            isExpanded = null,
            isSticky = null,
            parentId = null,
            type = LabelType.ContactGroup,
            path = "",
            order = 666
        )
        val expectedLabel = existingLabel.copy(
            name = contactGroupName,
            color = contactGroupColor
        )
        coEvery { labelRepositoryMock.updateLabel(userId, expectedLabel) } just Runs
        expectGetLabel(expectedLabel.labelId, expectedLabel)

        // When
        editContactGroup(userId, contactGroupLabelId, contactGroupName, contactGroupColor)

        // Then
        coVerify {
            labelRepositoryMock.updateLabel(userId, expectedLabel)
        }
    }

    @Test
    fun `should return error when getting label from repository fails`() = runTest {
        // Given
        expectGetLabel(contactGroupLabelId, null)

        // When
        val actual = editContactGroup(userId, contactGroupLabelId, contactGroupName, contactGroupColor)

        // Then
        assertEquals(EditContactGroupError.LabelNotFound.left(), actual)
    }

    private fun expectGetLabel(labelId: LabelId, expectedLabel: Label?) {
        coEvery { labelRepositoryMock.getLabel(userId, LabelType.ContactGroup, labelId) } returns expectedLabel
    }

}
