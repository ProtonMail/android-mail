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

import app.cash.turbine.test
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.LabelSample
import ch.protonmail.android.mailcontact.domain.model.ContactGroup
import ch.protonmail.android.mailcontact.domain.model.GetContactError
import ch.protonmail.android.testdata.contact.ContactEmailSample
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SearchContactGroupsTest {

    private val observeContactGroupLabelsMock = mockk<ObserveContactGroupLabels>()

    private val observeContactGroupMock = mockk<ObserveContactGroup>()

    private val searchContactGroups = SearchContactGroups(
        observeContactGroupLabelsMock,
        observeContactGroupMock
    )

    private val expectedLabels = listOf(
        LabelSample.GroupCoworkers,
        LabelSample.GroupFriends
    )

    private val expectedContactGroupCoworkers = ContactGroup(
        UserIdTestData.userId,
        LabelSample.GroupCoworkers.labelId,
        "Coworkers",
        "#ABCABC",
        listOf(
            ContactEmailSample.contactEmail1,
            ContactEmailSample.contactEmail2
        )
    )

    private val expectedContactGroupFriends = ContactGroup(
        UserIdTestData.userId,
        LabelSample.GroupFriends.labelId,
        "Friends",
        "#ABCABC",
        listOf(
            ContactEmailSample.contactEmail3,
            ContactEmailSample.contactEmail4
        )
    )

    @Test
    fun `when there are matching contact groups, return them`() = runTest {
        // Given
        val query = "LABEL"

        expectContactGroupLabelsSuccess(UserIdTestData.userId, expectedLabels)
        expectObserveContactGroup(
            UserIdTestData.userId,
            expectedContactGroupCoworkers.labelId,
            expectedContactGroupCoworkers
        )
        expectObserveContactGroup(
            UserIdTestData.userId,
            expectedContactGroupFriends.labelId,
            expectedContactGroupFriends
        )

        // When
        searchContactGroups(UserIdTestData.userId, query).test {
            // Then
            val actual = assertIs<Either.Right<List<ContactGroup>>>(awaitItem())
            assertEquals(actual.value.size, 2)
            awaitComplete()
        }
    }

    @Test
    fun `when there are matching contact groups but they are empty, don't return them if param is false`() = runTest {
        // Given
        val query = "work"

        expectContactGroupLabelsSuccess(UserIdTestData.userId, expectedLabels)

        val expectedEmptyGroupCoworkers = ContactGroup(
            UserIdTestData.userId,
            LabelSample.GroupCoworkers.labelId,
            "Coworkers",
            "#ABCABC",
            emptyList() // this group matches by name, but it's empty
        )

        expectObserveContactGroup(
            UserIdTestData.userId,
            expectedContactGroupCoworkers.labelId,
            expectedEmptyGroupCoworkers
        )

        // When
        searchContactGroups(UserIdTestData.userId, query).test {
            // Then
            val actual = assertIs<Either.Right<List<ContactGroup>>>(awaitItem())

            assertEquals(actual.value.size, 0)
            awaitComplete()
        }
    }

    @Test
    fun `when there are matching contact groups but they are empty, return them if param is true`() = runTest {
        // Given
        val query = "work"

        expectContactGroupLabelsSuccess(UserIdTestData.userId, expectedLabels)

        val expectedEmptyGroupCoworkers = ContactGroup(
            UserIdTestData.userId,
            LabelSample.GroupCoworkers.labelId,
            "Coworkers",
            "#ABCABC",
            emptyList() // this group matches by name, but it's empty
        )

        expectObserveContactGroup(
            UserIdTestData.userId,
            expectedContactGroupCoworkers.labelId,
            expectedEmptyGroupCoworkers
        )

        // When
        searchContactGroups(UserIdTestData.userId, query, returnEmpty = true).test {
            // Then
            val actual = assertIs<Either.Right<List<ContactGroup>>>(awaitItem())

            assertEquals(actual.value.size, 1)
            assertEquals(actual.value.first(), expectedEmptyGroupCoworkers)
            awaitComplete()
        }
    }

    @Test
    fun `when there are no matching contact groups, empty list is emitted`() = runTest {
        // Given
        val query = "there is no contact group like this"

        expectContactGroupLabelsSuccess(UserIdTestData.userId, expectedLabels)

        // When
        searchContactGroups(UserIdTestData.userId, query).test {
            // Then
            val actual = assertIs<Either.Right<List<ContactGroup>>>(awaitItem())
            assertEquals(emptyList(), actual.value)
            awaitComplete()
        }
    }


    @Test
    fun `when observe contact group labels returns error, error is emitted`() = runTest {
        // Given
        expectContactGroupLabelsFailure(UserIdTestData.userId)

        // When
        searchContactGroups(UserIdTestData.userId, "doesn't matter").test {
            // Then
            assertIs<Either.Left<GetContactError>>(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `when any of the observe contact groups returns error, error is emitted`() = runTest {
        // Given
        val query = "LABEL"

        expectContactGroupLabelsSuccess(UserIdTestData.userId, expectedLabels)
        expectObserveContactGroup(
            UserIdTestData.userId,
            expectedContactGroupCoworkers.labelId,
            expectedContactGroupCoworkers
        )
        expectObserveContactGroupFails(
            UserIdTestData.userId,
            expectedContactGroupFriends.labelId
        )

        // When
        searchContactGroups(UserIdTestData.userId, query).test {
            // Then
            assertIs<Either.Left<GetContactError>>(awaitItem())
            awaitComplete()
        }
    }

    private fun expectContactGroupLabelsSuccess(userId: UserId, result: List<Label>) {
        every {
            observeContactGroupLabelsMock(userId)
        } returns flowOf(result.right())
    }

    private fun expectContactGroupLabelsFailure(userId: UserId) {
        every {
            observeContactGroupLabelsMock(userId)
        } returns flowOf(GetContactGroupLabelsError.left())
    }

    private fun expectObserveContactGroup(
        userId: UserId,
        labelId: LabelId,
        contactGroup: ContactGroup
    ) {
        every {
            observeContactGroupMock(userId, labelId)
        } returns flowOf(contactGroup.right())
    }

    private fun expectObserveContactGroupFails(userId: UserId, labelId: LabelId) {
        every {
            observeContactGroupMock(userId, labelId)
        } returns flowOf(GetContactGroupError.GetContactsError.left())
    }
}
