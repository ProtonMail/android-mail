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

package ch.protonmail.android.mailcontact.presentation.contactdetails

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.Avatar
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactDetailsPreviewData
import me.proton.core.contact.domain.entity.ContactId
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ContactDetailsReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val reducer = ContactDetailsReducer()

    @Test
    fun `should produce the expected state`() = with(testInput) {
        val actualState = reducer.newStateFrom(currentState, event)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val loadedContactDetailsUiModel = ContactDetailsPreviewData.contactDetailsSampleData
        private val loadedContactDetailsUiModel2 = ContactDetailsPreviewData.contactDetailsSampleData.copy(
            id = ContactId("Id 2"),
            nameHeader = "John Doe 2",
            avatar = Avatar.Initials("JD 2")
        )

        private val emptyLoadingState = ContactDetailsState.Loading()
        private val loadedContactState = ContactDetailsState.Data(contact = loadedContactDetailsUiModel)

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = emptyLoadingState,
                event = ContactDetailsEvent.ContactLoaded(loadedContactDetailsUiModel),
                expectedState = loadedContactState
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactDetailsEvent.LoadContactError,
                expectedState = emptyLoadingState.copy(
                    errorLoading = Effect.of(TextUiModel(R.string.contact_details_loading_error))
                )
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactDetailsEvent.CloseContactDetails,
                expectedState = emptyLoadingState.copy(
                    close = Effect.of(Unit)
                )
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactDetailsEvent.DeleteRequested,
                expectedState = emptyLoadingState
            )
        )

        private val transitionsFromDataState = listOf(
            TestInput(
                currentState = loadedContactState,
                event = ContactDetailsEvent.ContactLoaded(loadedContactDetailsUiModel2),
                expectedState = loadedContactState.copy(
                    contact = loadedContactDetailsUiModel2
                )
            ),
            TestInput(
                currentState = loadedContactState,
                event = ContactDetailsEvent.LoadContactError,
                expectedState = loadedContactState
            ),
            TestInput(
                currentState = loadedContactState,
                event = ContactDetailsEvent.CloseContactDetails,
                expectedState = loadedContactState.copy(
                    close = Effect.of(Unit)
                )
            ),
            TestInput(
                currentState = loadedContactState,
                event = ContactDetailsEvent.DeleteRequested,
                expectedState = loadedContactState.copy(
                    showDeleteConfirmDialog = Effect.of(Unit)
                )
            ),
            TestInput(
                currentState = loadedContactState,
                event = ContactDetailsEvent.DeleteConfirmed,
                expectedState = loadedContactState.copy(
                    closeWithSuccess = Effect.of(TextUiModel(R.string.contact_details_delete_success))
                )
            ),
            TestInput(
                currentState = loadedContactState,
                event = ContactDetailsEvent.CallPhoneNumber("123456789"),
                expectedState = loadedContactState.copy(
                    callPhoneNumber = Effect.of("123456789")
                )
            ),
            TestInput(
                currentState = loadedContactState,
                event = ContactDetailsEvent.CopyToClipboard(loadedContactState.contact.nameHeader),
                expectedState = loadedContactState.copy(
                    copyToClipboard = Effect.of(loadedContactState.contact.nameHeader)
                )
            ),
            TestInput(
                currentState = loadedContactState,
                event = ContactDetailsEvent.ComposeEmail("test@proton.me"),
                expectedState = loadedContactState.copy(
                    openComposer = Effect.of("test@proton.me")
                )
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = (transitionsFromLoadingState + transitionsFromDataState)
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
        val currentState: ContactDetailsState,
        val event: ContactDetailsEvent,
        val expectedState: ContactDetailsState
    )

}
