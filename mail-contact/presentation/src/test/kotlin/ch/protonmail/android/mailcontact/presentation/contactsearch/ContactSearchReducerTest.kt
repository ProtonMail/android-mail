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

package ch.protonmail.android.mailcontact.presentation.contactsearch

import androidx.compose.ui.graphics.Color
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupItemUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactListItemUiModel
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class ContactSearchReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val reducer = ContactSearchReducer()

    @Test
    fun `should produce the expected state`() = with(testInput) {
        val actualState = reducer.newStateFrom(currentState, event)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val searchResultsContacts = listOf(
            ContactListItemUiModel.Contact(
                id = ContactId("result 1 ID"),
                name = "result 1 name",
                emailSubtext = TextUiModel.Text("result1@proton.me"),
                avatar = AvatarUiModel.ParticipantInitial("R1")
            ),
            ContactListItemUiModel.Contact(
                id = ContactId("result 2 ID"),
                name = "result 2 name",
                emailSubtext = TextUiModel.Text("result2@proton.me"),
                avatar = AvatarUiModel.ParticipantInitial("R2")
            )
        )

        private val searchResultsContactGroups = listOf(
            ContactGroupItemUiModel(
                labelId = LabelId("result 3 ID"),
                name = "result 3 group name",
                memberCount = 10,
                color = Color(1f, 1f, 1f)
            )
        )

        private val emptyState = ContactSearchState()
        private val noResultsState = ContactSearchState(
            contactUiModels = emptyList(),
            groupUiModels = emptyList()
        )
        private val someResultsContactsState = ContactSearchState(
            contactUiModels = searchResultsContacts,
            groupUiModels = emptyList()
        )
        private val someResultsContactGroupsState = ContactSearchState(
            contactUiModels = emptyList(),
            groupUiModels = searchResultsContactGroups
        )

        private val transitionsFromEmptyState = listOf(
            TestInput(
                currentState = emptyState,
                event = ContactSearchEvent.ContactsLoaded(
                    contacts = emptyList(),
                    groups = emptyList()
                ),
                expectedState = noResultsState
            ),
            TestInput(
                currentState = emptyState,
                event = ContactSearchEvent.ContactsLoaded(
                    contacts = searchResultsContacts,
                    groups = emptyList()
                ),
                expectedState = someResultsContactsState
            ),
            TestInput(
                currentState = emptyState,
                event = ContactSearchEvent.ContactsLoaded(
                    contacts = emptyList(),
                    groups = searchResultsContactGroups
                ),
                expectedState = someResultsContactGroupsState
            ),
            TestInput(
                currentState = emptyState,
                event = ContactSearchEvent.ContactsCleared,
                expectedState = emptyState
            )
        )

        private val transitionsFromNoResultsState = listOf(
            TestInput(
                currentState = noResultsState,
                event = ContactSearchEvent.ContactsLoaded(
                    contacts = emptyList(),
                    groups = emptyList()
                ),
                expectedState = noResultsState
            ),
            TestInput(
                currentState = noResultsState,
                event = ContactSearchEvent.ContactsLoaded(
                    contacts = searchResultsContacts,
                    groups = emptyList()
                ),
                expectedState = someResultsContactsState
            ),
            TestInput(
                currentState = noResultsState,
                event = ContactSearchEvent.ContactsLoaded(
                    contacts = emptyList(),
                    groups = searchResultsContactGroups
                ),
                expectedState = someResultsContactGroupsState
            ),
            TestInput(
                currentState = noResultsState,
                event = ContactSearchEvent.ContactsCleared,
                expectedState = emptyState
            )
        )

        private val transitionsFromSomeResultsState = listOf(
            TestInput(
                currentState = someResultsContactsState,
                event = ContactSearchEvent.ContactsLoaded(
                    contacts = emptyList(),
                    groups = emptyList()
                ),
                expectedState = noResultsState
            ),
            TestInput(
                currentState = someResultsContactsState,
                event = ContactSearchEvent.ContactsLoaded(
                    contacts = searchResultsContacts.take(1),
                    groups = emptyList()
                ),
                expectedState = someResultsContactsState.copy(
                    contactUiModels = searchResultsContacts.take(1)
                )
            ),
            TestInput(
                currentState = someResultsContactGroupsState,
                event = ContactSearchEvent.ContactsLoaded(
                    contacts = emptyList(),
                    groups = searchResultsContactGroups.take(1)
                ),
                expectedState = someResultsContactsState.copy(
                    contactUiModels = emptyList(),
                    groupUiModels = searchResultsContactGroups.take(1)
                )
            ),
            TestInput(
                currentState = someResultsContactsState,
                event = ContactSearchEvent.ContactsCleared,
                expectedState = emptyState
            )
        )


        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = (
            transitionsFromEmptyState +
                transitionsFromNoResultsState +
                transitionsFromSomeResultsState
            )
            .map { testInput ->
                val testName = """
                    Current state: ${testInput.currentState}
                    Event: ${testInput.event}
                    Next state: ${testInput.expectedState}
                    
                """.trimIndent()
                arrayOf(testName, testInput)
            }
    }

    data class TestInput(
        val currentState: ContactSearchState,
        val event: ContactSearchEvent,
        val expectedState: ContactSearchState
    )
}
