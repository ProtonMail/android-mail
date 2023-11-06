package ch.protonmail.android.mailmailbox.presentation.mailbox.reducer

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.DeleteDialogState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation.AffectingDeleteDialog
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import me.proton.core.mailsettings.domain.entity.ViewMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class MailboxDeleteDialogReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val deleteDialogReducer = MailboxDeleteDialogReducer()

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        val actualState = deleteDialogReducer.newStateFrom(operation)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val transitions = listOf(
            TestInput(
                operation = MailboxEvent.Delete(ViewMode.ConversationGrouping, 5),
                expectedState =
                DeleteDialogState.Shown(
                    title = TextUiModel.PluralisedText(
                        value = R.plurals.mailbox_action_delete_conversation_dialog_title,
                        count = 5
                    ),
                    message = TextUiModel.PluralisedText(
                        value = R.plurals.mailbox_action_delete_conversation_dialog_message,
                        count = 5
                    )
                )
            ),
            TestInput(
                operation = MailboxEvent.Delete(ViewMode.NoConversationGrouping, 5),
                expectedState =
                DeleteDialogState.Shown(
                    title = TextUiModel.PluralisedText(
                        value = R.plurals.mailbox_action_delete_message_dialog_title,
                        count = 5
                    ),
                    message = TextUiModel.PluralisedText(
                        value = R.plurals.mailbox_action_delete_message_dialog_message,
                        count = 5
                    )
                )
            ),
            TestInput(
                operation = MailboxEvent.DeleteConfirmed(ViewMode.NoConversationGrouping, 5),
                expectedState = DeleteDialogState.Hidden
            ),
            TestInput(
                operation = MailboxEvent.DeleteConfirmed(ViewMode.ConversationGrouping, 5),
                expectedState = DeleteDialogState.Hidden
            ),
            TestInput(
                operation = MailboxViewAction.DeleteDialogDismissed,
                expectedState = DeleteDialogState.Hidden
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

    }

    data class TestInput(
        val operation: AffectingDeleteDialog,
        val expectedState: DeleteDialogState
    )

}
