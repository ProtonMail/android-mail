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
import ch.protonmail.android.mailcontact.presentation.model.ContactSearchUiModel
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

        private val searchResults = listOf(
            ContactSearchUiModel.Contact(
                id = ContactId("result 1 ID"),
                name = "result 1 name",
                email = "result1@proton.me",
                initials = "R1"
            ),
            ContactSearchUiModel.Contact(
                id = ContactId("result 2 ID"),
                name = "result 2 name",
                email = "result2@proton.me",
                initials = "R2"
            ),
            ContactSearchUiModel.ContactGroup(
                id = LabelId("result 3 ID"),
                name = "result 3 group name",
                color = Color(1f, 1f, 1f),
                emailCount = 10
            )
        )

        private val emptyState = ContactSearchState()
        private val noResultsState = ContactSearchState(
            uiModels = emptyList()
        )
        private val someResultsState = ContactSearchState(
            uiModels = searchResults
        )

        private val transitionsFromEmptyState = listOf(
            TestInput(
                currentState = emptyState,
                event = ContactSearchEvent.ContactsLoaded(
                    contacts = emptyList()
                ),
                expectedState = noResultsState
            ),
            TestInput(
                currentState = emptyState,
                event = ContactSearchEvent.ContactsLoaded(
                    contacts = searchResults
                ),
                expectedState = someResultsState
            )
        )

        private val transitionsFromNoResultsState = listOf(
            TestInput(
                currentState = noResultsState,
                event = ContactSearchEvent.ContactsLoaded(
                    contacts = emptyList()
                ),
                expectedState = noResultsState
            ),
            TestInput(
                currentState = noResultsState,
                event = ContactSearchEvent.ContactsLoaded(
                    contacts = searchResults
                ),
                expectedState = someResultsState
            )
        )

        private val transitionsFromSomeResultsState = listOf(
            TestInput(
                currentState = someResultsState,
                event = ContactSearchEvent.ContactsLoaded(
                    contacts = emptyList()
                ),
                expectedState = noResultsState
            ),
            TestInput(
                currentState = someResultsState,
                event = ContactSearchEvent.ContactsLoaded(
                    contacts = searchResults.take(1)
                ),
                expectedState = someResultsState.copy(
                    uiModels = searchResults.take(1)
                )
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
