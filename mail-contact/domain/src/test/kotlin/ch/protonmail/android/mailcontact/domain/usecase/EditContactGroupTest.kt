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
import arrow.core.right
import ch.protonmail.android.maillabel.domain.model.ColorRgbHex
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.ContactEmailId
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

    private val expectedContactEmailIds = listOf(
        ContactEmailId("contact email ID 1"),
        ContactEmailId("contact email ID 2"),
        ContactEmailId("contact email ID 3")
    )

    private val existingLabel = Label(
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

    private val expectedLabel = existingLabel.copy(
        name = contactGroupName,
        color = contactGroupColor
    )

    private val labelRepositoryMock = mockk<LabelRepository>()
    private val editContactGroupMembersMock = mockk<EditContactGroupMembers>()
    val editContactGroup = EditContactGroup(labelRepositoryMock, editContactGroupMembersMock)

    @Test
    fun `should call LabelRepository and EditContactGroupMembers with correct arguments`() = runTest {
        // Given
        coEvery { labelRepositoryMock.updateLabel(userId, expectedLabel) } just Runs
        expectGetLabel(expectedLabel.labelId, expectedLabel)
        expectEditContactGroupMembersSuccess(expectedContactEmailIds)

        // When
        editContactGroup(
            userId,
            contactGroupLabelId,
            contactGroupName,
            ColorRgbHex(contactGroupColor),
            expectedContactEmailIds
        )

        // Then
        coVerify {
            labelRepositoryMock.updateLabel(userId, expectedLabel)
            editContactGroupMembersMock(userId, contactGroupLabelId, expectedContactEmailIds.toSet())
        }
    }

    @Test
    fun `should trim the whitespaces from contact group name`() = runTest {
        // Given
        val contactGroupNameWithWhitespaces = " $contactGroupName "
        coEvery { labelRepositoryMock.updateLabel(userId, expectedLabel) } just Runs
        expectGetLabel(expectedLabel.labelId, expectedLabel)
        expectEditContactGroupMembersSuccess(expectedContactEmailIds)

        // When
        editContactGroup(
            userId,
            contactGroupLabelId,
            contactGroupNameWithWhitespaces,
            ColorRgbHex(contactGroupColor),
            expectedContactEmailIds
        )

        // Then
        coVerify {
            labelRepositoryMock.updateLabel(userId, expectedLabel)
            editContactGroupMembersMock(userId, contactGroupLabelId, expectedContactEmailIds.toSet())
        }
    }

    @Test
    fun `should return error when getting label from repository fails`() = runTest {
        // Given
        expectGetLabel(contactGroupLabelId, null)

        // When
        val actual = editContactGroup(
            userId,
            contactGroupLabelId,
            contactGroupName,
            ColorRgbHex(contactGroupColor),
            expectedContactEmailIds
        )

        // Then
        assertEquals(EditContactGroupError.LabelNotFound.left(), actual)
    }

    @Test
    fun `should return error when when editing ContactGroup Members fails`() = runTest {
        // Given
        coEvery { labelRepositoryMock.updateLabel(userId, expectedLabel) } just Runs
        expectGetLabel(expectedLabel.labelId, expectedLabel)
        expectEditContactGroupMembersFail(expectedContactEmailIds)

        // When
        val actual = editContactGroup(
            userId,
            contactGroupLabelId,
            contactGroupName,
            ColorRgbHex(contactGroupColor),
            expectedContactEmailIds
        )

        // Then
        assertEquals(EditContactGroupError.EditingMembersError.left(), actual)
    }

    private fun expectGetLabel(labelId: LabelId, expectedLabel: Label?) {
        coEvery { labelRepositoryMock.getLabel(userId, LabelType.ContactGroup, labelId) } returns expectedLabel
    }

    private fun expectEditContactGroupMembersSuccess(expectedContactEmailIds: List<ContactEmailId>) {
        coEvery {
            editContactGroupMembersMock(
                userId,
                contactGroupLabelId,
                expectedContactEmailIds.toSet()
            )
        } returns Unit.right()
    }

    private fun expectEditContactGroupMembersFail(expectedContactEmailIds: List<ContactEmailId>) {
        coEvery {
            editContactGroupMembersMock(
                userId,
                contactGroupLabelId,
                expectedContactEmailIds.toSet()
            )
        } returns EditContactGroupMembers.EditContactGroupMembersError.ObservingContactGroup.left()
    }

}
