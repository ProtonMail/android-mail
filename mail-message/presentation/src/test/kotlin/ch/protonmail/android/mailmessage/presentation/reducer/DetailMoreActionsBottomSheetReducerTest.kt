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

package ch.protonmail.android.mailmessage.presentation.reducer

import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.sample.ActionUiModelSample
import ch.protonmail.android.mailmessage.presentation.mapper.DetailMoreActionsBottomSheetUiMapper
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.DetailMoreActionsBottomSheetState
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.collections.immutable.toImmutableList
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class DetailMoreActionsBottomSheetReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val mapper = mockk<DetailMoreActionsBottomSheetUiMapper> {
        every { this@mockk.mapMoreActionUiModels(any()) } returns expectedMultipleParticipantAction.toImmutableList()
    }
    private val reducer = DetailMoreActionsBottomSheetReducer(mapper)

    @Before
    fun setup() {
        every {
            mapper.mapMoreActionUiModels(
                listOf(
                    Action.Forward, Action.ReportPhishing,
                    Action.OpenCustomizeToolbar
                )
            )
        } returns expectedMultipleParticipantAction

        every {
            mapper.toHeaderUiModel(ExpectedSender, ExpectedSubject, ExpectedMessageId)
        } returns expectedUiModel
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should produce the expected new bottom sheet state`() = with(testInput) {
        val actualState = reducer.newStateFrom(currentState, operation)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private const val ExpectedSender = "Sender"
        private const val ExpectedSubject = "Subject"
        private const val ExpectedMessageId = "messageId"
        private const val SingleParticipantCount = 1
        private const val MultipleParticipantsCount = 10

        private val expectedUiModel = DetailMoreActionsBottomSheetState.MessageDataUiModel(
            headerSubjectText = TextUiModel(ExpectedSubject),
            headerDescriptionText = TextUiModel(ExpectedSender),
            messageId = ExpectedMessageId
        )
        private val expectedSingleParticipantAction =
            listOf(
                ActionUiModelSample.Reply, ActionUiModelSample.ReportPhishing,
                ActionUiModelSample.CustomizeToolbar
            ).toImmutableList()
        private val expectedMultipleParticipantAction =
            listOf(
                ActionUiModelSample.Forward, ActionUiModelSample.ReportPhishing,
                ActionUiModelSample.CustomizeToolbar
            ).toImmutableList()

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = BottomSheetState(DetailMoreActionsBottomSheetState.Loading),
                operation = DetailMoreActionsBottomSheetState.MessageDetailMoreActionsBottomSheetEvent.DataLoaded(
                    affectingConversation = false,
                    messageSender = ExpectedSender,
                    messageSubject = ExpectedSubject,
                    messageId = ExpectedMessageId,
                    participantsCount = MultipleParticipantsCount,
                    actions = emptyList()
                ),
                expectedState = BottomSheetState(
                    contentState = DetailMoreActionsBottomSheetState.Data(
                        isAffectingConversation = false,
                        messageDataUiModel = expectedUiModel,
                        replyActionsUiModel = expectedSingleParticipantAction
                    )
                )
            ),
            TestInput(
                currentState = BottomSheetState(DetailMoreActionsBottomSheetState.Loading),
                operation = DetailMoreActionsBottomSheetState.MessageDetailMoreActionsBottomSheetEvent.DataLoaded(
                    affectingConversation = false,
                    messageSender = ExpectedSender,
                    messageSubject = ExpectedSubject,
                    messageId = ExpectedMessageId,
                    participantsCount = SingleParticipantCount,
                    actions = emptyList()
                ),
                expectedState = BottomSheetState(
                    contentState = DetailMoreActionsBottomSheetState.Data(
                        isAffectingConversation = false,
                        messageDataUiModel = expectedUiModel,
                        replyActionsUiModel = expectedMultipleParticipantAction
                    )
                )
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = transitionsFromLoadingState
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
        val currentState: BottomSheetState,
        val operation: DetailMoreActionsBottomSheetState.MessageDetailMoreActionsBottomSheetOperation,
        val expectedState: BottomSheetState
    )
}
