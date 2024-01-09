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

package ch.protonmail.android.mailcontact.presentation.contactlist

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.ContactListItemUiModel
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ContactListReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val reducer = ContactListReducer()

    @Test
    fun `should produce the expected state`() = with(testInput) {
        val actualState = reducer.newStateFrom(currentState, event)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val loadedContactListItemUiModels = listOf(
            ContactListItemUiModel.Header("F"),
            ContactListItemUiModel.Contact(
                id = "1",
                name = "first contact",
                emailSubtext = TextUiModel("firstcontact+alias@protonmail.com"),
                avatar = AvatarUiModel.ParticipantInitial("FC")
            ),
            ContactListItemUiModel.Contact(
                id = "1.1",
                name = "first contact bis",
                emailSubtext = TextUiModel("firstcontactbis@protonmail.com"),
                avatar = AvatarUiModel.ParticipantInitial("FB")
            )
        )

        private val emptyLoadingState = ContactListState.Loading()
        private val errorLoadingState = ContactListState.Loading(
            errorLoading = Effect.of(TextUiModel(R.string.contact_list_loading_error))
        )
        private val emptyListLoadedState = ContactListState.ListLoaded.Empty()
        private val dataListLoadedState = ContactListState.ListLoaded.Data(
            contacts = loadedContactListItemUiModels
        )

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = emptyLoadingState,
                event = ContactListEvent.ContactListLoaded(loadedContactListItemUiModels),
                expectedState = dataListLoadedState
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactListEvent.ContactListLoaded(emptyList()),
                expectedState = ContactListState.ListLoaded.Empty()
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactListEvent.ErrorLoadingContactList,
                expectedState = errorLoadingState
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactListEvent.OpenContactForm,
                expectedState = emptyLoadingState
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactListEvent.OpenContactGroupForm,
                expectedState = emptyLoadingState
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactListEvent.OpenImportContact,
                expectedState = emptyLoadingState
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactListEvent.OpenBottomSheet,
                expectedState = emptyLoadingState
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactListEvent.DismissBottomSheet,
                expectedState = emptyLoadingState
            )
        )

        private val transitionsFromEmptyListLoadedState = listOf(
            TestInput(
                currentState = emptyListLoadedState,
                event = ContactListEvent.ContactListLoaded(loadedContactListItemUiModels),
                expectedState = dataListLoadedState
            ),
            TestInput(
                currentState = emptyListLoadedState.copy(
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                ),
                event = ContactListEvent.ContactListLoaded(emptyList()),
                expectedState = ContactListState.ListLoaded.Empty().copy(
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            ),
            TestInput(
                currentState = emptyListLoadedState,
                event = ContactListEvent.ErrorLoadingContactList,
                expectedState = errorLoadingState
            ),
            TestInput(
                currentState = emptyListLoadedState,
                event = ContactListEvent.OpenContactForm,
                expectedState = emptyListLoadedState.copy(
                    openContactForm = Effect.of(Unit),
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            ),
            TestInput(
                currentState = emptyListLoadedState,
                event = ContactListEvent.OpenContactGroupForm,
                expectedState = emptyListLoadedState.copy(
                    openContactGroupForm = Effect.of(Unit),
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            ),
            TestInput(
                currentState = emptyListLoadedState,
                event = ContactListEvent.OpenImportContact,
                expectedState = emptyListLoadedState.copy(
                    openImportContact = Effect.of(Unit),
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            ),
            TestInput(
                currentState = emptyListLoadedState,
                event = ContactListEvent.OpenBottomSheet,
                expectedState = emptyListLoadedState.copy(
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Show)
                )
            ),
            TestInput(
                currentState = emptyListLoadedState,
                event = ContactListEvent.DismissBottomSheet,
                expectedState = emptyListLoadedState.copy(
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            )
        )

        private val transitionsFromDataListLoadedState = listOf(
            TestInput(
                currentState = dataListLoadedState,
                event = ContactListEvent.ContactListLoaded(loadedContactListItemUiModels),
                expectedState = dataListLoadedState
            ),
            TestInput(
                currentState = dataListLoadedState,
                event = ContactListEvent.ContactListLoaded(emptyList()),
                expectedState = ContactListState.ListLoaded.Empty()
            ),
            TestInput(
                currentState = dataListLoadedState,
                event = ContactListEvent.ErrorLoadingContactList,
                expectedState = errorLoadingState
            ),
            TestInput(
                currentState = dataListLoadedState,
                event = ContactListEvent.OpenContactForm,
                expectedState = dataListLoadedState.copy(
                    openContactForm = Effect.of(Unit),
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            ),
            TestInput(
                currentState = dataListLoadedState,
                event = ContactListEvent.OpenContactGroupForm,
                expectedState = dataListLoadedState.copy(
                    openContactGroupForm = Effect.of(Unit),
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            ),
            TestInput(
                currentState = dataListLoadedState,
                event = ContactListEvent.OpenImportContact,
                expectedState = dataListLoadedState.copy(
                    openImportContact = Effect.of(Unit),
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            ),
            TestInput(
                currentState = dataListLoadedState,
                event = ContactListEvent.OpenBottomSheet,
                expectedState = dataListLoadedState.copy(
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Show)
                )
            ),
            TestInput(
                currentState = dataListLoadedState,
                event = ContactListEvent.DismissBottomSheet,
                expectedState = dataListLoadedState.copy(
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
                )
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = (
            transitionsFromLoadingState +
                transitionsFromEmptyListLoadedState +
                transitionsFromDataListLoadedState
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
        val currentState: ContactListState,
        val event: ContactListEvent,
        val expectedState: ContactListState
    )

}
