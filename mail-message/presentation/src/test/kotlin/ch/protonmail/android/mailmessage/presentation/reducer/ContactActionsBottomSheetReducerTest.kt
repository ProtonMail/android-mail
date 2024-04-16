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

package ch.protonmail.android.mailmessage.presentation.reducer

import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailmessage.domain.model.Participant
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.ContactActionsBottomSheetState
import ch.protonmail.android.testdata.contact.ContactSample
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class ContactActionsBottomSheetReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val reducer = ContactActionsBottomSheetReducer()

    @Test
    fun `should produce the expected new bottom sheet state`() = with(testInput) {
        val actualState = reducer.newStateFrom(currentState, operation)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {
        private val sampleParticipant = Participant(
            address = "test@proton.me",
            name = "Test User"
        )

        private val sampleContact = ContactSample.Stefano

        private val sampleAvatar = AvatarUiModel.ParticipantInitial("S")
        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = BottomSheetState(ContactActionsBottomSheetState.Loading),
                operation = ContactActionsBottomSheetState.ContactActionsBottomSheetEvent.ActionData(
                    participant = sampleParticipant,
                    avatarUiModel = sampleAvatar,
                    contactId = sampleContact.id
                ),
                expectedState = BottomSheetState(
                    contentState = ContactActionsBottomSheetState.Data(
                        participant = sampleParticipant,
                        avatarUiModel = sampleAvatar,
                        contactId = sampleContact.id
                    )
                )
            ),
            TestInput(
                currentState = BottomSheetState(ContactActionsBottomSheetState.Loading),
                operation = ContactActionsBottomSheetState.ContactActionsBottomSheetEvent.ActionData(
                    participant = sampleParticipant,
                    avatarUiModel = sampleAvatar,
                    contactId = null
                ),
                expectedState = BottomSheetState(
                    contentState = ContactActionsBottomSheetState.Data(
                        participant = sampleParticipant,
                        avatarUiModel = sampleAvatar,
                        contactId = null
                    )
                )
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = transitionsFromLoadingState
            .map { testInput ->
                val testName = """
                    Current state: ${testInput.currentState}
                    Operation: ${testInput.operation}
                    Next state: ${testInput.expectedState}   
                """.trimIndent()
                arrayOf(testName, testInput)
            }
    }

    data class TestInput(
        val currentState: BottomSheetState,
        val operation: ContactActionsBottomSheetState.ContactActionsBottomSheetOperation,
        val expectedState: BottomSheetState
    )
}
