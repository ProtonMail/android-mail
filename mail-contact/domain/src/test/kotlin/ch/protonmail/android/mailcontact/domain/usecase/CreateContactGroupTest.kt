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
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.entity.NewLabel
import me.proton.core.label.domain.repository.LabelRepository
import org.junit.Test
import kotlin.test.assertEquals

class CreateContactGroupTest {

    private val userId = UserId("userId")
    private val contactGroupName = "Contact Group Name"
    private val contactGroupColor = "#AABBCC"

    private val labelRepositoryMock = mockk<LabelRepository>()
    val createContactGroup = CreateContactGroup(labelRepositoryMock)

    @Test
    fun `should call LabelRepository with correct argument`() = runTest {
        // Given
        val expectedNewLabel = NewLabel(
            name = contactGroupName,
            color = contactGroupColor,
            isNotified = null,
            isExpanded = null,
            isSticky = null,
            parentId = null,
            type = LabelType.ContactGroup
        )
        coEvery { labelRepositoryMock.createLabel(userId, expectedNewLabel) } just Runs

        // When
        createContactGroup(userId, contactGroupName, contactGroupColor)

        // Then
        coVerify {
            labelRepositoryMock.createLabel(userId, expectedNewLabel)
        }
    }

    @Test
    fun `should return error when LabelRepository throws an exception`() = runTest {
        // Given
        expectLabelRepositoryFailure()

        // When
        val actual = createContactGroup(userId, contactGroupName, contactGroupColor)

        // Then
        assertEquals(CreateContactGroup.CreateContactGroupErrors.FailedToCreateContactGroup.left(), actual)
    }

    private fun expectLabelRepositoryFailure() {
        coEvery { labelRepositoryMock.createLabel(userId, any()) } throws Exception("")
    }

}
