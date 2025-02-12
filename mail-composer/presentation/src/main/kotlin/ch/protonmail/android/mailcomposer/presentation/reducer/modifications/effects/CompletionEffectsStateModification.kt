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

package ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcomposer.presentation.model.ComposerState

internal sealed interface CompletionEffectsStateModification : EffectsStateModification {
    data class CloseComposer(val hasSavedDraft: Boolean) : EffectsStateModification {

        override fun apply(state: ComposerState.Effects): ComposerState.Effects =
            if (hasSavedDraft) state.copy(closeComposerWithDraftSaved = Effect.of(Unit))
            else state.copy(closeComposer = Effect.of(Unit))
    }

    sealed interface SendMessage : EffectsStateModification {

        override fun apply(state: ComposerState.Effects): ComposerState.Effects = when (this) {
            SendAndExit -> state.copy(closeComposerWithMessageSending = Effect.of(Unit))
            SendAndExitOffline -> state.copy(closeComposerWithMessageSendingOffline = Effect.of(Unit))
        }

        data object SendAndExit : SendMessage
        data object SendAndExitOffline : SendMessage
    }
}
