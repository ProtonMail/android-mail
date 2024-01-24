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

package ch.protonmail.android.maildetail.presentation.reducer

import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.maildetail.presentation.model.ReportPhishingDialogState
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class MessageReportPhishingDialogReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val reportPhishingDialogReducer = MessageReportPhishingDialogReducer()

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        val actualState = reportPhishingDialogReducer.newStateFrom(operation)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val transitions = listOf(
            TestInput(
                operation = MessageViewAction.ReportPhishingDismissed,
                expectedState = ReportPhishingDialogState.Hidden
            ),
            TestInput(
                operation = MessageViewAction.ReportPhishingConfirmed,
                expectedState = ReportPhishingDialogState.Hidden
            ),
            TestInput(
                operation = MessageDetailEvent.ReportPhishingRequested(
                    messageId = MessageIdSample.Invoice,
                    isOffline = true
                ),
                expectedState = ReportPhishingDialogState.Shown.ShowOfflineHint
            ),
            TestInput(
                operation = MessageDetailEvent.ReportPhishingRequested(
                    messageId = MessageIdSample.Invoice,
                    isOffline = false
                ),
                expectedState = ReportPhishingDialogState.Shown.ShowConfirmation(messageId = MessageIdSample.Invoice)
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            return transitions
                .map {
                    val testName = """
                        Operation: ${it.operation}
                        Next State: ${it.expectedState}
                    """.trimIndent()
                    arrayOf(testName, it)
                }
        }

        data class TestInput(
            val operation: MessageDetailOperation.AffectingReportPhishingDialog,
            val expectedState: ReportPhishingDialogState
        )
    }
}
