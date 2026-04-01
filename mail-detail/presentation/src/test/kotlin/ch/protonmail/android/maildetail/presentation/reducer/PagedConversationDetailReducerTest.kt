/*
 * Copyright (c) 2025 Proton Technologies AG
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

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.CursorId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailconversation.domain.entity.ConversationDetailEntryPoint
import ch.protonmail.android.maildetail.presentation.model.DynamicViewPagerState
import ch.protonmail.android.maildetail.presentation.model.NavigationArgs
import ch.protonmail.android.maildetail.presentation.model.Page
import ch.protonmail.android.maildetail.presentation.model.PagedConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.PagedConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.PagerSettings
import ch.protonmail.android.maillabel.domain.model.LabelId
import kotlinx.collections.immutable.toImmutableList
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class PagedConversationDetailReducerTest(
    private val testName: String,
    private val testInput: TestParams.TestInput
) {

    private val pagedConversationDetailReducer = PagedConversationDetailReducer()

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        val actualState = pagedConversationDetailReducer.newStateFrom(testInput.currentState, testInput.event)

        assertEquals(testInput.expectedState, actualState, testName)
    }

    companion object {

        private val readyState = PagedConversationDetailState.Ready(
            settings = PagerSettings(
                swipeEnabled = true,
                autoAdvanceEnabled = true
            ),
            dynamicViewPagerState = DynamicViewPagerState(
                currentPageIndex = 1,
                focusPageIndex = 1,
                pages = listOf(
                    Page.Conversation(CursorId(ConversationId("400"))),
                    Page.Conversation(CursorId(ConversationId("300"), null)),
                    Page.Conversation(CursorId(ConversationId("200")))
                ).toImmutableList()
            ),
            navigationArgs = NavigationArgs(
                singleMessageMode = false,
                LabelId("1"),
                ConversationDetailEntryPoint.Mailbox
            )
        )

        private val transitionsFromLoadingState = listOf(
            TestParams(
                "from loading to conversation data with swipe enabled",
                TestParams.TestInput(
                    currentState = PagedConversationDetailState.Loading,
                    event = PagedConversationDetailEvent.Ready(
                        pagerSettings = PagerSettings(
                            swipeEnabled = true,
                            autoAdvanceEnabled = true
                        ),
                        currentItem = Page.Conversation(CursorId(ConversationId("300"))),
                        nextItem = Page.Conversation(CursorId(ConversationId("400"))),
                        previousItem = Page.Conversation(CursorId(ConversationId("200"))),
                        navigationArgs = NavigationArgs(
                            singleMessageMode = false,
                            LabelId("1"),
                            ConversationDetailEntryPoint.Mailbox
                        )
                    ),
                    expectedState = PagedConversationDetailState.Ready(
                        settings = PagerSettings(
                            swipeEnabled = true,
                            autoAdvanceEnabled = true
                        ),
                        dynamicViewPagerState = DynamicViewPagerState(
                            currentPageIndex = 1,
                            focusPageIndex = 1,
                            pages = listOf(
                                Page.Conversation(CursorId(ConversationId("200"))),
                                Page.Conversation(CursorId(ConversationId("300"))),
                                Page.Conversation(CursorId(ConversationId("400")))
                            ).toImmutableList()
                        ),
                        navigationArgs = NavigationArgs(
                            singleMessageMode = false,
                            LabelId("1"),
                            ConversationDetailEntryPoint.Mailbox
                        )
                    )
                )
            ),
            TestParams(
                "from loading to conversation data with swipe disabled",
                TestParams.TestInput(
                    currentState = PagedConversationDetailState.Loading,
                    event = PagedConversationDetailEvent.Ready(
                        pagerSettings = PagerSettings(
                            swipeEnabled = false,
                            autoAdvanceEnabled = true
                        ),
                        currentItem = Page.Conversation(CursorId(ConversationId("300"))),
                        nextItem = Page.Conversation(CursorId(ConversationId("400"))),
                        previousItem = Page.Conversation(CursorId(ConversationId("200"))),
                        navigationArgs = NavigationArgs(
                            singleMessageMode = false,
                            LabelId("1"),
                            ConversationDetailEntryPoint.Mailbox
                        )
                    ),
                    expectedState = PagedConversationDetailState.Ready(
                        settings = PagerSettings(
                            swipeEnabled = false,
                            autoAdvanceEnabled = true
                        ),
                        dynamicViewPagerState = DynamicViewPagerState(
                            currentPageIndex = 1,
                            focusPageIndex = 1,
                            pages = listOf(
                                Page.Conversation(CursorId(ConversationId("200"))),
                                Page.Conversation(CursorId(ConversationId("300"))),
                                Page.Conversation(CursorId(ConversationId("400")))
                            ).toImmutableList()
                        ),
                        navigationArgs = NavigationArgs(
                            singleMessageMode = false,
                            LabelId("1"),
                            ConversationDetailEntryPoint.Mailbox
                        )
                    )
                )
            )
        )
        private val transitionsFromDataState = listOf(
            TestParams(
                "on update page",
                TestParams.TestInput(
                    currentState = readyState,
                    event = PagedConversationDetailEvent.UpdatePage(
                        currentItem = Page.Conversation(CursorId(ConversationId("500"))),
                        nextItem = Page.Conversation(CursorId(ConversationId("600"))),
                        previousItem = Page.Conversation(CursorId(ConversationId("900")))
                    ),
                    expectedState =
                    readyState.copy(
                        dynamicViewPagerState = readyState.dynamicViewPagerState.copy(
                            pages = listOf(
                                Page.Conversation(CursorId(ConversationId("900"))),
                                Page.Conversation(CursorId(ConversationId("500"))),
                                Page.Conversation(CursorId(ConversationId("600")))
                            ).toImmutableList(),
                            currentPageIndex = 1,
                            focusPageIndex = 1
                        )
                    )
                )
            ),
            TestParams(
                "on update page with page start End",
                TestParams.TestInput(
                    currentState = readyState,
                    event = PagedConversationDetailEvent.UpdatePage(
                        currentItem = Page.Conversation(CursorId(ConversationId("500"))),
                        nextItem = Page.Conversation(CursorId(ConversationId("600"))),
                        previousItem = Page.End
                    ),
                    expectedState =
                    readyState.copy(
                        dynamicViewPagerState = readyState.dynamicViewPagerState.copy(
                            pages = listOf(
                                Page.Conversation(CursorId(ConversationId("500"))),
                                Page.Conversation(CursorId(ConversationId("600")))
                            ).toImmutableList(),
                            currentPageIndex = 0,
                            focusPageIndex = 0
                        )
                    )
                )
            ),
            TestParams(
                "on update page with page finish End",
                TestParams.TestInput(
                    currentState = readyState,
                    event = PagedConversationDetailEvent.UpdatePage(
                        currentItem = Page.Conversation(CursorId(ConversationId("500"))),
                        nextItem = Page.End,
                        previousItem = Page.Conversation(CursorId(ConversationId("600")))
                    ),
                    expectedState =
                    readyState.copy(
                        dynamicViewPagerState = readyState.dynamicViewPagerState.copy(
                            pages = listOf(
                                Page.Conversation(CursorId(ConversationId("600"))),
                                Page.Conversation(CursorId(ConversationId("500")))
                            ).toImmutableList(),
                            currentPageIndex = 1,
                            focusPageIndex = 1
                        )
                    )
                )
            ),
            TestParams(
                "on clear focus page",
                TestParams.TestInput(
                    currentState = readyState,
                    event = PagedConversationDetailEvent.ClearFocusPage,
                    expectedState =
                    readyState.copy(
                        dynamicViewPagerState = readyState.dynamicViewPagerState.copy(
                            focusPageIndex = null
                        )
                    )
                )
            ),
            TestParams(
                "on auto advance",
                TestParams.TestInput(
                    currentState = readyState,
                    event = PagedConversationDetailEvent.AutoAdvanceRequested,
                    expectedState =
                    readyState.copy(
                        dynamicViewPagerState = readyState.dynamicViewPagerState.copy(
                            scrollToPage = Effect.of(Unit),
                            pendingRemoval = Page.Conversation(CursorId(ConversationId("300"))),
                            userScrollEnabled = false
                        )
                    )
                )
            ),
            TestParams(
                "on auto advance no next item",
                TestParams.TestInput(
                    currentState = readyState.copy(
                        dynamicViewPagerState = readyState.dynamicViewPagerState.copy(
                            pages = listOf(
                                Page.Conversation(CursorId(ConversationId("600"))),
                                Page.Conversation(CursorId(ConversationId("500")))
                            ).toImmutableList(),
                            currentPageIndex = 1
                        )
                    ),
                    event = PagedConversationDetailEvent.AutoAdvanceRequested,
                    expectedState =
                    readyState.copy(
                        dynamicViewPagerState = readyState.dynamicViewPagerState.copy(
                            pages = listOf(
                                Page.Conversation(CursorId(ConversationId("600"))),
                                Page.Conversation(CursorId(ConversationId("500")))
                            ).toImmutableList(),
                            currentPageIndex = 1,
                            exit = Effect.of(Unit)
                        )
                    )
                )
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = (transitionsFromLoadingState + transitionsFromDataState)
            .map { arrayOf(it.testName, it.testInput) }
    }

    data class TestParams(
        val testName: String,
        val testInput: TestInput
    ) {

        data class TestInput(
            val currentState: PagedConversationDetailState,
            val event: PagedConversationDetailEvent,
            val expectedState: PagedConversationDetailState
        )
    }
}
