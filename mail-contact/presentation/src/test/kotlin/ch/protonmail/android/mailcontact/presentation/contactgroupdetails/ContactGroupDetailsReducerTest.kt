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

package ch.protonmail.android.mailcontact.presentation.contactgroupdetails

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactGroupDetailsPreviewData
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ContactGroupDetailsReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val reducer = ContactGroupDetailsReducer()

    @Test
    fun `should produce the expected state`() = with(testInput) {
        val actualState = reducer.newStateFrom(currentState, event)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val loadedContactGroupDetailsUiModel = ContactGroupDetailsPreviewData.contactGroupDetailsSampleData
        private val loadedContactGroupDetailsUiModel2 = ContactGroupDetailsPreviewData
            .contactGroupDetailsSampleData
            .copy(memberCount = 0, members = emptyList())

        private val emptyLoadingState = ContactGroupDetailsState.Loading()
        private val loadedContactGroupState = ContactGroupDetailsState.Data(
            isSendEnabled = true,
            contactGroup = loadedContactGroupDetailsUiModel,
            deleteDialogState = DeleteDialogState.Hidden
        )

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = emptyLoadingState,
                event = ContactGroupDetailsEvent.ContactGroupLoaded(loadedContactGroupDetailsUiModel),
                expectedState = loadedContactGroupState
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactGroupDetailsEvent.LoadContactGroupError,
                expectedState = emptyLoadingState.copy(
                    errorLoading = Effect.of(TextUiModel(R.string.contact_group_details_loading_error))
                )
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactGroupDetailsEvent.CloseContactGroupDetails,
                expectedState = emptyLoadingState.copy(
                    close = Effect.of(Unit)
                )
            )
        )

        private val transitionsFromDataState = listOf(
            TestInput(
                currentState = loadedContactGroupState,
                event = ContactGroupDetailsEvent.ContactGroupLoaded(loadedContactGroupDetailsUiModel2),
                expectedState = loadedContactGroupState.copy(
                    isSendEnabled = false,
                    contactGroup = loadedContactGroupDetailsUiModel2
                )
            ),
            TestInput(
                currentState = loadedContactGroupState,
                event = ContactGroupDetailsEvent.LoadContactGroupError,
                expectedState = loadedContactGroupState
            ),
            TestInput(
                currentState = loadedContactGroupState,
                event = ContactGroupDetailsEvent.CloseContactGroupDetails,
                expectedState = loadedContactGroupState.copy(
                    close = Effect.of(Unit)
                )
            ),
            TestInput(
                currentState = loadedContactGroupState,
                event = ContactGroupDetailsEvent.ComposeEmail(
                    loadedContactGroupState.contactGroup.members.map { it.email }
                ),
                expectedState = loadedContactGroupState.copy(
                    openComposer = Effect.of(
                        loadedContactGroupState.contactGroup.members.map { it.email }
                    )
                )
            ),
            TestInput(
                currentState = loadedContactGroupState,
                event = ContactGroupDetailsEvent.ShowDeleteDialog,
                expectedState = loadedContactGroupState.copy(
                    deleteDialogState = DeleteDialogState.Shown(
                        title = TextUiModel(
                            R.string.contact_group_delete_dialog_title,
                            loadedContactGroupState.contactGroup.name
                        ),
                        message = TextUiModel(R.string.contact_group_delete_dialog_message)
                    )
                )
            ),
            TestInput(
                currentState = loadedContactGroupState,
                event = ContactGroupDetailsEvent.DismissDeleteDialog,
                expectedState = loadedContactGroupState.copy(
                    deleteDialogState = DeleteDialogState.Hidden
                )
            ),
            TestInput(
                currentState = loadedContactGroupState,
                event = ContactGroupDetailsEvent.DeletingError,
                expectedState = loadedContactGroupState.copy(
                    deleteDialogState = DeleteDialogState.Hidden,
                    deletionError = Effect.of(TextUiModel(R.string.contact_group_details_deletion_error))
                )
            ),
            TestInput(
                currentState = loadedContactGroupState,
                event = ContactGroupDetailsEvent.DeletingSuccess,
                expectedState = loadedContactGroupState.copy(
                    deleteDialogState = DeleteDialogState.Hidden,
                    deletionSuccess = Effect.of(TextUiModel(R.string.contact_group_details_deletion_success))
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
        val currentState: ContactGroupDetailsState,
        val event: ContactGroupDetailsEvent,
        val expectedState: ContactGroupDetailsState
    )
}
