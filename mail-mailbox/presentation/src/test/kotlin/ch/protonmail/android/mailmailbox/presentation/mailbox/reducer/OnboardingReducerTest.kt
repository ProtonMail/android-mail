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

package ch.protonmail.android.mailmailbox.presentation.mailbox.reducer

import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.OnboardingState
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class OnboardingReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val onboardingReducer = OnboardingReducer()

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        val actualState = onboardingReducer.newStateFrom(operation)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val transitionsFromHiddenState = listOf(
            TestInput(
                currentState = OnboardingState.Hidden,
                operation = MailboxEvent.ShowOnboarding,
                expectedState = OnboardingState.Shown
            ),
            TestInput(
                currentState = OnboardingState.Hidden,
                operation = MailboxViewAction.CloseOnboarding,
                expectedState = OnboardingState.Hidden
            )
        )

        private val transitionsFromShownState = listOf(
            TestInput(
                currentState = OnboardingState.Shown,
                operation = MailboxViewAction.CloseOnboarding,
                expectedState = OnboardingState.Hidden
            ),
            TestInput(
                currentState = OnboardingState.Shown,
                operation = MailboxEvent.ShowOnboarding,
                expectedState = OnboardingState.Shown
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = (transitionsFromHiddenState + transitionsFromShownState)
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
        val currentState: OnboardingState,
        val operation: MailboxOperation.AffectingOnboarding,
        val expectedState: OnboardingState
    )
}
