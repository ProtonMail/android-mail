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

package ch.protonmail.android.mailnotifications.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class NotificationInteractionTest(
    @Suppress("unused") private val testName: String,
    private val input: TestInput
) {

    @Test
    fun testResolveNotificationInteraction() {
        val interaction = resolveNotificationInteraction(input.userId, input.messageId, input.action)
        assertEquals(input.expectedInteraction, interaction)
    }

    companion object {

        private const val DummyId = "userId"
        private const val DummyMessageId = "messageId"

        private val inputs = listOf(
            TestInput(
                userId = null,
                messageId = null,
                action = null,
                expectedInteraction = NotificationInteraction.NoAction
            ),
            TestInput(
                userId = DummyId,
                messageId = null,
                action = null,
                expectedInteraction = NotificationInteraction.GroupTap(DummyId)
            ),
            TestInput(
                userId = DummyId,
                messageId = DummyMessageId,
                action = null,
                expectedInteraction = NotificationInteraction.SingleTap(DummyId, DummyMessageId)
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = inputs
            .map { testInput ->
                val testName = """
                    User Id: ${testInput.userId}
                    Message Id: ${testInput.messageId}
                    Action: ${testInput.action}
                    Expected Interaction: ${testInput.expectedInteraction}
                        
                """.trimIndent()
                arrayOf(testName, testInput)
            }
    }

    data class TestInput(
        val userId: String?,
        val messageId: String?,
        val action: String?,
        val expectedInteraction: NotificationInteraction
    )
}
