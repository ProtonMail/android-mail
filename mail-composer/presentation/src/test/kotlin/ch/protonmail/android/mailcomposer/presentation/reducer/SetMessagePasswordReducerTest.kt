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

package ch.protonmail.android.mailcomposer.presentation.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcomposer.domain.model.MessagePassword
import ch.protonmail.android.mailcomposer.presentation.model.MessagePasswordOperation
import ch.protonmail.android.mailcomposer.presentation.model.SetMessagePasswordState
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.testdata.user.UserIdTestData
import me.proton.core.util.kotlin.EMPTY_STRING
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class SetMessagePasswordReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val setMessagePasswordReducer = SetMessagePasswordReducer()

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        val actualState = setMessagePasswordReducer.newStateFrom(currentState, operation)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private const val Password = "password"
        private const val Hint = "hint"
        private val messagePassword = MessagePassword(UserIdTestData.userId, MessageIdSample.EmptyDraft, Password, Hint)

        private val testInputList = listOf(
            TestInput(
                currentState = SetMessagePasswordState.Loading,
                operation = MessagePasswordOperation.Event.InitializeScreen(messagePassword),
                expectedState = SetMessagePasswordState.Data(
                    initialMessagePasswordValue = Password,
                    initialMessagePasswordHintValue = Hint,
                    hasMessagePasswordError = false,
                    hasRepeatedMessagePasswordError = false,
                    isInEditMode = true,
                    exitScreen = Effect.empty()
                )
            ),
            TestInput(
                currentState = SetMessagePasswordState.Loading,
                operation = MessagePasswordOperation.Event.InitializeScreen(null),
                expectedState = SetMessagePasswordState.Data(
                    initialMessagePasswordValue = EMPTY_STRING,
                    initialMessagePasswordHintValue = EMPTY_STRING,
                    hasMessagePasswordError = false,
                    hasRepeatedMessagePasswordError = false,
                    isInEditMode = false,
                    exitScreen = Effect.empty()
                )
            ),
            TestInput(
                currentState = SetMessagePasswordState.Data(
                    initialMessagePasswordValue = Password,
                    initialMessagePasswordHintValue = Hint,
                    hasMessagePasswordError = false,
                    hasRepeatedMessagePasswordError = false,
                    isInEditMode = true,
                    exitScreen = Effect.empty()
                ),
                operation = MessagePasswordOperation.Event.ExitScreen,
                expectedState = SetMessagePasswordState.Data(
                    initialMessagePasswordValue = Password,
                    initialMessagePasswordHintValue = Hint,
                    hasMessagePasswordError = false,
                    hasRepeatedMessagePasswordError = false,
                    isInEditMode = true,
                    exitScreen = Effect.of(Unit)
                )
            ),
            TestInput(
                currentState = SetMessagePasswordState.Data(
                    initialMessagePasswordValue = EMPTY_STRING,
                    initialMessagePasswordHintValue = EMPTY_STRING,
                    hasMessagePasswordError = false,
                    hasRepeatedMessagePasswordError = false,
                    isInEditMode = false,
                    exitScreen = Effect.empty()
                ),
                operation = MessagePasswordOperation.Event.PasswordValidated(hasMessagePasswordError = true),
                expectedState = SetMessagePasswordState.Data(
                    initialMessagePasswordValue = EMPTY_STRING,
                    initialMessagePasswordHintValue = EMPTY_STRING,
                    hasMessagePasswordError = true,
                    hasRepeatedMessagePasswordError = false,
                    isInEditMode = false,
                    exitScreen = Effect.empty()
                )
            ),
            TestInput(
                currentState = SetMessagePasswordState.Data(
                    initialMessagePasswordValue = EMPTY_STRING,
                    initialMessagePasswordHintValue = EMPTY_STRING,
                    hasMessagePasswordError = false,
                    hasRepeatedMessagePasswordError = false,
                    isInEditMode = false,
                    exitScreen = Effect.empty()
                ),
                operation = MessagePasswordOperation.Event.RepeatedPasswordValidated(
                    hasRepeatedMessagePasswordError = true
                ),
                expectedState = SetMessagePasswordState.Data(
                    initialMessagePasswordValue = EMPTY_STRING,
                    initialMessagePasswordHintValue = EMPTY_STRING,
                    hasMessagePasswordError = false,
                    hasRepeatedMessagePasswordError = true,
                    isInEditMode = false,
                    exitScreen = Effect.empty()
                )
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = testInputList
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
        val currentState: SetMessagePasswordState,
        val operation: MessagePasswordOperation.Event,
        val expectedState: SetMessagePasswordState
    )
}
