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

package ch.protonmail.android.mailcomposer.presentation.mapper.modifications

import ch.protonmail.android.mailcomposer.domain.model.QuotedHtmlContent
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.presentation.model.ComposerState
import ch.protonmail.android.mailcomposer.presentation.model.SenderUiModel
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.MainStateModification
import ch.protonmail.android.mailmessage.domain.model.MessageId
import io.mockk.mockk
import kotlinx.collections.immutable.toImmutableList
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class MainStateModificationTest(
    @Suppress("unused") private val testName: String,
    private val initialState: ComposerState.Main,
    private val modification: MainStateModification,
    private val expectedState: ComposerState.Main
) {

    @Test
    fun `should apply the modification`() {
        val updatedState = modification.apply(initialState)
        assertEquals(expectedState, updatedState)
    }

    companion object {

        private val initialState = ComposerState.Main.initial(draftId = MessageId("1234"))
        private val quotedHtmlContent = mockk<QuotedHtmlContent>()

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(
                "set sender and quoted content with no restricted height from initial state",
                initialState,
                MainStateModification.OnDraftReady(
                    sender = "test@example.com",
                    quotedHtmlContent = quotedHtmlContent,
                    shouldRestrictWebViewHeight = false
                ),
                initialState.copy(
                    senderUiModel = SenderUiModel("test@example.com"),
                    quotedHtmlContent = quotedHtmlContent
                )
            ),
            arrayOf(
                "set sender and quoted content with restricted height from initial state",
                initialState,
                MainStateModification.OnDraftReady(
                    sender = "test@example.com",
                    quotedHtmlContent = quotedHtmlContent,
                    shouldRestrictWebViewHeight = true
                ),
                initialState.copy(
                    senderUiModel = SenderUiModel("test@example.com"),
                    quotedHtmlContent = quotedHtmlContent,
                    shouldRestrictWebViewHeight = true
                )
            ),
            arrayOf(
                "set sender without quoted content",
                initialState,
                MainStateModification.OnDraftReady(
                    sender = "another@example.com",
                    quotedHtmlContent = null,
                    shouldRestrictWebViewHeight = false
                ),
                initialState.copy(
                    senderUiModel = SenderUiModel("another@example.com")
                )
            ),
            arrayOf(
                "update loading type (save)",
                initialState,
                MainStateModification.UpdateLoading(ComposerState.LoadingType.Initial),
                initialState.copy(loadingType = ComposerState.LoadingType.Initial)
            ),
            arrayOf(
                "update loading type (initial)",
                initialState,
                MainStateModification.UpdateLoading(ComposerState.LoadingType.Save),
                initialState.copy(loadingType = ComposerState.LoadingType.Save)
            ),
            arrayOf(
                "update loading type (save)",
                initialState,
                MainStateModification.UpdateLoading(ComposerState.LoadingType.None),
                initialState.copy(loadingType = ComposerState.LoadingType.None)
            ),
            arrayOf(
                "update sender from initial state",
                initialState,
                MainStateModification.UpdateSender(SenderEmail("newSender@example.com")),
                initialState.copy(senderUiModel = SenderUiModel("newSender@example.com"))
            ),
            arrayOf(
                "update senders list",
                initialState,
                MainStateModification.SendersListReady(
                    listOf(
                        SenderUiModel("sender1@example.com"),
                        SenderUiModel("sender2@example.com")
                    )
                ),
                initialState.copy(
                    senderAddresses = listOf(
                        SenderUiModel("sender1@example.com"),
                        SenderUiModel("sender2@example.com")
                    ).toImmutableList()
                )
            ),
            arrayOf(
                "update submittable to true",
                initialState,
                MainStateModification.UpdateSubmittable(true),
                initialState.copy(isSubmittable = true)
            ),
            arrayOf(
                "update submittable to false",
                initialState.copy(isSubmittable = true),
                MainStateModification.UpdateSubmittable(false),
                initialState.copy(isSubmittable = false)
            ),
            arrayOf(
                "remove quoted HTML text",
                initialState.copy(quotedHtmlContent = quotedHtmlContent),
                MainStateModification.RemoveHtmlQuotedText,
                initialState
            )
        )
    }
}
