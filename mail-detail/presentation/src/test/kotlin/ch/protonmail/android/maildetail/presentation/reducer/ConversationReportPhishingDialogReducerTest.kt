package ch.protonmail.android.maildetail.presentation.reducer

import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maildetail.presentation.model.ReportPhishingDialogState
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ConversationReportPhishingDialogReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val reportPhishingDialogReducer = ConversationReportPhishingDialogReducer()

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        val actualState = reportPhishingDialogReducer.newStateFrom(operation)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val transitions = listOf(
            TestInput(
                operation = ConversationDetailViewAction.ReportPhishingDismissed,
                expectedState = ReportPhishingDialogState.Hidden
            ),
            TestInput(
                operation = ConversationDetailViewAction.ReportPhishingConfirmed(MessageIdSample.Invoice),
                expectedState = ReportPhishingDialogState.Hidden
            ),
            TestInput(
                operation = ConversationDetailEvent.ReportPhishingRequested(
                    messageId = MessageIdSample.HtmlInvoice,
                    isOffline = true
                ),
                expectedState = ReportPhishingDialogState.Shown.ShowOfflineHint
            ),
            TestInput(
                operation = ConversationDetailEvent.ReportPhishingRequested(
                    messageId = MessageIdSample.HtmlInvoice,
                    isOffline = false
                ),
                expectedState = ReportPhishingDialogState.Shown.ShowConfirmation(MessageIdSample.HtmlInvoice)
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
            val operation: ConversationDetailOperation.AffectingReportPhishingDialog,
            val expectedState: ReportPhishingDialogState
        )
    }

}
