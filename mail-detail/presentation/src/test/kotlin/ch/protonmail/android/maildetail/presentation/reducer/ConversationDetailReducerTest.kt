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
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMetadataUiModelSample
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertNotNull

@RunWith(Parameterized::class)
class ConversationDetailReducerTest(
    @Suppress("unused") private val testName: String,
    private val operation: ConversationDetailOperation
) {

    private val bottomBarReducer = mockk<BottomBarReducer>(relaxed = true)
    private val messagesReducer = mockk<ConversationDetailMessagesReducer>(relaxed = true)
    private val metadataReducer = mockk<ConversationDetailMetadataReducer>(relaxed = true)
    private val reducer = ConversationDetailReducer(
        bottomBarReducer = bottomBarReducer,
        messagesReducer = messagesReducer,
        metadataReducer = metadataReducer
    )

    @Test
    fun `does call the correct sub-reducers`() {
        val nextState = reducer.newStateFrom(ConversationDetailState.Loading, operation)

        if (operation is ConversationDetailOperation.AffectingErrorBar) {
            verify { messagesReducer wasNot Called }
            verify { metadataReducer wasNot Called }
            verify { bottomBarReducer wasNot Called }
            assertNotNull(nextState.error.consume())
        }

        if (operation is ConversationDetailOperation.AffectingMessages) {
            verify { messagesReducer.newStateFrom(any(), operation) }
        } else {
            verify { messagesReducer wasNot Called }
        }

        if (operation is ConversationDetailOperation.AffectingConversation) {
            verify { metadataReducer.newStateFrom(any(), operation) }
        } else {
            verify { metadataReducer wasNot Called }
        }

        if (operation is ConversationDetailEvent.ConversationBottomBarEvent) {
            verify { bottomBarReducer.newStateFrom(any(), operation.bottomBarEvent) }
        } else {
            verify { bottomBarReducer wasNot Called }
        }
    }

    private companion object {

        private val operations = listOf(
            ConversationDetailEvent.ConversationBottomBarEvent(BottomBarEvent.ErrorLoadingActions),
            ConversationDetailEvent.ConversationData(ConversationDetailMetadataUiModelSample.WeatherForecast),
            ConversationDetailEvent.ErrorLoadingContacts,
            ConversationDetailEvent.ErrorLoadingConversation,
            ConversationDetailEvent.ErrorLoadingMessages,
            ConversationDetailEvent.MessagesData(emptyList()),
            ConversationDetailViewAction.Star,
            ConversationDetailViewAction.UnStar,
            ConversationDetailEvent.ErrorAddStar,
            ConversationDetailEvent.ErrorRemoveStar
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = operations
            .map { operation ->
                val testName = "Operation: $operation"
                arrayOf(testName, operation)
            }
    }
}
