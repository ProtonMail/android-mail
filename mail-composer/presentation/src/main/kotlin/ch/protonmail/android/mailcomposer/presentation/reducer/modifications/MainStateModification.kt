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

package ch.protonmail.android.mailcomposer.presentation.reducer.modifications

import ch.protonmail.android.mailcomposer.domain.model.QuotedHtmlContent
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.presentation.model.ComposerState
import ch.protonmail.android.mailcomposer.presentation.model.SenderUiModel
import kotlinx.collections.immutable.toImmutableList
import me.proton.core.util.kotlin.takeIfNotBlank

internal sealed interface MainStateModification : ComposerStateModification<ComposerState.Main> {

    data class OnDraftReady(
        val sender: String,
        val quotedHtmlContent: QuotedHtmlContent?,
        val shouldRestrictWebViewHeight: Boolean
    ) : MainStateModification {

        override fun apply(state: ComposerState.Main): ComposerState.Main = state.copy(
            senderUiModel = SenderUiModel(sender),
            quotedHtmlContent = quotedHtmlContent,
            shouldRestrictWebViewHeight = shouldRestrictWebViewHeight
        )
    }

    data class UpdateLoading(val value: ComposerState.LoadingType) : MainStateModification {

        override fun apply(state: ComposerState.Main): ComposerState.Main = state.copy(loadingType = value)
    }

    data class UpdateSender(val sender: SenderEmail) : MainStateModification {

        override fun apply(state: ComposerState.Main): ComposerState.Main {
            val currentSender = state.senderUiModel.email
            return state.copy(
                senderUiModel = SenderUiModel(sender.value),
                prevSenderEmail = currentSender
                    .takeIfNotBlank()
                    ?.let { SenderEmail(it) }
            )
        }
    }

    data class SendersListReady(val list: List<SenderUiModel>) : MainStateModification {

        override fun apply(state: ComposerState.Main): ComposerState.Main =
            state.copy(senderAddresses = list.toImmutableList())
    }

    data class UpdateSubmittable(val isSubmittable: Boolean) : MainStateModification {

        override fun apply(state: ComposerState.Main): ComposerState.Main = state.copy(isSubmittable = isSubmittable)
    }

    data object RemoveHtmlQuotedText : MainStateModification {

        override fun apply(state: ComposerState.Main): ComposerState.Main = state.copy(quotedHtmlContent = null)
    }
}
