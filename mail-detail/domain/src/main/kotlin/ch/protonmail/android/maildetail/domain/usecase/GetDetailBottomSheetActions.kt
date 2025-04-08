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
import ch.protonmail.android.mailsettings.domain.annotations.CustomizeToolbarFeatureEnabled
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Inject

class GetDetailBottomSheetActions @Inject constructor(
    @CustomizeToolbarFeatureEnabled val isCustomizeToolbarEnabled: Boolean
) {

    operator fun invoke(
        conversation: Conversation?,
        message: Message,
        affectingConversation: Boolean
    ): List<Action> {
        val showCustomizeToolbar = isCustomizeToolbarEnabled
        val showDelete = when {
            conversation != null -> {
                affectingConversation && conversation.allMessagesAreSpamOrTrash()
            }
            else -> message.isSpamOrTrash()
        }

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
            add(Action.Spam)
            add(Action.Move)
            add(Action.Print)
            if (showCustomizeToolbar) {
                add(Action.OpenCustomizeToolbar)
            }
            add(Action.ReportPhishing)
        }.toList()
    }

    private fun Message.isSpamOrTrash() = this.labelIds
        .ignoringAllMail()
        .ignoringAllSent()
        .ignoringAllDrafts()
        .areAllTrashOrSpam()

    private fun Conversation.allMessagesAreSpamOrTrash() = labels
        .map { it.labelId }
        .ignoringAllMail()
        .ignoringAllSent()
        .ignoringAllDrafts()
        .areAllTrashOrSpam()

    private fun List<LabelId>.areAllTrashOrSpam() = this.all {
        it == SystemLabelId.Spam.labelId || it == SystemLabelId.Trash.labelId
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
