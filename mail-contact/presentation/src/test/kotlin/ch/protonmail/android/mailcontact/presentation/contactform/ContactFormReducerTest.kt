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

package ch.protonmail.android.mailcontact.presentation.contactform

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.FieldType
import ch.protonmail.android.mailcontact.presentation.model.InputField
import ch.protonmail.android.mailcontact.presentation.model.emptyContactFormUiModel
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactFormPreviewData
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ContactFormReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val reducer = ContactFormReducer()

    @Test
    fun `should produce the expected state`() = with(testInput) {
        val actualState = reducer.newStateFrom(currentState, event)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val loadedContactFormUiModel = ContactFormPreviewData.contactFormSampleData

        private val emptyLoadingState = ContactFormState.Loading()
        private val loadedCreateContactState = ContactFormState.Data.Create(contact = emptyContactFormUiModel)
        private val loadedUpdateContactState = ContactFormState.Data.Update(contact = loadedContactFormUiModel)

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = emptyLoadingState,
                event = ContactFormEvent.NewContact(emptyContactFormUiModel),
                expectedState = loadedCreateContactState
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactFormEvent.EditContact(loadedContactFormUiModel),
                expectedState = loadedUpdateContactState
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactFormEvent.LoadContactError,
                expectedState = emptyLoadingState.copy(
                    errorLoading = Effect.of(TextUiModel(R.string.contact_form_loading_error))
                )
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ContactFormEvent.CloseContactForm,
                expectedState = emptyLoadingState.copy(
                    close = Effect.of(Unit)
                )
            )
        )

        private val transitionsFromDataState = listOf(
            TestInput(
                currentState = loadedCreateContactState,
                event = ContactFormEvent.CloseContactForm,
                expectedState = loadedCreateContactState.copy(
                    close = Effect.of(Unit)
                )
            ),
            TestInput(
                currentState = loadedUpdateContactState,
                event = ContactFormEvent.CloseContactForm,
                expectedState = loadedUpdateContactState.copy(
                    close = Effect.of(Unit)
                )
            ),
            TestInput(
                currentState = loadedCreateContactState,
                event = ContactFormEvent.UpdateContact(
                    contact = loadedContactFormUiModel.copy(displayName = "Updated displayName")
                ),
                expectedState = loadedCreateContactState.copy(
                    contact = loadedContactFormUiModel.copy(displayName = "Updated displayName")
                )
            ),
            TestInput(
                currentState = loadedUpdateContactState,
                event = ContactFormEvent.UpdateContact(
                    contact = emptyContactFormUiModel.copy(displayName = "Updated displayName")
                ),
                expectedState = loadedUpdateContactState.copy(
                    contact = emptyContactFormUiModel.copy(displayName = "Updated displayName")
                )
            ),
            TestInput(
                currentState = loadedCreateContactState,
                event = ContactFormEvent.UpdateContact(
                    contact = loadedContactFormUiModel.copy(
                        emails = loadedContactFormUiModel.emails.apply {
                            this[0] = InputField.SingleTyped(
                                value = "Updated",
                                selectedType = FieldType.EmailType.Work
                            )
                        }
                    )
                ),
                expectedState = loadedCreateContactState.copy(
                    contact = loadedContactFormUiModel.copy(
                        emails = loadedContactFormUiModel.emails.apply {
                            this[0] = InputField.SingleTyped(
                                value = "Updated",
                                selectedType = FieldType.EmailType.Work
                            )
                        }
                    )
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
        val currentState: ContactFormState,
        val event: ContactFormEvent,
        val expectedState: ContactFormState
    )

}
