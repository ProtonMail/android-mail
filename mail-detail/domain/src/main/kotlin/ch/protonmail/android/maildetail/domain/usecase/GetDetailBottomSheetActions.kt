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

package ch.protonmail.android.maildetail.domain.usecase

import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.model.Message
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Inject

class GetDetailBottomSheetActions @Inject constructor() {

    operator fun invoke(
        conversation: Conversation?,
        message: Message,
        affectingConversation: Boolean
    ): List<Action> {
        val areAllSpam = affectingConversation && conversation?.allMessagesAreSpam() == true ||
            !affectingConversation && message.isSpam()
        val areAllTrash = affectingConversation && conversation?.allMessagesAreTrash() == true ||
            !affectingConversation && message.isTrash()
        return getActions(
            showDelete = areAllSpam || areAllTrash,
            showSpam = !areAllSpam,
            affectingConversation = affectingConversation
        )
    }

    operator fun invoke(message: Message): List<Action> {
        val isSpam = message.isSpam()
        val isTrash = message.isTrash()

        return getActions(
            showDelete = isSpam || isTrash,
            showSpam = !isSpam,
            affectingConversation = false
        )
    }

    private fun getActions(
        showDelete: Boolean,
        showSpam: Boolean,
        affectingConversation: Boolean
    ): List<Action> {
        return mutableListOf<Action>().apply {
            if (!affectingConversation) {
                add(Action.Reply)
                add(Action.ReplyAll)
                add(Action.Forward)
            }
            add(Action.MarkUnread)
            add(Action.Label)
            if (!affectingConversation) add(Action.ViewInLightMode)
            if (!affectingConversation) add(Action.ViewInDarkMode)
            if (showDelete) {
                add(Action.Delete)
            } else {
                add(Action.Trash)
            }
            add(Action.Archive)
            if (showSpam) {
                add(Action.Spam)
            }
            add(Action.Move)
            add(Action.Print)
            add(Action.OpenCustomizeToolbar)
            add(Action.ReportPhishing)
        }.toList()
    }

    private fun List<LabelId>.filterRelevant() = ignoringAllMail()
        .ignoringAllSent()
        .ignoringAllDrafts()

    private fun Message.isSpam() = this.labelIds
        .filterRelevant()
        .areAllSpam()

    private fun Message.isTrash() = this.labelIds
        .filterRelevant()
        .areAllTrash()

    private fun Conversation.allMessagesAreSpam() = labels
        .map { it.labelId }
        .filterRelevant()
        .areAllSpam()

    private fun Conversation.allMessagesAreTrash() = labels
        .map { it.labelId }
        .filterRelevant()
        .areAllTrash()

    private fun List<LabelId>.areAllSpam() = this.all {
        it == SystemLabelId.Spam.labelId
    }

    private fun List<LabelId>.areAllTrash() = this.all {
        it == SystemLabelId.Trash.labelId
    }

    private fun List<LabelId>.ignoringAllMail() = this.filterNot {
        it == SystemLabelId.AllMail.labelId
    }

    private fun List<LabelId>.ignoringAllSent() = this.filterNot {
        it == SystemLabelId.AllSent.labelId
    }

    private fun List<LabelId>.ignoringAllDrafts() = this.filterNot {
        it == SystemLabelId.AllDrafts.labelId
    }
}
