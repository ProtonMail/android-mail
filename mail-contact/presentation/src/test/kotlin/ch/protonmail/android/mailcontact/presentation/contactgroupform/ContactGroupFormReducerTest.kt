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

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.contacgroupform.ContactGroupFormEvent
import ch.protonmail.android.mailcontact.presentation.contacgroupform.ContactGroupFormReducer
import ch.protonmail.android.mailcontact.presentation.contacgroupform.ContactGroupFormState
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

        private val emptyLoadingState = ContactGroupFormState.Loading()
        private val loadedContactGroupState = ContactGroupFormState.Data(
            contactGroup = loadedContactGroupFormUiModel
        )

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = emptyLoadingState,
                event = ContactGroupFormEvent.ContactGroupLoaded(loadedContactGroupFormUiModel),
                expectedState = loadedContactGroupState
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
                currentState = loadedContactGroupState,
                event = ContactGroupFormEvent.ContactGroupLoaded(loadedContactGroupFormUiModel2),
                expectedState = loadedContactGroupState.copy(
                    contactGroup = loadedContactGroupFormUiModel2
                )
            ),
            TestInput(
                currentState = loadedContactGroupState,
                event = ContactGroupFormEvent.LoadError,
                expectedState = loadedContactGroupState
            ),
            TestInput(
                currentState = loadedContactGroupState,
                event = ContactGroupFormEvent.Close,
                expectedState = loadedContactGroupState.copy(
                    close = Effect.of(Unit)
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