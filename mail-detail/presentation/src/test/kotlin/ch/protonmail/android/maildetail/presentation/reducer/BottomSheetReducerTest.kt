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

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.presentation.MailLabelUiModel
import ch.protonmail.android.maillabel.presentation.model.LabelUiModelWithSelectedState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetOperation
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetVisibilityEffect
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.DetailMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MailboxMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MailboxUpsellingEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.UpsellingBottomSheetState
import ch.protonmail.android.mailmessage.presentation.reducer.BottomSheetReducer
import ch.protonmail.android.mailmessage.presentation.reducer.ContactActionsBottomSheetReducer
import ch.protonmail.android.mailmessage.presentation.reducer.DetailMoreActionsBottomSheetReducer
import ch.protonmail.android.mailmessage.presentation.reducer.LabelAsBottomSheetReducer
import ch.protonmail.android.mailmessage.presentation.reducer.MailboxMoreActionsBottomSheetReducer
import ch.protonmail.android.mailmessage.presentation.reducer.MoveToBottomSheetReducer
import ch.protonmail.android.mailmessage.presentation.reducer.UpsellingBottomSheetReducer
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.toImmutableList
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class BottomSheetReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val moveToBottomSheetReducer: MoveToBottomSheetReducer = mockk(relaxed = true)
    private val labelAsBottomSheetReducer: LabelAsBottomSheetReducer = mockk(relaxed = true)
    private val mailboxMoreActionsBottomSheetReducer: MailboxMoreActionsBottomSheetReducer = mockk(relaxed = true)
    private val detailMoreActionsBottomSheetReducer: DetailMoreActionsBottomSheetReducer = mockk(relaxed = true)
    private val upsellingBottomSheetReducer: UpsellingBottomSheetReducer = mockk(relaxed = true)
    private val contactActionsBottomSheetReducer: ContactActionsBottomSheetReducer = mockk(relaxed = true)
    private val reducer = BottomSheetReducer(
        moveToBottomSheetReducer,
        labelAsBottomSheetReducer,
        mailboxMoreActionsBottomSheetReducer,
        detailMoreActionsBottomSheetReducer,
        contactActionsBottomSheetReducer,
        upsellingBottomSheetReducer
    )

    @Test
    fun `should produce the expected new bottom sheet state`() = with(testInput) {
        val actualState = reducer.newStateFrom(currentState, operation)

        if (reducesBottomSheetVisibilityEffects) {
            assertEquals(expectedState, actualState, testName)
        }

        if (reducesMoveTo) {
            verify {
                moveToBottomSheetReducer.newStateFrom(
                    currentState,
                    testInput.operation as MoveToBottomSheetState.MoveToBottomSheetOperation
                )
            }
        } else {
            verify { moveToBottomSheetReducer wasNot Called }
        }

        if (reducesLabelAs) {
            verify {
                labelAsBottomSheetReducer.newStateFrom(
                    currentState,
                    testInput.operation as LabelAsBottomSheetState.LabelAsBottomSheetOperation
                )
            }
        } else {
            verify { labelAsBottomSheetReducer wasNot Called }
        }

        if (reducesMailboxMoreActions) {
            verify {
                mailboxMoreActionsBottomSheetReducer.newStateFrom(
                    currentState,
                    testInput.operation as MailboxMoreActionsBottomSheetState.MailboxMoreActionsBottomSheetOperation
                )
            }
        } else {
            verify { mailboxMoreActionsBottomSheetReducer wasNot Called }
        }

        if (reducesDetailMoreActions) {
            verify {
                detailMoreActionsBottomSheetReducer.newStateFrom(
                    currentState,
                    testInput.operation
                        as DetailMoreActionsBottomSheetState.MessageDetailMoreActionsBottomSheetOperation
                )
            }
        } else {
            verify { detailMoreActionsBottomSheetReducer wasNot Called }
        }

        if (reducesUpselling) {
            verify {
                upsellingBottomSheetReducer.newStateFrom(
                    currentState,
                    testInput.operation as UpsellingBottomSheetState.UpsellingBottomSheetOperation
                )
            }
        } else {
            verify { upsellingBottomSheetReducer wasNot Called }
        }
    }

    companion object {

        private val bottomSheetVisibilityOperations = listOf(
            TestInput(
                currentState = null,
                operation = BottomSheetOperation.Requested,
                expectedState = BottomSheetState(null, Effect.of(BottomSheetVisibilityEffect.Show)),
                reducesBottomSheetVisibilityEffects = true,
                reducesMoveTo = false,
                reducesLabelAs = false,
                reducesMailboxMoreActions = false,
                reducesDetailMoreActions = false,
                reducesUpselling = false
            ),
            TestInput(
                currentState = BottomSheetState(
                    MoveToBottomSheetState.Data(
                        listOf<MailLabelUiModel>().toImmutableList(),
                        null,
                        MoveToBottomSheetEntryPoint.SelectionMode
                    )
                ),
                operation = BottomSheetOperation.Dismiss,
                expectedState = BottomSheetState(null, Effect.of(BottomSheetVisibilityEffect.Hide)),
                reducesBottomSheetVisibilityEffects = true,
                reducesMoveTo = false,
                reducesLabelAs = false,
                reducesMailboxMoreActions = false,
                reducesDetailMoreActions = false,
                reducesUpselling = false
            )
        )

        private val moveToBottomSheetOperation = listOf(
            TestInput(
                currentState = BottomSheetState(null, Effect.empty()),
                operation = MoveToBottomSheetState.MoveToBottomSheetEvent.ActionData(
                    listOf<MailLabelUiModel>()
                        .toImmutableList(),
                    entryPoint = MoveToBottomSheetEntryPoint.SelectionMode
                ),
                expectedState = BottomSheetState(
                    MoveToBottomSheetState.Data(
                        listOf<MailLabelUiModel>()
                            .toImmutableList(),
                        null,
                        MoveToBottomSheetEntryPoint.SelectionMode
                    )
                ),
                reducesBottomSheetVisibilityEffects = false,
                reducesMoveTo = true,
                reducesLabelAs = false,
                reducesMailboxMoreActions = false,
                reducesDetailMoreActions = false,
                reducesUpselling = false
            )
        )

        private val labelAsBottomSheetOperation = listOf(
            TestInput(
                currentState = BottomSheetState(null, Effect.empty()),
                operation = LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
                    customLabelList = listOf<MailLabelUiModel.Custom>()
                        .toImmutableList(),
                    selectedLabels = listOf<LabelId>().toImmutableList(),
                    entryPoint = LabelAsBottomSheetEntryPoint.SelectionMode
                ),
                expectedState = BottomSheetState(
                    LabelAsBottomSheetState.Data(
                        listOf<LabelUiModelWithSelectedState>()
                            .toImmutableList(),
                        LabelAsBottomSheetEntryPoint.SelectionMode
                    )
                ),
                reducesBottomSheetVisibilityEffects = false,
                reducesLabelAs = true,
                reducesMoveTo = false,
                reducesMailboxMoreActions = false,
                reducesDetailMoreActions = false,
                reducesUpselling = false
            ),
            TestInput(
                currentState = BottomSheetState(
                    LabelAsBottomSheetState.Data(
                        listOf<LabelUiModelWithSelectedState>()
                            .toImmutableList(),
                        LabelAsBottomSheetEntryPoint.SelectionMode
                    )
                ),
                operation = LabelAsBottomSheetState.LabelAsBottomSheetAction.LabelToggled(LabelId("labelId")),
                expectedState = BottomSheetState(
                    LabelAsBottomSheetState.Data(
                        listOf<LabelUiModelWithSelectedState>()
                            .toImmutableList(),
                        LabelAsBottomSheetEntryPoint.SelectionMode
                    )
                ),
                reducesBottomSheetVisibilityEffects = false,
                reducesLabelAs = true,
                reducesMoveTo = false,
                reducesMailboxMoreActions = false,
                reducesDetailMoreActions = false,
                reducesUpselling = false
            )
        )

        private val mailboxMoreActionsBottomSheetOperation = listOf(
            TestInput(
                currentState = BottomSheetState(null, Effect.empty()),
                operation = MailboxMoreActionsBottomSheetState.MailboxMoreActionsBottomSheetEvent.ActionData(
                    listOf<ActionUiModel>().toImmutableList()
                ),
                expectedState = BottomSheetState(
                    MailboxMoreActionsBottomSheetState.Data(
                        listOf<ActionUiModel>().toImmutableList()
                    )
                ),
                reducesBottomSheetVisibilityEffects = false,
                reducesLabelAs = false,
                reducesMoveTo = false,
                reducesMailboxMoreActions = true,
                reducesDetailMoreActions = false,
                reducesUpselling = false
            )
        )

        private val detailMoreActionsBottomSheetOperation = listOf(
            TestInput(
                currentState = BottomSheetState(null, Effect.empty()),
                operation = DetailMoreActionsBottomSheetState.MessageDetailMoreActionsBottomSheetEvent.DataLoaded(
                    affectingConversation = false,
                    messageSender = "Sender",
                    messageSubject = "Subject",
                    messageId = "messageId",
                    participantsCount = 1,
                    actions = emptyList()
                ),
                expectedState = BottomSheetState(
                    DetailMoreActionsBottomSheetState.Data(
                        isAffectingConversation = false,
                        messageDataUiModel = DetailMoreActionsBottomSheetState.MessageDataUiModel(
                            headerDescriptionText = TextUiModel("Sender"),
                            headerSubjectText = TextUiModel("Subject"),
                            messageId = "messageId"
                        ),
                        replyActionsUiModel = emptyList<ActionUiModel>().toImmutableList()
                    )
                ),
                reducesBottomSheetVisibilityEffects = false,
                reducesLabelAs = false,
                reducesMoveTo = false,
                reducesMailboxMoreActions = false,
                reducesDetailMoreActions = true,
                reducesUpselling = false
            )
        )

        private val upsellingBottomSheetOperation = listOf(
            TestInput(
                currentState = BottomSheetState(null, Effect.empty()),
                operation = UpsellingBottomSheetState.UpsellingBottomSheetEvent.Ready(
                    MailboxUpsellingEntryPoint.Mailbox
                ),
                expectedState = BottomSheetState(
                    UpsellingBottomSheetState.Requested(MailboxUpsellingEntryPoint.Mailbox)
                ),
                reducesBottomSheetVisibilityEffects = false,
                reducesLabelAs = false,
                reducesMoveTo = false,
                reducesMailboxMoreActions = false,
                reducesDetailMoreActions = false,
                reducesUpselling = true
            ),
            TestInput(
                currentState = BottomSheetState(null, Effect.empty()),
                operation = UpsellingBottomSheetState.UpsellingBottomSheetEvent.Ready(
                    MailboxUpsellingEntryPoint.AutoDelete
                ),
                expectedState = BottomSheetState(
                    UpsellingBottomSheetState.Requested(MailboxUpsellingEntryPoint.AutoDelete)
                ),
                reducesBottomSheetVisibilityEffects = false,
                reducesLabelAs = false,
                reducesMoveTo = false,
                reducesMailboxMoreActions = false,
                reducesDetailMoreActions = false,
                reducesUpselling = true
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = (
            bottomSheetVisibilityOperations +
                moveToBottomSheetOperation +
                labelAsBottomSheetOperation +
                mailboxMoreActionsBottomSheetOperation +
                detailMoreActionsBottomSheetOperation +
                upsellingBottomSheetOperation
            )
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
        val currentState: BottomSheetState?,
        val operation: BottomSheetOperation,
        val expectedState: BottomSheetState?,
        val reducesBottomSheetVisibilityEffects: Boolean,
        val reducesMoveTo: Boolean,
        val reducesLabelAs: Boolean,
        val reducesMailboxMoreActions: Boolean,
        val reducesDetailMoreActions: Boolean,
        val reducesUpselling: Boolean
    )
}
