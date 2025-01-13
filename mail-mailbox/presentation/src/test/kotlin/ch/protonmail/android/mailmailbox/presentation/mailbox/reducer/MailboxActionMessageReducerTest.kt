package ch.protonmail.android.mailmailbox.presentation.mailbox.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.presentation.mapper.MailLabelTextMapper
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import io.mockk.every
import io.mockk.mockk
import me.proton.core.mailsettings.domain.entity.ViewMode
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals
import ch.protonmail.android.maillabel.presentation.R as labelR

@RunWith(Parameterized::class)
internal class MailboxActionMessageReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val mailLabelTextMapper = mockk<MailLabelTextMapper> {
        every { this@mockk.mapToString(MailLabelText.TextRes(labelR.string.label_title_archive)) } returns "Archive"
        every { this@mockk.mapToString(MailLabelText.TextRes(labelR.string.label_title_spam)) } returns "Spam"
        every { this@mockk.mapToString(MailLabelText.TextRes(labelR.string.label_title_trash)) } returns "Trash"
    }
    private val actionMessageReducer = MailboxActionMessageReducer(mailLabelTextMapper)

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        val actualState = actionMessageReducer.newStateFrom(operation)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val transitions = listOf(
            TestInput(
                operation = MailboxEvent.Trash(ViewMode.NoConversationGrouping, 5),
                expectedState = Effect.of(
                    ActionResult.UndoableActionResult(TextUiModel(R.plurals.mailbox_action_trash_message, 5))
                )
            ),
            TestInput(
                operation = MailboxEvent.Trash(ViewMode.ConversationGrouping, 5),
                expectedState = Effect.of(
                    ActionResult.UndoableActionResult(TextUiModel(R.plurals.mailbox_action_trash_conversation, 5))
                )
            ),
            TestInput(
                operation = MailboxEvent.SwipeActionMoveCompleted.Archive(
                    viewMode = ViewMode.ConversationGrouping
                ),
                expectedState = Effect.of(
                    ActionResult.UndoableActionResult(
                        TextUiModel.TextResWithArgs(R.string.mailbox_action_move_conversation, listOf("Archive"))
                    )
                )
            ),
            TestInput(
                operation = MailboxEvent.SwipeActionMoveCompleted.Archive(
                    viewMode = ViewMode.NoConversationGrouping
                ),
                expectedState = Effect.of(
                    ActionResult.UndoableActionResult(
                        TextUiModel.TextResWithArgs(R.string.mailbox_action_move_message, listOf("Archive"))
                    )
                )
            ),
            TestInput(
                operation = MailboxEvent.SwipeActionMoveCompleted.Spam(
                    viewMode = ViewMode.ConversationGrouping
                ),
                expectedState = Effect.of(
                    ActionResult.UndoableActionResult(
                        TextUiModel.TextResWithArgs(R.string.mailbox_action_move_conversation, listOf("Spam"))
                    )
                )
            ),
            TestInput(
                operation = MailboxEvent.SwipeActionMoveCompleted.Spam(
                    viewMode = ViewMode.NoConversationGrouping
                ),
                expectedState = Effect.of(
                    ActionResult.UndoableActionResult(
                        TextUiModel.TextResWithArgs(R.string.mailbox_action_move_message, listOf("Spam"))
                    )
                )
            ),
            TestInput(
                operation = MailboxEvent.SwipeActionMoveCompleted.Trash(
                    viewMode = ViewMode.ConversationGrouping
                ),
                expectedState = Effect.of(
                    ActionResult.UndoableActionResult(
                        TextUiModel.TextResWithArgs(R.string.mailbox_action_move_conversation, listOf("Trash"))
                    )
                )
            ),
            TestInput(
                operation = MailboxEvent.SwipeActionMoveCompleted.Trash(
                    viewMode = ViewMode.NoConversationGrouping
                ),
                expectedState = Effect.of(
                    ActionResult.UndoableActionResult(
                        TextUiModel.TextResWithArgs(R.string.mailbox_action_move_message, listOf("Trash"))
                    )
                )
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
        val expectedState: Effect<ActionResult>
    )
}
