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

package ch.protonmail.android.mailcomposer.presentation.usecase

import ch.protonmail.android.mailmessage.domain.model.DraftAction
import javax.inject.Inject

class SubjectWithPrefixForAction @Inject constructor() {

    suspend operator fun invoke(action: DraftAction, subject: String): String = when (action) {
        is DraftAction.Compose,
        is DraftAction.PrefillForShare,
        is DraftAction.ComposeToAddresses -> subject

        is DraftAction.Forward -> if (subject.trimStart()
                .startsWith(FORWARD_PREFIX)
        ) subject else "$FORWARD_PREFIX $subject"

        is DraftAction.Reply,
        is DraftAction.ReplyAll -> if (subject.trimStart()
                .startsWith(REPLY_PREFIX)
        ) subject else "$REPLY_PREFIX $subject"
    }

    companion object {
        const val FORWARD_PREFIX = "Fw:"
        const val REPLY_PREFIX = "Re:"
    }
}
