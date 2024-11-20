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

import ch.protonmail.android.mailmailbox.domain.model.UserAccountStorageStatus
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.StorageLimitState
import io.mockk.MockKAnnotations
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class StorageLimitReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val reducer = StorageLimitReducer()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `should produce the expected new state`() {
        // Given
        val currentState = testInput.currentState
        val event = testInput.event

        // When
        val actualState = reducer.newStateFrom(currentState, event)

        // Then
        assert(actualState == testInput.expectedState) { testName }
    }


    companion object {

        private val transitionsFromNoneState = listOf(
            // Transition from StorageLimitState.None to StorageLimitState.HasEnoughSpace
            TestInput(
                currentState = StorageLimitState.None,
                event = MailboxEvent.StorageLimitStatusChanged(
                    userAccountStorageStatus = createStorageStatus()
                ),
                expectedState = StorageLimitState.HasEnoughSpace
            ),
            // Transition from StorageLimitState.None to StorageLimitState.Notifiable.QuotaOver
            TestInput(
                currentState = StorageLimitState.None,
                event = MailboxEvent.StorageLimitStatusChanged(
                    userAccountStorageStatus = createStorageStatus(overQuota = true)
                ),
                expectedState = StorageLimitState.Notifiable.QuotaOver(false)
            )
        )

        private val transitionsFromQuotaOver =
            transitionsFromNoneState.map {
                it.transformInput(
                    currentState = StorageLimitState.Notifiable.QuotaOver(false),
                    expectedStateConfirmed = false
                )
            }

        private fun TestInput.transformInput(
            currentState: StorageLimitState,
            expectedStateConfirmed: Boolean
        ): TestInput {
            return this.copy(
                currentState = currentState,
                expectedState = this.expectedState.transformState(expectedStateConfirmed)
            )
        }

        private fun StorageLimitState.transformState(stateConfirmed: Boolean): StorageLimitState {
            return when (this) {
                is StorageLimitState.None -> this
                is StorageLimitState.Notifiable.QuotaOver -> this.copy(confirmed = stateConfirmed)
                is StorageLimitState.HasEnoughSpace -> this
            }
        }

        private fun createStorageStatus(overQuota: Boolean = false): UserAccountStorageStatus {
            val maxSpace = 10_000L
            val usedSpace: Long = if (overQuota) maxSpace + 1 else 0L

            return UserAccountStorageStatus(
                usedSpace = usedSpace,
                maxSpace = maxSpace
            )
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            val transitions =
                transitionsFromNoneState + transitionsFromQuotaOver

            return transitions.map { testInput ->
                val testName = """
                    Current state: ${testInput.currentState}
                    Event: ${testInput.event}
                    Expected state: ${testInput.expectedState}
                """.trimIndent()
                arrayOf(testName, testInput)
            }
        }
    }

    data class TestInput(
        val currentState: StorageLimitState,
        val event: MailboxOperation.AffectingStorageLimit,
        val expectedState: StorageLimitState
    )
}
