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
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.presentation.R.string
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailsMessagesState
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyWithType
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailmessage.presentation.usecase.InjectCssIntoDecryptedMessageBody
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.toImmutableList
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals
import ch.protonmail.android.mailcommon.presentation.R.string as commonString

@RunWith(Parameterized::class)
class ConversationDetailMessagesReducerTest(
    private val testName: String,
    private val input: Input
) {

    private val injectCssIntoDecryptedMessageBody = mockk<InjectCssIntoDecryptedMessageBody> {
        every {
            this@mockk.invoke(
                MessageBodyWithType(
                    ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded.messageBodyUiModel.messageBody,
                    ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded.messageBodyUiModel.mimeType
                ),
                ViewModePreference.LightMode
            )
        } returns ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded.messageBodyUiModel.messageBody
    }

    @Test
    fun test() {
        val reducer = ConversationDetailMessagesReducer(injectCssIntoDecryptedMessageBody)
        val result = reducer.newStateFrom(input.currentState, input.operation)
        assertEquals(input.expectedState, result, testName)
    }

    data class Input(
        val currentState: ConversationDetailsMessagesState,
        val operation: ConversationDetailOperation.AffectingMessages,
        val expectedState: ConversationDetailsMessagesState
    )

    private companion object {

        private val allMessages = listOf(
            ConversationDetailMessageUiModelSample.AugWeatherForecast,
            ConversationDetailMessageUiModelSample.SepWeatherForecast
        ).toImmutableList()

        private val allMessagesFirstExpanded = listOf(
            ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded,
            ConversationDetailMessageUiModelSample.SepWeatherForecast
        ).toImmutableList()

        private val allMessagesFirstExpanding = listOf(
            ConversationDetailMessageUiModelSample.AugWeatherForecastExpanding,
            ConversationDetailMessageUiModelSample.SepWeatherForecast
        ).toImmutableList()

        private val fromLoadingState = listOf(

            Input(
                currentState = ConversationDetailsMessagesState.Loading,
                operation = ConversationDetailEvent.ErrorLoadingContacts,
                expectedState = ConversationDetailsMessagesState.Error(
                    message = TextUiModel(string.detail_error_loading_contacts)
                )
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Loading,
                operation = ConversationDetailEvent.ErrorLoadingMessages,
                expectedState = ConversationDetailsMessagesState.Error(
                    message = TextUiModel(string.detail_error_loading_messages)
                )
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Loading,
                operation = ConversationDetailEvent.MessagesData(
                    messagesUiModels = allMessages,
                    messagesLabelIds = emptyMap(),
                    requestScrollToMessageId = null,
                    filterByLocation = null,
                    shouldHideMessagesBasedOnTrashFilter = false
                ),
                expectedState = ConversationDetailsMessagesState.Data(messages = allMessages)
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Loading,
                operation = ConversationDetailEvent.NoNetworkError,
                expectedState = ConversationDetailsMessagesState.Offline
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Loading,
                operation = ConversationDetailEvent.ErrorLoadingConversation,
                expectedState = ConversationDetailsMessagesState.Error(
                    message = TextUiModel(string.detail_error_loading_messages)
                )
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Loading,
                operation = ConversationDetailEvent.MessagesData(
                    messagesUiModels = allMessagesFirstExpanded,
                    messagesLabelIds = emptyMap(),
                    requestScrollToMessageId = allMessagesFirstExpanded.first().messageId,
                    filterByLocation = null,
                    shouldHideMessagesBasedOnTrashFilter = false
                ),
                expectedState = ConversationDetailsMessagesState.Data(messages = allMessagesFirstExpanded)
            )
        ).toImmutableList()

        private val fromErrorState = listOf(

            Input(
                currentState = ConversationDetailsMessagesState.Error(
                    message = TextUiModel(commonString.x_error_not_logged_in)
                ),
                operation = ConversationDetailEvent.ErrorLoadingContacts,
                expectedState = ConversationDetailsMessagesState.Error(
                    message = TextUiModel(string.detail_error_loading_contacts)
                )
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Error(
                    message = TextUiModel(commonString.x_error_not_logged_in)
                ),
                operation = ConversationDetailEvent.ErrorLoadingMessages,
                expectedState = ConversationDetailsMessagesState.Error(
                    message = TextUiModel(string.detail_error_loading_messages)
                )
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Error(
                    message = TextUiModel(commonString.x_error_not_logged_in)
                ),
                operation = ConversationDetailEvent.MessagesData(
                    messagesUiModels = allMessages,
                    messagesLabelIds = emptyMap(),
                    requestScrollToMessageId = null,
                    filterByLocation = null,
                    shouldHideMessagesBasedOnTrashFilter = false
                ),
                expectedState = ConversationDetailsMessagesState.Data(messages = allMessages)
            )
        )

        private val fromSuccessState = listOf(

            Input(
                currentState = ConversationDetailsMessagesState.Data(
                    messages = emptyList<ConversationDetailMessageUiModel>().toImmutableList()
                ),
                operation = ConversationDetailEvent.ErrorLoadingContacts,
                expectedState = ConversationDetailsMessagesState.Error(
                    message = TextUiModel(string.detail_error_loading_contacts)
                )
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Data(
                    messages = emptyList<ConversationDetailMessageUiModel>().toImmutableList()
                ),
                operation = ConversationDetailEvent.ErrorLoadingMessages,
                expectedState = ConversationDetailsMessagesState.Error(
                    message = TextUiModel(string.detail_error_loading_messages)
                )
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Data(
                    messages = emptyList<ConversationDetailMessageUiModel>().toImmutableList()
                ),
                operation = ConversationDetailEvent.MessagesData(
                    messagesUiModels = allMessages,
                    messagesLabelIds = emptyMap(),
                    requestScrollToMessageId = null,
                    filterByLocation = null,
                    shouldHideMessagesBasedOnTrashFilter = false
                ),
                expectedState = ConversationDetailsMessagesState.Data(messages = allMessages)
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Data(messages = allMessages),
                operation = ConversationDetailEvent.NoNetworkError,
                expectedState = ConversationDetailsMessagesState.Data(messages = allMessages)
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Data(messages = allMessages),
                operation = ConversationDetailEvent.ErrorLoadingConversation,
                expectedState = ConversationDetailsMessagesState.Data(messages = allMessages)
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Data(messages = allMessages),
                operation = ConversationDetailEvent.ExpandDecryptedMessage(
                    messageId = allMessages.first().messageId,
                    conversationDetailMessageUiModel = ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded
                ),
                expectedState = ConversationDetailsMessagesState.Data(messages = allMessagesFirstExpanded)
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Data(messages = allMessagesFirstExpanded),
                operation = ConversationDetailEvent.CollapseDecryptedMessage(
                    messageId = allMessagesFirstExpanded.first().messageId,
                    conversationDetailMessageUiModel = ConversationDetailMessageUiModelSample.AugWeatherForecast
                ),
                expectedState = ConversationDetailsMessagesState.Data(messages = allMessages)
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Data(messages = allMessages),
                operation = ConversationDetailEvent.ExpandingMessage(
                    allMessages.first().messageId,
                    ConversationDetailMessageUiModelSample.AugWeatherForecast
                ),
                expectedState = ConversationDetailsMessagesState.Data(messages = allMessagesFirstExpanding)
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Data(messages = allMessagesFirstExpanding),
                operation = ConversationDetailEvent.ErrorExpandingRetrievingMessageOffline(
                    allMessages.first().messageId
                ),
                expectedState = ConversationDetailsMessagesState.Data(messages = allMessages)
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Data(messages = allMessagesFirstExpanding),
                operation = ConversationDetailEvent.ErrorExpandingRetrieveMessageError(
                    allMessages.first().messageId
                ),
                expectedState = ConversationDetailsMessagesState.Data(messages = allMessages)
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Data(messages = allMessagesFirstExpanding),
                operation = ConversationDetailEvent.ErrorExpandingDecryptMessageError(
                    allMessages.first().messageId
                ),
                expectedState = ConversationDetailsMessagesState.Data(messages = allMessages)
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Data(
                    messages = listOf(
                        ConversationDetailMessageUiModelSample.invoiceExpandedWithAttachments(3),
                        ConversationDetailMessageUiModelSample.SepWeatherForecast
                    ).toImmutableList()
                ),
                operation = ConversationDetailEvent.ShowAllAttachmentsForMessage(
                    messageId = ConversationDetailMessageUiModelSample.invoiceExpandedWithAttachments(3).messageId,
                    conversationDetailMessageUiModel =
                    ConversationDetailMessageUiModelSample.invoiceExpandedWithAttachments(4)
                ),
                expectedState = ConversationDetailsMessagesState.Data(
                    messages = listOf(
                        ConversationDetailMessageUiModelSample.invoiceExpandedWithAttachments(4),
                        ConversationDetailMessageUiModelSample.SepWeatherForecast
                    ).toImmutableList()
                )
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Data(
                    messages = listOf(
                        ConversationDetailMessageUiModelSample.MessageWithRemoteContentBlocked
                    ).toImmutableList()
                ),
                operation = ConversationDetailViewAction.LoadRemoteContent(
                    messageId = ConversationDetailMessageUiModelSample.MessageWithRemoteContentBlocked.messageId
                ),
                expectedState = ConversationDetailsMessagesState.Data(
                    messages = listOf(
                        ConversationDetailMessageUiModelSample.MessageWithRemoteContentLoaded
                    ).toImmutableList()
                )
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Data(
                    messages = listOf(
                        ConversationDetailMessageUiModelSample.MessageWithEmbeddedImagesBlocked
                    ).toImmutableList()
                ),
                operation = ConversationDetailViewAction.ShowEmbeddedImages(
                    messageId = ConversationDetailMessageUiModelSample.MessageWithEmbeddedImagesBlocked.messageId
                ),
                expectedState = ConversationDetailsMessagesState.Data(
                    messages = listOf(
                        ConversationDetailMessageUiModelSample.MessageWithEmbeddedImagesLoaded
                    ).toImmutableList()
                )
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Data(
                    messages = listOf(
                        ConversationDetailMessageUiModelSample.WithRemoteAndEmbeddedContentBlocked
                    ).toImmutableList()
                ),
                operation = ConversationDetailViewAction.LoadRemoteAndEmbeddedContent(
                    ConversationDetailMessageUiModelSample.WithRemoteAndEmbeddedContentBlocked.messageId
                ),
                expectedState = ConversationDetailsMessagesState.Data(
                    messages = listOf(
                        ConversationDetailMessageUiModelSample.WithRemoteAndEmbeddedContentLoaded
                    ).toImmutableList()
                )
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Data(
                    messages = listOf(
                        ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded
                    ).toImmutableList()
                ),
                operation = ConversationDetailViewAction.SwitchViewMode(
                    MessageIdSample.AugWeatherForecast, ViewModePreference.LightMode
                ),
                expectedState = ConversationDetailsMessagesState.Data(
                    messages = listOf(
                        ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded.copy(
                            messageBodyUiModel =
                            ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded.messageBodyUiModel.copy(
                                viewModePreference = ViewModePreference.LightMode
                            )
                        )
                    ).toImmutableList()
                )
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Data(
                    messages = listOf(
                        ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded
                    ).toImmutableList()
                ),
                operation = ConversationDetailViewAction.PrintRequested(
                    MessageIdSample.AugWeatherForecast
                ),
                expectedState = ConversationDetailsMessagesState.Data(
                    messages = listOf(
                        ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded.copy(
                            messageBodyUiModel =
                            ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded.messageBodyUiModel.copy(
                                printEffect = Effect.of(Unit)
                            )
                        )
                    ).toImmutableList()
                )
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = (fromLoadingState + fromErrorState + fromSuccessState)
            .map { testInput ->
                val testName = """
                    Current state: ${testInput.currentState}
                    Operation: ${testInput.operation}
                    Next state: ${testInput.expectedState}
                    
                """.trimIndent()
                arrayOf(testName, testInput)
            }
    }
}
