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
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.ContactEmailId
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.entity.NewLabel
import me.proton.core.label.domain.repository.LabelLocalDataSource
import me.proton.core.label.domain.repository.LabelRemoteDataSource
import org.junit.Test
import kotlin.test.assertEquals

class CreateContactGroupTest {

    private val userId = UserId("userId")
    private val contactGroupName = "Contact Group Name"
    private val contactGroupColor = "#AABBCC"
    private val contactGroupLabelId = LabelId("Contact Group Label ID")

    private val expectedNewLabel = NewLabel(
        name = contactGroupName,
        color = contactGroupColor,
        isNotified = null,
        isExpanded = null,
        isSticky = null,
        parentId = null,
        type = LabelType.ContactGroup
    )

    private val expectedCreatedLabel = Label(
        userId = userId,
        labelId = contactGroupLabelId,
        parentId = null,
        name = contactGroupName,
        type = LabelType.ContactGroup,
        path = contactGroupName,
        color = contactGroupColor,
        order = 32,
        isNotified = false,
        isExpanded = false,
        isSticky = false
    )

    private val expectedContactEmailIds = listOf(
        ContactEmailId("contact email ID 1"),
        ContactEmailId("contact email ID 2"),
        ContactEmailId("contact email ID 3")
    )

    private val labelRemoteDataSourceMock = mockk<LabelRemoteDataSource>()
    private val labelLocalDataSourceMock = mockk<LabelLocalDataSource>()
    private val editContactGroupMembersMock = mockk<EditContactGroupMembers>()

    val createContactGroup = CreateContactGroup(
        labelRemoteDataSourceMock,
        labelLocalDataSourceMock,
        editContactGroupMembersMock
    )

    @Test
    fun `should call data sources and use case with correct arguments`() = runTest {
        // Given
        expectCreateLabelSuccess(expectedNewLabel, expectedCreatedLabel)
        expectUpsertLabelSuccess(expectedCreatedLabel)
        expectEditContactGroupMembersSuccess(expectedContactEmailIds)

        // When
        val actual = createContactGroup(
            userId,
            contactGroupName,
            ColorRgbHex(contactGroupColor),
            expectedContactEmailIds
        )

        // Then
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `should return error when creating ContactGroup throws exception`() = runTest {
        // Given
        expectCreateLabelThrows(expectedNewLabel)

        // When
        val actual = createContactGroup(
            userId,
            contactGroupName,
            ColorRgbHex(contactGroupColor),
            expectedContactEmailIds
        )

        // Then
        assertEquals(CreateContactGroupError.CreatingLabelError.left(), actual)
    }

    @Test
    fun `should return error when editing ContactGroup Members fails`() = runTest {
        // Given
        expectCreateLabelSuccess(expectedNewLabel, expectedCreatedLabel)
        expectUpsertLabelSuccess(expectedCreatedLabel)
        expectEditContactGroupMembersFail(expectedContactEmailIds)

        // When
        val actual = createContactGroup(
            userId,
            contactGroupName,
            ColorRgbHex(contactGroupColor),
            expectedContactEmailIds
        )

        // Then
        assertEquals(CreateContactGroupError.EditingMembersError.left(), actual)
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

    private fun expectUpsertLabelSuccess(expectedCreatedLabel: Label) {
        coEvery { labelLocalDataSourceMock.upsertLabel(listOf(expectedCreatedLabel)) } just Runs
    }

    private fun expectCreateLabelSuccess(expectedNewLabel: NewLabel, expectedCreatedLabel: Label) {
        coEvery { labelRemoteDataSourceMock.createLabel(userId, expectedNewLabel) } returns expectedCreatedLabel
    }

    private fun expectCreateLabelThrows(expectedNewLabel: NewLabel) {
        coEvery { labelRemoteDataSourceMock.createLabel(userId, expectedNewLabel) } throws Exception("")
    }

}
