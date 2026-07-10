package ch.protonmail.android.mailmailbox.presentation.mailbox.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.model.ViewMode
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmessage.presentation.mapper.MailLabelTextMapper
import io.mockk.every
import io.mockk.mockk
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
        every { this@mockk.mapToString(customMailLabelText) } returns customFolderName
    }

    private val actionMessageReducer = MailboxActionMessageReducer(mailLabelTextMapper)

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        val actualState = actionMessageReducer.newStateFrom(operation)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private const val customFolderName = "My folder"
        private val customMailLabelText = MailLabelText(customFolderName)

        private val transitions = listOf(
            TestInput(
                operation = MailboxEvent.MoveToConfirmed.Custom(
                    viewMode = ViewMode.NoConversationGrouping,
                    itemCount = 5,
                    label = customMailLabelText
                ),
                expectedState = Effect.of(
                    ActionResult.UndoableActionResult(
                        TextUiModel.PluralisedText(
                            R.plurals.mailbox_action_move_message,
                            5,
                            listOf(customFolderName)
                        )
                    )
                )
            ),
            TestInput(
                operation = MailboxEvent.MoveToConfirmed.Custom(
                    viewMode = ViewMode.ConversationGrouping,
                    itemCount = 5,
                    label = customMailLabelText
                ),
                expectedState = Effect.of(
                    ActionResult.UndoableActionResult(
                        TextUiModel.PluralisedText(
                            R.plurals.mailbox_action_move_conversation,
                            5,
                            listOf(customFolderName)
                        )
                    )
                )
            ),
            TestInput(
                operation = MailboxEvent.MoveToConfirmed.Category(
                    viewMode = ViewMode.NoConversationGrouping,
                    itemCount = 5,
                    label = customMailLabelText
                ),
                expectedState = Effect.of(
                    ActionResult.UndoableActionResult(
                        TextUiModel.TextResWithArgs(
                            R.string.mailbox_action_move_to_category,
                            listOf(customFolderName)
                        )
                    )
                )
            ),
            TestInput(
                operation = MailboxEvent.MoveToConfirmed.Category(
                    viewMode = ViewMode.ConversationGrouping,
                    itemCount = 5,
                    label = customMailLabelText
                ),
                expectedState = Effect.of(
                    ActionResult.UndoableActionResult(
                        TextUiModel.TextResWithArgs(
                            R.string.mailbox_action_move_to_category,
                            listOf(customFolderName)
                        )
                    )
                )
            ),
            TestInput(
                operation = MailboxEvent.MoveToConfirmed.Archive(
                    viewMode = ViewMode.ConversationGrouping,
                    itemCount = 1
                ),
                expectedState = Effect.of(
                    ActionResult.UndoableActionResult(
                        TextUiModel.PluralisedText(R.plurals.mailbox_action_move_conversation, 1, listOf("Archive"))
                    )
                )
            ),
            TestInput(
                operation = MailboxEvent.MoveToConfirmed.Archive(
                    viewMode = ViewMode.NoConversationGrouping,
                    itemCount = 1
                ),
                expectedState = Effect.of(
                    ActionResult.UndoableActionResult(
                        TextUiModel.PluralisedText(R.plurals.mailbox_action_move_message, 1, listOf("Archive"))
                    )
                )
            ),
            TestInput(
                operation = MailboxEvent.MoveToConfirmed.Spam(
                    viewMode = ViewMode.ConversationGrouping,
                    itemCount = 1
                ),
                expectedState = Effect.of(
                    ActionResult.UndoableActionResult(
                        TextUiModel.PluralisedText(R.plurals.mailbox_action_move_conversation, 1, listOf("Spam"))
                    )
                )
            ),
            TestInput(
                operation = MailboxEvent.MoveToConfirmed.Spam(
                    viewMode = ViewMode.NoConversationGrouping,
                    itemCount = 1
                ),
                expectedState = Effect.of(
                    ActionResult.UndoableActionResult(
                        TextUiModel.PluralisedText(
                            R.plurals.mailbox_action_move_message,
                            1,
                            listOf("Spam")
                        )
                    )
                )
            ),
            TestInput(
                operation = MailboxEvent.MoveToConfirmed.Trash(
                    viewMode = ViewMode.ConversationGrouping,
                    itemCount = 1
                ),
                expectedState = Effect.of(
                    ActionResult.UndoableActionResult(
                        TextUiModel.PluralisedText(
                            R.plurals.mailbox_action_move_conversation,
                            1,
                            listOf("Trash")
                        )
                    )
                )
            ),
            TestInput(
                operation = MailboxEvent.MoveToConfirmed.Trash(
                    viewMode = ViewMode.NoConversationGrouping,
                    itemCount = 1
                ),
                expectedState = Effect.of(
                    ActionResult.UndoableActionResult(
                        TextUiModel.PluralisedText(
                            R.plurals.mailbox_action_move_message,
                            1,
                            listOf("Trash")
                        )
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
