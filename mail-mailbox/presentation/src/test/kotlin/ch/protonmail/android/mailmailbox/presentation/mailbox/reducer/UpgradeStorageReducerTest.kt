package ch.protonmail.android.mailmailbox.presentation.mailbox.reducer

import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UpgradeStorageState
import io.mockk.MockKAnnotations
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class UpgradeStorageReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val reducer = UpgradeStorageReducer()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `should produce the expected new state`() {
        // Given
        val currentState = testInput.currentState
        val event = testInput.operation

        // When
        val actualState = reducer.newStateFrom(event)

        // Then
        assert(actualState == testInput.expectedState) { testName }
    }

    companion object {

        private val transitions = listOf(
            TestInput(
                currentState = UpgradeStorageState(false),
                operation = MailboxEvent.UpgradeStorageStatusChanged(true),
                expectedState = UpgradeStorageState(true)
            ),
            TestInput(
                currentState = UpgradeStorageState(false),
                operation = MailboxEvent.UpgradeStorageStatusChanged(false),
                expectedState = UpgradeStorageState(false)
            ),
            TestInput(
                currentState = UpgradeStorageState(true),
                operation = MailboxEvent.UpgradeStorageStatusChanged(false),
                expectedState = UpgradeStorageState(false)
            ),
            TestInput(
                currentState = UpgradeStorageState(true),
                operation = MailboxEvent.UpgradeStorageStatusChanged(true),
                expectedState = UpgradeStorageState(true)
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {

            return transitions.map { testInput ->
                val testName = """
                    Current state: ${testInput.currentState}
                    Event: ${testInput.operation}
                    Expected state: ${testInput.expectedState}
                """.trimIndent()
                arrayOf(testName, testInput)
            }
        }
    }
    data class TestInput(
        val currentState: UpgradeStorageState,
        val operation: MailboxOperation.AffectingUpgradeStorage,
        val expectedState: UpgradeStorageState
    )
}
