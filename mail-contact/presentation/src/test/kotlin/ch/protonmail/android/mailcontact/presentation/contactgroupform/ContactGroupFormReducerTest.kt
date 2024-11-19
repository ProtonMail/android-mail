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

package ch.protonmail.android.mailcontact.presentation.contactgroupform

import androidx.compose.ui.graphics.Color
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.emptyContactGroupFormUiModel
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactGroupFormPreviewData
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ContactGroupFormReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val reducer = ContactGroupFormReducer()

    @Test
    fun `should produce the expected state`() = with(testInput) {
        val actualState = reducer.newStateFrom(currentState, event)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val loadedContactGroupFormUiModel = ContactGroupFormPreviewData.contactGroupFormSampleData
        private val loadedContactGroupFormUiModel2 = ContactGroupFormPreviewData
            .contactGroupFormSampleData
            .copy(memberCount = 0, members = emptyList())
        private val loadedContactGroupFormUiModelBlankName = ContactGroupFormPreviewData
            .contactGroupFormSampleData
            .copy(name = " ")

        private val emptyLoadingState = ContactGroupFormState.Loading()
        private val loadedCreateContactGroupState = ContactGroupFormState.Data(
            contactGroup = emptyContactGroupFormUiModel(Color.Red),
            colors = emptyList()
        )
        private val loadedUpdateContactGroupState = ContactGroupFormState.Data(
            contactGroup = loadedContactGroupFormUiModel,
            colors = emptyList(),
            isSaveEnabled = true
        )
        private val loadedUpdateContactGroupStateBlankName = ContactGroupFormState.Data(
            contactGroup = loadedContactGroupFormUiModelBlankName,
            colors = emptyList(),
            isSaveEnabled = false
        )

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = emptyLoadingState,
                event = ContactGroupFormEvent.ContactGroupLoaded(
                    loadedContactGroupFormUiModel, emptyList()
                ),
                expectedState = loadedUpdateContactGroupState
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactGroupFormEvent.ContactGroupLoaded(
                    loadedContactGroupFormUiModelBlankName, emptyList()
                ),
                expectedState = loadedUpdateContactGroupStateBlankName
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactGroupFormEvent.LoadError,
                expectedState = emptyLoadingState.copy(
                    errorLoading = Effect.of(TextUiModel(R.string.contact_group_form_loading_error))
                )
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactGroupFormEvent.Close,
                expectedState = emptyLoadingState.copy(
                    close = Effect.of(Unit)
                )
            )
        )

        private val transitionsFromDataState = listOf(
            TestInput(
                currentState = loadedUpdateContactGroupState,
                event = ContactGroupFormEvent.ContactGroupLoaded(
                    loadedContactGroupFormUiModel2, emptyList()
                ),
                expectedState = loadedUpdateContactGroupState.copy(
                    contactGroup = loadedContactGroupFormUiModel2
                )
            ),
            TestInput(
                currentState = loadedUpdateContactGroupState,
                event = ContactGroupFormEvent.LoadError,
                expectedState = loadedUpdateContactGroupState
            ),
            TestInput(
                currentState = loadedUpdateContactGroupState,
                event = ContactGroupFormEvent.Close,
                expectedState = loadedUpdateContactGroupState.copy(
                    close = Effect.of(Unit)
                )
            ),
            TestInput(
                currentState = loadedCreateContactGroupState,
                event = ContactGroupFormEvent.ContactGroupCreated,
                expectedState = loadedCreateContactGroupState.copy(
                    closeWithSuccess = Effect.of(TextUiModel(R.string.contact_group_form_create_success))
                )
            ),
            TestInput(
                currentState = loadedUpdateContactGroupState,
                event = ContactGroupFormEvent.ContactGroupUpdated,
                expectedState = loadedUpdateContactGroupState.copy(
                    closeWithSuccess = Effect.of(TextUiModel(R.string.contact_group_form_update_success))
                )
            ),
            TestInput(
                currentState = loadedUpdateContactGroupState,
                event = ContactGroupFormEvent.UpdateContactGroupFormUiModel(
                    loadedContactGroupFormUiModelBlankName
                ),
                expectedState = loadedUpdateContactGroupState.copy(
                    contactGroup = loadedContactGroupFormUiModelBlankName,
                    isSaveEnabled = false
                )
            ),
            TestInput(
                currentState = loadedUpdateContactGroupStateBlankName,
                event = ContactGroupFormEvent.UpdateContactGroupFormUiModel(
                    loadedContactGroupFormUiModel
                ),
                expectedState = loadedUpdateContactGroupState.copy(
                    contactGroup = loadedContactGroupFormUiModel,
                    isSaveEnabled = true
                )
            ),
            TestInput(
                currentState = loadedUpdateContactGroupState,
                event = ContactGroupFormEvent.SaveContactGroupError,
                expectedState = loadedUpdateContactGroupState.copy(
                    showErrorSnackbar = Effect.of(TextUiModel(R.string.contact_group_form_save_error))
                )
            ),
            TestInput(
                currentState = loadedUpdateContactGroupState,
                event = ContactGroupFormEvent.SavingContactGroup,
                expectedState = loadedUpdateContactGroupState.copy(
                    displaySaveLoader = true
                )
            ),
            TestInput(
                currentState = loadedUpdateContactGroupState,
                event = ContactGroupFormEvent.UpdateMembersError,
                expectedState = loadedUpdateContactGroupState.copy(
                    showErrorSnackbar = Effect.of(TextUiModel(R.string.add_members_error))
                )
            ),
            TestInput(
                currentState = loadedUpdateContactGroupState,
                event = ContactGroupFormEvent.ShowDeleteDialog,
                expectedState = loadedUpdateContactGroupState.copy(
                    deleteDialogState = DeleteDialogState.Shown(
                        title = TextUiModel(
                            R.string.contact_group_delete_dialog_title,
                            loadedUpdateContactGroupState.contactGroup.name
                        ),
                        message = TextUiModel(R.string.contact_group_delete_dialog_message)
                    )
                )
            ),
            TestInput(
                currentState = loadedUpdateContactGroupState,
                event = ContactGroupFormEvent.DismissDeleteDialog,
                expectedState = loadedUpdateContactGroupState.copy(
                    deleteDialogState = DeleteDialogState.Hidden
                )
            ),
            TestInput(
                currentState = loadedUpdateContactGroupState,
                event = ContactGroupFormEvent.DeletingError,
                expectedState = loadedUpdateContactGroupState.copy(
                    deleteDialogState = DeleteDialogState.Hidden,
                    deletionError = Effect.of(TextUiModel(R.string.contact_group_details_deletion_error))
                )
            ),
            TestInput(
                currentState = loadedUpdateContactGroupState,
                event = ContactGroupFormEvent.SubscriptionNeededError,
                expectedState = loadedUpdateContactGroupState.copy(
                    subscriptionNeededError = Effect.of(TextUiModel(R.string.contact_group_form_subscription_error)),
                    displaySaveLoader = false
                )
            ),
            TestInput(
                currentState = loadedUpdateContactGroupState,
                event = ContactGroupFormEvent.DeletingSuccess,
                expectedState = loadedUpdateContactGroupState.copy(
                    deleteDialogState = DeleteDialogState.Hidden,
                    deletionSuccess = Effect.of(TextUiModel(R.string.contact_group_details_deletion_success))
                )
            ),
            TestInput(
                currentState = loadedUpdateContactGroupState.copy(
                    displaySaveLoader = true
                ),
                event = ContactGroupFormEvent.DuplicatedContactGroupName,
                expectedState = loadedUpdateContactGroupState.copy(
                    showErrorSnackbar = Effect.of(TextUiModel(R.string.contact_group_form_save_error_already_exists)),
                    displaySaveLoader = false
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
        val currentState: ContactGroupFormState,
        val event: ContactGroupFormEvent,
        val expectedState: ContactGroupFormState
    )

}
