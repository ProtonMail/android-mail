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

package ch.protonmail.android.mailmessage.domain.model

import ch.protonmail.android.mailcommon.domain.model.IntentShareInfo
import kotlinx.serialization.Serializable

@Serializable
sealed interface DraftAction {

    @Serializable
    object Compose : DraftAction

    @Serializable
    data class Reply(val parentId: MessageId) : DraftAction

    @Serializable
    data class ReplyAll(val parentId: MessageId) : DraftAction

    @Serializable
    data class Forward(val parentId: MessageId) : DraftAction

    @Serializable
    data class ComposeToAddress(val recipient: String) : DraftAction

    @Serializable
    data class PrefillForShare(val intentShareInfo: IntentShareInfo) : DraftAction

    fun getParentMessageId(): MessageId? = when (this) {
        is Compose,
        is ComposeToAddress -> null
        is PrefillForShare -> null
        is Forward -> this.parentId
        is Reply -> this.parentId
        is ReplyAll -> this.parentId
    }
}
