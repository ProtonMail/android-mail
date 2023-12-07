package ch.protonmail.android.mailmailbox.presentation.mailbox.reducer

import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class MailboxActionMessageReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val actionMessageReducer = MailboxActionMessageReducer()

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        val actualState = actionMessageReducer.newStateFrom(operation)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val transitions = listOf(
            TestInput(
                operation = MailboxEvent.Trash(5),
                expectedState = Effect.of(TextUiModel(R.plurals.mailbox_action_trash, 5))
            ),
            TestInput(
                operation = MailboxViewAction.SwipeStarAction(UserIdSample.Primary, "itemId", false),
                expectedState = Effect.of(TextUiModel(R.string.mailbox_action_star_message))
            ),
            TestInput(
                operation = MailboxViewAction.SwipeTrashAction(UserIdSample.Primary, "itemId"),
                expectedState = Effect.of(TextUiModel(R.string.mailbox_action_trash_message))
            ),
            TestInput(
                operation = MailboxViewAction.SwipeArchiveAction(UserIdSample.Primary, "itemId"),
                expectedState = Effect.of(TextUiModel(R.string.mailbox_action_archive_message))
            ),
            TestInput(
                operation = MailboxViewAction.SwipeReadAction(UserIdSample.Primary, "itemId", false),
                expectedState = Effect.of(TextUiModel(R.string.mailbox_action_read_message))
            ),
            TestInput(
                operation = MailboxViewAction.SwipeSpamAction(UserIdSample.Primary, "itemId"),
                expectedState = Effect.of(TextUiModel(R.string.mailbox_action_spam_message))
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
        val operation: MailboxOperation.AffectingActionMessage,
        val expectedState: Effect<TextUiModel>
    )

}
