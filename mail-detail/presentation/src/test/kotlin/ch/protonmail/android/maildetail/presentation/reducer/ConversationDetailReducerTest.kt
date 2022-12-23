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

import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.maildetail.presentation.R.string
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMetadataUiModelSample
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.toMailLabelSystem
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(Parameterized::class)
class ConversationDetailReducerTest(
    @Suppress("unused") private val testName: String,
    private val testInput: TestInput
) {

    private val bottomBarReducer = mockk<BottomBarReducer>(relaxed = true)
    private val messagesReducer = mockk<ConversationDetailMessagesReducer>(relaxed = true)
    private val metadataReducer = mockk<ConversationDetailMetadataReducer>(relaxed = true)
    private val bottomSheetReducer = mockk<BottomSheetReducer>(relaxed = true)
    private val reducer = ConversationDetailReducer(
        bottomBarReducer = bottomBarReducer,
        messagesReducer = messagesReducer,
        metadataReducer = metadataReducer,
        bottomSheetReducer = bottomSheetReducer
    )

    @Test
    fun `does call the correct sub-reducers`() {
        with(testInput) {
            val result = reducer.newStateFrom(ConversationDetailState.Loading, operation)

            if (reducesMessages) {
                verify { messagesReducer.newStateFrom(any(), operationAffectingMessages()) }
            } else {
                verify { messagesReducer wasNot Called }
            }

            if (reducesConversation) {
                verify { metadataReducer.newStateFrom(any(), operationAffectingConversation()) }
            } else {
                verify { metadataReducer wasNot Called }
            }

            if (reducesBottomBar) {
                verify { bottomBarReducer.newStateFrom(any(), operationAffectingBottomBar().bottomBarEvent) }
            } else {
                verify { bottomBarReducer wasNot Called }
            }

            if (reducesBottomSheet) {
                verify { bottomSheetReducer.newStateFrom(any(), any()) }
            } else {
                verify { bottomSheetReducer wasNot Called }
            }

            if (reducesErrorBar) {
                assertNotNull(result.error.consume())
            } else {
                assertNull(result.error.consume())
            }

            if (reducesExit) {
                assertNotNull(result.exitScreenEffect.consume())
            } else {
                assertNull(result.exitScreenEffect.consume())
            }

            if (expectedExitMessage != null) {
                assertEquals(expectedExitMessage, result.exitScreenWithMessageEffect.consume())
            }
        }
    }

    data class TestInput(
        val operation: ConversationDetailOperation,
        val reducesConversation: Boolean,
        val reducesMessages: Boolean,
        val reducesBottomBar: Boolean,
        val reducesErrorBar: Boolean,
        val reducesExit: Boolean,
        val expectedExitMessage: TextUiModel?,
        val reducesBottomSheet: Boolean
    ) {

        fun operationAffectingBottomBar() = operation as ConversationDetailEvent.ConversationBottomBarEvent
        fun operationAffectingConversation() = operation as ConversationDetailOperation.AffectingConversation
        fun operationAffectingMessages() = operation as ConversationDetailOperation.AffectingMessages
    }

    private companion object {

        val actions = listOf(
            ConversationDetailViewAction.Star affects Conversation,
            ConversationDetailViewAction.Trash affects ExitWithMessage(TextUiModel(string.conversation_moved_to_trash)),
            ConversationDetailViewAction.UnStar affects Conversation,
            ConversationDetailViewAction.MoveToDestinationSelected(
                SystemLabelId.Archive.toMailLabelSystem().id
            ) affects BottomSheet,
            ConversationDetailViewAction.MoveToDestinationConfirmed("spam") affects ExitWithMessage(
                TextUiModel(string.conversation_moved_to_selected_destination, "spam")
            )
        )

        val events = listOf(
            ConversationDetailEvent.ConversationBottomBarEvent(BottomBarEvent.ErrorLoadingActions) affects BottomBar,
            ConversationDetailEvent.ConversationData(ConversationDetailMetadataUiModelSample.WeatherForecast)
                affects Conversation,
            ConversationDetailEvent.ErrorAddStar affects ErrorBar,
            ConversationDetailEvent.ErrorRemoveStar affects ErrorBar,
            ConversationDetailEvent.ErrorLoadingConversation affects Conversation,
            ConversationDetailEvent.ErrorLoadingMessages affects Messages,
            ConversationDetailEvent.ErrorMovingToTrash affects ErrorBar,
            ConversationDetailEvent.MessagesData(emptyList()) affects Messages,
            ConversationDetailEvent.ErrorMovingConversation affects ErrorBar
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = (actions + events)
            .map { operation ->
                val testName = "Operation: $operation"
                arrayOf(testName, operation)
            }
    }
}

private infix fun ConversationDetailOperation.affects(entity: Entity) = ConversationDetailReducerTest.TestInput(
    operation = this,
    reducesConversation = entity == Conversation,
    reducesMessages = entity == Messages,
    reducesBottomBar = entity == BottomBar,
    reducesErrorBar = entity == ErrorBar,
    reducesExit = entity == Exit,
    expectedExitMessage = (entity as? ExitWithMessage)?.message,
    reducesBottomSheet = entity == BottomSheet
)

private sealed interface Entity
private object Messages : Entity
private object Conversation : Entity
private object BottomBar : Entity
private object Exit : Entity
private data class ExitWithMessage(val message: TextUiModel) : Entity
private object ErrorBar : Entity
private object BottomSheet : Entity
